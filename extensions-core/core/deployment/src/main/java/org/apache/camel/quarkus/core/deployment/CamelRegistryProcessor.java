/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.quarkus.core.deployment;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;
import org.apache.camel.quarkus.core.CamelBeanQualifierResolver;
import org.apache.camel.quarkus.core.CamelConfig;
import org.apache.camel.quarkus.core.CamelRecorder;
import org.apache.camel.quarkus.core.deployment.spi.CamelBeanBuildItem;
import org.apache.camel.quarkus.core.deployment.spi.CamelBeanQualifierResolverBuildItem;
import org.apache.camel.quarkus.core.deployment.spi.CamelContextBuildItem;
import org.apache.camel.quarkus.core.deployment.spi.CamelRegistryBuildItem;
import org.apache.camel.quarkus.core.deployment.spi.CamelRuntimeBeanBuildItem;
import org.apache.camel.quarkus.core.deployment.spi.CamelRuntimeTaskBuildItem;
import org.apache.camel.quarkus.core.deployment.spi.CamelServiceDestination;
import org.apache.camel.quarkus.core.deployment.spi.CamelServiceFilterBuildItem;
import org.apache.camel.quarkus.core.deployment.spi.CamelServicePatternBuildItem;
import org.apache.camel.quarkus.core.deployment.spi.ContainerBeansBuildItem;
import org.apache.camel.quarkus.core.deployment.util.CamelSupport;
import org.apache.camel.quarkus.core.deployment.util.PathFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CamelRegistryProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(CamelRegistryProcessor.class);

    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep
    CamelRegistryBuildItem registry(
            List<CamelBeanQualifierResolverBuildItem> camelBeanQualifierResolvers,
            CamelRecorder recorder) {

        Map<String, CamelBeanQualifierResolver> beanQualifierResolvers = new HashMap<>();
        for (CamelBeanQualifierResolverBuildItem resolver : camelBeanQualifierResolvers) {
            recorder.registerCamelBeanQualifierResolver(resolver.getBeanTypeName(), resolver.getRuntimeValue(),
                    beanQualifierResolvers);
        }

        return new CamelRegistryBuildItem(recorder.createRegistry(beanQualifierResolvers));
    }

    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep
    public void bindBeansToRegistry(
            CamelRecorder recorder,
            CamelConfig camelConfig,
            ApplicationArchivesBuildItem applicationArchives,
            ContainerBeansBuildItem containerBeans,
            CamelRegistryBuildItem registry,
            // CamelContextBuildItem placeholder ensures this build step runs after the CamelContext is created
            CamelContextBuildItem camelContextBuildItem,
            List<CamelBeanBuildItem> registryItems,
            List<CamelServiceFilterBuildItem> serviceFilters,
            List<CamelServicePatternBuildItem> servicePatterns) {

        final ClassLoader TCCL = Thread.currentThread().getContextClassLoader();

        final PathFilter pathFilter = servicePatterns.stream()
                .filter(patterns -> patterns.getDestination() == CamelServiceDestination.REGISTRY)
                .collect(
                        PathFilter.Builder::new,
                        (builder, patterns) -> builder.patterns(patterns.isInclude(), patterns.getPatterns()),
                        PathFilter.Builder::combine)
                .include(camelConfig.service.registry.includePatterns)
                .exclude(camelConfig.service.registry.excludePatterns)
                .build();

        CamelSupport.services(applicationArchives, pathFilter)
                .filter(si -> !containerBeans.getBeans().contains(si))
                .filter(si -> {
                    //
                    // by default all the service found in META-INF/service/org/apache/camel are
                    // bound to the registry but some of the services are then replaced or set
                    // to the camel context directly by extension so it does not make sense to
                    // instantiate them in this phase.
                    //
                    boolean blacklisted = serviceFilters.stream().anyMatch(filter -> filter.getPredicate().test(si));
                    if (blacklisted) {
                        LOGGER.debug("Ignore service: {}", si);
                    }

                    return !blacklisted;
                })
                .forEach(si -> {
                    LOGGER.debug("Binding bean with name: {}, type {}", si.name, si.type);

                    recorder.bind(
                            registry.getRegistry(),
                            si.name,
                            CamelSupport.loadClass(si.type, TCCL));
                });

        registryItems.stream()
                .filter(item -> !containerBeans.getBeans().contains(item))
                .forEach(item -> {
                    LOGGER.debug("Binding bean with name: {}, type {}", item.getName(), item.getType());
                    if (item.getValue().isPresent()) {
                        recorder.bind(
                                registry.getRegistry(),
                                item.getName(),
                                CamelSupport.loadClass(item.getType(), TCCL),
                                item.getValue().get());
                    } else {
                        // the instance of the service will be instantiated by the recorder, this avoid
                        // creating a recorder for trivial services.
                        recorder.bind(
                                registry.getRegistry(),
                                item.getName(),
                                CamelSupport.loadClass(item.getType(), TCCL));
                    }
                });

    }

    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep
    CamelRuntimeTaskBuildItem bindRuntimeBeansToRegistry(
            CamelRecorder recorder,
            ContainerBeansBuildItem containerBeans,
            CamelRegistryBuildItem registry,
            // CamelContextBuildItem placeholder ensures this build step runs after the CamelContext is created
            CamelContextBuildItem camelContextBuildItem,
            List<CamelRuntimeBeanBuildItem> registryItems) {

        final ClassLoader TCCL = Thread.currentThread().getContextClassLoader();

        registryItems.stream()
                .filter(item -> !containerBeans.getBeans().contains(item))
                .forEach(item -> {
                    LOGGER.debug("Binding runtime bean with name: {}, type {}", item.getName(), item.getType());

                    if (item.getValue().isPresent()) {
                        recorder.bind(
                                registry.getRegistry(),
                                item.getName(),
                                CamelSupport.loadClass(item.getType(), TCCL),
                                item.getValue().get());
                    } else {
                        // the instance of the service will be instantiated by the recorder, this avoid
                        // creating a recorder for trivial services.
                        recorder.bind(
                                registry.getRegistry(),
                                item.getName(),
                                CamelSupport.loadClass(item.getType(), TCCL));
                    }
                });

        return new CamelRuntimeTaskBuildItem("registry");
    }
}
