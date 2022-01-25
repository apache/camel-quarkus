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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;

import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.arc.deployment.BeanDiscoveryFinishedBuildItem;
import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.runtime.RuntimeValue;
import org.apache.camel.CamelContext;
import org.apache.camel.quarkus.core.CamelConfig;
import org.apache.camel.quarkus.core.CamelContextRecorder;
import org.apache.camel.quarkus.core.deployment.spi.CamelComponentNameResolverBuildItem;
import org.apache.camel.quarkus.core.deployment.spi.CamelContextBuildItem;
import org.apache.camel.quarkus.core.deployment.spi.CamelContextCustomizerBuildItem;
import org.apache.camel.quarkus.core.deployment.spi.CamelFactoryFinderResolverBuildItem;
import org.apache.camel.quarkus.core.deployment.spi.CamelModelJAXBContextFactoryBuildItem;
import org.apache.camel.quarkus.core.deployment.spi.CamelModelToXMLDumperBuildItem;
import org.apache.camel.quarkus.core.deployment.spi.CamelRegistryBuildItem;
import org.apache.camel.quarkus.core.deployment.spi.CamelTypeConverterRegistryBuildItem;
import org.apache.camel.quarkus.core.deployment.util.CamelSupport;
import org.apache.camel.spi.ModelJAXBContextFactory;
import org.apache.camel.spi.TypeConverterRegistry;

public class CamelContextProcessor {
    /**
     * This build step is responsible to assemble a {@link CamelContext} instance.
     *
     * @param  beanContainer           a reference to a fully initialized CDI bean container
     * @param  recorder                the recorder.
     * @param  registry                a reference to a {@link org.apache.camel.spi.Registry}.
     * @param  typeConverterRegistry   a reference to a {@link TypeConverterRegistry}.
     * @param  modelJAXBContextFactory a list of known {@link ModelJAXBContextFactory}.
     * @param  modelDumper             a list of known {@link CamelModelToXMLDumperBuildItem}.
     * @param  factoryFinderResolver   a list of known {@link org.apache.camel.spi.FactoryFinderResolver}.
     * @param  customizers             a list of {@link org.apache.camel.spi.CamelContextCustomizer} used to
     *                                 customize the {@link CamelContext} at {@link ExecutionTime#STATIC_INIT}.
     * @return                         a build item holding an instance of a {@link CamelContext}
     */
    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep
    CamelContextBuildItem context(
            BeanContainerBuildItem beanContainer,
            CamelContextRecorder recorder,
            CamelRegistryBuildItem registry,
            CamelTypeConverterRegistryBuildItem typeConverterRegistry,
            CamelModelJAXBContextFactoryBuildItem modelJAXBContextFactory,
            CamelModelToXMLDumperBuildItem modelDumper,
            CamelFactoryFinderResolverBuildItem factoryFinderResolver,
            List<CamelContextCustomizerBuildItem> customizers,
            CamelComponentNameResolverBuildItem componentNameResolver,
            CamelConfig config) {

        RuntimeValue<CamelContext> context = recorder.createContext(
                registry.getRegistry(),
                typeConverterRegistry.getRegistry(),
                modelJAXBContextFactory.getContextFactory(),
                modelDumper.getValue(),
                factoryFinderResolver.getFactoryFinderResolver(),
                componentNameResolver.getComponentNameResolver(),
                beanContainer.getValue(),
                CamelSupport.getCamelVersion(),
                config);

        for (CamelContextCustomizerBuildItem customizer : customizers) {
            recorder.customize(context, customizer.get());
        }

        return new CamelContextBuildItem(context);
    }

    /**
     * This step customizes camel context for development mode.
     *
     * @param recorder the recorder
     * @param producer producer of context customizer build item
     */
    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep(onlyIf = IsDevelopment.class)
    public void devModeCamelContextCustomizations(
            CamelContextRecorder recorder,
            BuildProducer<CamelContextCustomizerBuildItem> producer) {
        String val = CamelSupport.getOptionalConfigValue("camel.main.shutdownTimeout", String.class, null);
        if (val == null) {
            //if no graceful timeout is set in development mode, graceful shutdown is replaced with no shutdown
            producer.produce(new CamelContextCustomizerBuildItem(recorder.createNoShutdownStrategyCustomizer()));
        }
    }

    /**
     * Registers Camel CDI event bridges if quarkus.camel.event-bridge.enabled=true and if
     * the relevant events have CDI observers configured for them.
     *
     * @param beanDiscovery build item containing the results of bean discovery
     * @param context       build item containing the CamelContext instance
     * @param recorder      the CamelContext recorder instance
     */
    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep(onlyIf = EventBridgeEnabled.class)
    public void registerCamelEventBridges(
            BeanDiscoveryFinishedBuildItem beanDiscovery,
            CamelContextBuildItem context,
            CamelContextRecorder recorder) {

        Set<String> observedLifecycleEvents = beanDiscovery.getObservers()
                .stream()
                .map(observerInfo -> observerInfo.getObservedType().name().toString())
                .filter(observedType -> observedType.startsWith("org.apache.camel.quarkus.core.events"))
                .collect(Collectors.collectingAndThen(Collectors.toUnmodifiableSet(), HashSet::new));

        // For management events the event class simple name is collected as users can
        // observe events on either the Camel event interface or the concrete event class, and
        // these are located in different packages
        Set<String> observedManagementEvents = beanDiscovery.getObservers()
                .stream()
                .map(observerInfo -> observerInfo.getObservedType().name().toString())
                .filter(className -> className.matches("org.apache.camel(?!.quarkus).*Event$"))
                .map(className -> CamelSupport.loadClass(className, Thread.currentThread().getContextClassLoader()))
                .map(observedEventClass -> observedEventClass.getSimpleName())
                .collect(Collectors.collectingAndThen(Collectors.toUnmodifiableSet(), HashSet::new));

        if (!observedLifecycleEvents.isEmpty()) {
            recorder.registerLifecycleEventBridge(context.getCamelContext(), observedLifecycleEvents);
        }

        if (!observedManagementEvents.isEmpty()) {
            recorder.registerManagementEventBridge(context.getCamelContext(), observedManagementEvents);
        }
    }

    public static final class EventBridgeEnabled implements BooleanSupplier {
        CamelConfig config;

        @Override
        public boolean getAsBoolean() {
            return config.eventBridge.enabled;
        }
    }
}
