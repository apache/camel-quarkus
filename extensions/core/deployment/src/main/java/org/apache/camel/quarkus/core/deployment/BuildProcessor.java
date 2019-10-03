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

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Stream;
import javax.inject.Inject;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.arc.deployment.BeanContainerListenerBuildItem;
import io.quarkus.arc.deployment.RuntimeBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.ServiceStartBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.runtime.RuntimeValue;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.quarkus.core.runtime.CamelConfig;
import org.apache.camel.quarkus.core.runtime.CamelConfig.BuildTime;
import org.apache.camel.quarkus.core.runtime.CamelProducers;
import org.apache.camel.quarkus.core.runtime.CamelRecorder;
import org.apache.camel.quarkus.core.runtime.CamelRuntime;
import org.apache.camel.quarkus.core.runtime.support.RuntimeRegistry;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class BuildProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(BuildProcessor.class);

    @Inject
    ApplicationArchivesBuildItem applicationArchivesBuildItem;
    @Inject
    CombinedIndexBuildItem combinedIndexBuildItem;

    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep
    CamelRuntimeBuildItem create(
            CamelRecorder recorder,
            List<CamelBeanBuildItem> camelBeans,
            BuildProducer<RuntimeBeanBuildItem> runtimeBeans) {

        RuntimeRegistry registry = new RuntimeRegistry();
        RuntimeValue<CamelRuntime> camelRuntime = recorder.create(registry);

        getBuildTimeRouteBuilderClasses().forEach(
            b -> recorder.addBuilder(camelRuntime, b)
        );

        services().filter(
            si -> camelBeans.stream().noneMatch(
                c -> Objects.equals(si.name, c.getName()) && c.getType().isAssignableFrom(si.type)
            )
        ).forEach(
            si -> {
                LOGGER.debug("Binding camel service {} with type {}", si.name, si.type);

                recorder.bind(
                    camelRuntime,
                    si.name,
                    si.type
                );
            }
        );

        for (CamelBeanBuildItem item: camelBeans) {
            LOGGER.debug("Binding item with name: {}, type {}", item.getName(), item.getType());

            recorder.bind(
                camelRuntime,
                item.getName(),
                item.getType(),
                item.getValue()
            );
        }

        runtimeBeans.produce(RuntimeBeanBuildItem.builder(CamelRuntime.class).setRuntimeValue(camelRuntime).build());

        return new CamelRuntimeBuildItem(camelRuntime);
    }

    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep
    AdditionalBeanBuildItem createProducers(
            CamelRuntimeBuildItem runtime,
            CamelRecorder recorder,
            BuildProducer<BeanContainerListenerBuildItem> listeners) {

        listeners.produce(new BeanContainerListenerBuildItem(recorder.initRuntimeInjection(runtime.getRuntime())));

        return AdditionalBeanBuildItem.unremovableOf(CamelProducers.class);
    }

    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep
    void init(
            // TODO: keep this field as we need to be sure ArC is initialized before starting events
            //       We need to re-evaluate the need of fire events from context once doing
            //       https://github.com/apache/camel-quarkus/issues/9
            BeanContainerBuildItem beanContainerBuildItem,
            CamelRuntimeBuildItem runtime,
            CamelRecorder recorder,
            BuildTime buildTimeConfig) {

        recorder.init(runtime.getRuntime(), buildTimeConfig);
    }

    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep
    void start(
            CamelRecorder recorder,
            CamelRuntimeBuildItem runtime,
            ShutdownContextBuildItem shutdown,
            // TODO: keep this list as placeholder to ensure the ArC container is fully
            //       started before starting the runtime
            List<ServiceStartBuildItem> startList,
            CamelConfig.Runtime runtimeConfig)
            throws Exception {

        recorder.start(shutdown, runtime.getRuntime(), runtimeConfig);
    }

    protected Stream<String> getBuildTimeRouteBuilderClasses() {
        Set<ClassInfo> allKnownImplementors = new HashSet<>();
        allKnownImplementors.addAll(
                combinedIndexBuildItem.getIndex().getAllKnownImplementors(DotName.createSimple(RoutesBuilder.class.getName())));
        allKnownImplementors.addAll(
                combinedIndexBuildItem.getIndex().getAllKnownSubclasses(DotName.createSimple(RouteBuilder.class.getName())));
        allKnownImplementors.addAll(combinedIndexBuildItem.getIndex()
                .getAllKnownSubclasses(DotName.createSimple(AdviceWithRouteBuilder.class.getName())));

        return allKnownImplementors
                .stream()
                .filter(CamelSupport::isConcrete)
                .filter(CamelSupport::isPublic)
                .map(ClassInfo::toString);
    }

    protected Stream<ServiceInfo> services() {
        return CamelSupport.resources(applicationArchivesBuildItem, CamelSupport.CAMEL_SERVICE_BASE_PATH)
            .map(this::services)
            .flatMap(Collection::stream);
    }

    protected List<ServiceInfo> services(Path p) {
        List<ServiceInfo> answer = new ArrayList<>();

        String name = p.getFileName().toString();
        try (InputStream is = Files.newInputStream(p)) {
            Properties props = new Properties();
            props.load(is);
            for (Map.Entry<Object, Object> entry : props.entrySet()) {
                String k = entry.getKey().toString();
                if (k.equals("class")) {
                    String clazz = entry.getValue().toString();
                    Class<?> cl = Class.forName(clazz);

                    answer.add(new ServiceInfo(name, cl));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return answer;
    }

    static class ServiceInfo {
        final String name;
        final Class<?> type;

        public ServiceInfo(String name, Class<?> type) {
            this.name = name;
            this.type = type;
        }

        @Override
        public String toString() {
            return "ServiceInfo{"
                + "name='" + name + '\''
                + ", type=" + type
                + '}';
        }
    }
}
