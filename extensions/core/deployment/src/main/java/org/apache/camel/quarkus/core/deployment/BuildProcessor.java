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
import io.quarkus.deployment.recording.RecorderContext;
import io.quarkus.runtime.RuntimeValue;
import org.apache.camel.CamelContext;
import org.apache.camel.RoutesBuilder;
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
            RecorderContext recorderContext,
            ApplicationArchivesBuildItem applicationArchives,
            List<CamelBeanBuildItem> registryItems) {

            RuntimeValue<Registry> registry = recorder.createRegistry();

            CamelSupport.services(applicationArchives)
                .filter(si -> {
                    //
                    // by default all the service found in META-INF/service/org/apache/camel are
                    // bound to the registry but some of the services are then replaced or set
                    // to the camel context directly by extension so it does not make sense to
                    // instantiate them in this phase.
                    //
                    boolean blacklisted = si.path.endsWith("reactive-executor") || si.path.endsWith("platform-http");
                    if (blacklisted) {
                        LOGGER.debug("Ignore service: {}", si);
                    }

                    return !blacklisted;
                })
                .forEach(si -> {
                    LOGGER.debug("Binding bean with name: {}, type {}", si.name, si.type);

                    recorder.bind(
                        registry,
                        si.name,
                        recorderContext.classProxy(si.type)
                    );
                });

            for (CamelBeanBuildItem item : registryItems) {
                LOGGER.debug("Binding bean with name: {}, type {}", item.getName(), item.getType());

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

        @Record(ExecutionTime.RUNTIME_INIT)
        @BuildStep
        CamelRuntimeRegistryBuildItem bindRuntimeBeansToRegistry(
            CamelRecorder recorder,
            CamelRegistryBuildItem registry,
            List<CamelRuntimeBeanBuildItem> registryItems) {


            for (CamelRuntimeBeanBuildItem item : registryItems) {
                LOGGER.debug("Binding runtime bean with name: {}, type {}", item.getName(), item.getType());

                recorder.bind(
                    registry.getRegistry(),
                    item.getName(),
                    item.getType(),
                    item.getValue()
                );
            }

            return new CamelRuntimeRegistryBuildItem(registry.getRegistry());
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

        /**
         * This method is responsible to configure camel-main during static init phase which means
         * discovering routes, listeners and services that need to be bound to the camel-main.
         * <p>
         * This method should not attempt to start or initialize camel-main as this need to be done
         * at runtime.
         */
        @SuppressWarnings("unchecked")
        @Record(ExecutionTime.STATIC_INIT)
        @BuildStep(onlyIfNot = Flags.MainDisabled.class)
        CamelMainBuildItem main(
            CombinedIndexBuildItem combinedIndex,
            CamelMainRecorder recorder,
            RecorderContext recorderContext,
            CamelContextBuildItem context,
            List<CamelMainListenerBuildItem> listeners,
            List<CamelRoutesBuilderBuildItem> routesBuilders,
            BeanContainerBuildItem beanContainer) {

            RuntimeValue<CamelMain> main = recorder.createCamelMain(context.getCamelContext(), beanContainer.getValue());
            for (CamelMainListenerBuildItem listener : listeners) {
                recorder.addListener(main, listener.getListener());
            }

            CamelSupport.getRouteBuilderClasses(combinedIndex.getIndex()).forEach(name -> {
                recorder.addRouteBuilder(main, (Class<RoutesBuilder>)recorderContext.classProxy(name));
            });
            routesBuilders.forEach(routesBuilder -> {
                recorder.addRouteBuilder(main, routesBuilder.getInstance());
            });

            return new CamelMainBuildItem(main);
        }

        /**
         * This method is responsible to start camel-main ar runtime.
         *
         * @param recorder  the recorder.
         * @param main      a reference to a {@link CamelMain}.
         * @param registry  a reference to a {@link Registry}; note that this parameter is here as placeholder to
         *                  ensure the {@link Registry} is fully configured before starting camel-main.
         * @param config    runtime configuration.
         * @param executors the {@link org.apache.camel.spi.ReactiveExecutor} to be configured on camel-main, this
         *                  happens during {@link ExecutionTime#RUNTIME_INIT} because the executor may need to start
         *                  threads and so on. Note that we now expect a list of executors but that's because there is
         *                  no way as of quarkus 0.23.x to have optional items.
         * @param shutdown  a reference to a {@link io.quarkus.runtime.ShutdownContext} used to register shutdown logic.
         * @param startList a placeholder to ensure camel-main start after the ArC container is fully initialized. This
         *                  is required as under the hoods the camel registry may look-up beans form the
         *                  container thus we need it to be fully initialized to avoid unexpected behaviors.
         */
        @Record(ExecutionTime.RUNTIME_INIT)
        @BuildStep(onlyIfNot = Flags.MainDisabled.class)
        void start(
            CamelMainRecorder recorder,
            CamelMainBuildItem main,
            CamelRuntimeRegistryBuildItem registry,
            CamelConfig.Runtime config,
            List<CamelReactiveExecutorBuildItem> executors,  // TODO: replace with @Overridable
            ShutdownContextBuildItem shutdown,
            List<ServiceStartBuildItem> startList) {

            //
            // Note that this functionality may be incorporated by camel-main, see:
            //
            //     https://issues.apache.org/jira/browse/CAMEL-14050
            //
            config.routesUris.forEach(location -> {
                recorder.addRoutesFromLocation(main.getInstance(), location);
            });

            if (executors.size() > 1) {
                throw new IllegalArgumentException("Detected multiple reactive executors");
            } else if (executors.size() == 1) {
                recorder.setReactiveExecutor(main.getInstance(), executors.get(0).getInstance());
            }

            recorder.start(shutdown, main.getInstance());
        }
    }
}
