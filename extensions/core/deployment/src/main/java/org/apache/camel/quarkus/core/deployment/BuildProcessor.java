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

import java.util.List;
import java.util.Objects;
import javax.inject.Inject;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.runtime.RuntimeValue;
import org.apache.camel.CamelContext;
import org.apache.camel.quarkus.core.runtime.CamelConfig;
import org.apache.camel.quarkus.core.runtime.CamelProducers;
import org.apache.camel.quarkus.core.runtime.CamelRecorder;
import org.apache.camel.spi.Registry;
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
    CamelRegistryBuildItem registry(
            CamelRecorder recorder,
            List<CamelBeanBuildItem> registryItems) {

        RuntimeValue<Registry> registry = recorder.createRegistry();

        CamelSupport.services(applicationArchivesBuildItem).filter(
            si -> registryItems.stream().noneMatch(
                c -> Objects.equals(si.name, c.getName()) && c.getType().isAssignableFrom(si.type)
            )
        ).forEach(
            si -> {
                LOGGER.debug("Binding camel service {} with type {}", si.name, si.type);

                recorder.bind(
                    registry,
                    si.name,
                    si.type
                );
            }
        );

        for (CamelBeanBuildItem item: registryItems) {
            LOGGER.debug("Binding item with name: {}, type {}", item.getName(), item.getType());

            recorder.bind(
                registry,
                item.getName(),
                item.getType(),
                item.getValue()
            );
        }

        return new CamelRegistryBuildItem(registry);
    }

    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep
    CamelContextBuildItem context(
            CamelRecorder recorder,
            CamelRegistryBuildItem registry,
            BeanContainerBuildItem beanContainer,
            CamelConfig.BuildTime buildTimeConfig) {

        RuntimeValue<CamelContext> context = recorder.createContext(registry.getRegistry(), beanContainer.getValue(), buildTimeConfig);
        return new CamelContextBuildItem(context);
    }

    @BuildStep
    void beans(BuildProducer<AdditionalBeanBuildItem> beanProducer) {
        beanProducer.produce(AdditionalBeanBuildItem.unremovableOf(CamelProducers.class));
    }
}
