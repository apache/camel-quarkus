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

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.ServiceStartBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.runtime.RuntimeValue;
import org.apache.camel.CamelContext;
import org.apache.camel.quarkus.core.CamelConfig;
import org.apache.camel.quarkus.core.CamelMain;
import org.apache.camel.quarkus.core.CamelMainProducers;
import org.apache.camel.quarkus.core.CamelMainRecorder;
import org.apache.camel.quarkus.core.CamelProducers;
import org.apache.camel.quarkus.core.CamelRecorder;
import org.apache.camel.quarkus.core.Flags;
import org.apache.camel.spi.Registry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class BuildProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(BuildProcessor.class);

    /*
     * Build steps related to camel core.
     */
    public static class Core {
        @BuildStep
        void beans(BuildProducer<AdditionalBeanBuildItem> beanProducer) {
            beanProducer.produce(AdditionalBeanBuildItem.unremovableOf(CamelProducers.class));
        }

        @Record(ExecutionTime.STATIC_INIT)
        @BuildStep
        CamelRegistryBuildItem registry(
            CamelRecorder recorder,
            ApplicationArchivesBuildItem applicationArchives,
            List<CamelBeanBuildItem> registryItems) {

            RuntimeValue<Registry> registry = recorder.createRegistry();

            CamelSupport.services(applicationArchives).filter(
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

            for (CamelBeanBuildItem item : registryItems) {
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
    }

    /*
     * Build steps related to camel main that are activated by default but can be
     * disabled by setting quarkus.camel.disable-main = true
     */
    public static class Main {

        @BuildStep(onlyIfNot = Flags.MainDisabled.class)
        void beans(BuildProducer<AdditionalBeanBuildItem> beanProducer) {
            beanProducer.produce(AdditionalBeanBuildItem.unremovableOf(CamelMainProducers.class));
        }

        @Record(ExecutionTime.STATIC_INIT)
        @BuildStep(onlyIfNot = Flags.MainDisabled.class)
        CamelMainBuildItem main(
            CombinedIndexBuildItem combinedIndex,
            CamelMainRecorder recorder,
            CamelContextBuildItem context,
            List<CamelMainListenerBuildItem> listeners,
            List<CamelRoutesBuilderBuildItem> routesBuilders,
            BeanContainerBuildItem beanContainer,
            CamelConfig.BuildTime buildTimeConfig) {

            RuntimeValue<CamelMain> main = recorder.createCamelMain(context.getCamelContext(), beanContainer.getValue());
            for (CamelMainListenerBuildItem listener : listeners) {
                recorder.addListener(main, listener.getListener());
            }

            CamelSupport.getRouteBuilderClasses(combinedIndex.getIndex()).forEach(name -> {
                recorder.addRouteBuilder(main, name);
            });
            routesBuilders.forEach(routesBuilder -> {
                recorder.addRouteBuilder(main, routesBuilder.getInstance());
            });
            buildTimeConfig.routesUris.forEach(location -> {
                recorder.addRoutesFromLocation(main, location);
            });

            return new CamelMainBuildItem(main);
        }

        @Record(ExecutionTime.RUNTIME_INIT)
        @BuildStep(onlyIfNot = Flags.MainDisabled.class)
        void start(
            CamelMainRecorder recorder,
            CamelMainBuildItem main,
            ShutdownContextBuildItem shutdown,
            // TODO: keep this list as placeholder to ensure the ArC container is fully
            //       started before starting main
            List<ServiceStartBuildItem> startList) {

            recorder.start(shutdown, main.getInstance());
        }
    }
}
