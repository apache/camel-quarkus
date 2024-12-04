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
package org.apache.camel.quarkus.core;

import java.util.Optional;
import java.util.Set;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import org.apache.camel.CamelContext;
import org.apache.camel.ContextEvents;
import org.apache.camel.ExtendedCamelContext;
import org.apache.camel.catalog.RuntimeCamelCatalog;
import org.apache.camel.clock.Clock;
import org.apache.camel.impl.debugger.BacklogTracer;
import org.apache.camel.impl.engine.DefaultVariableRepositoryFactory;
import org.apache.camel.quarkus.core.devmode.NoOpModelineFactory;
import org.apache.camel.quarkus.core.devmode.NoShutdownStrategy;
import org.apache.camel.spi.CamelContextCustomizer;
import org.apache.camel.spi.ComponentNameResolver;
import org.apache.camel.spi.FactoryFinderResolver;
import org.apache.camel.spi.ModelJAXBContextFactory;
import org.apache.camel.spi.ModelReifierFactory;
import org.apache.camel.spi.ModelToXMLDumper;
import org.apache.camel.spi.ModelToYAMLDumper;
import org.apache.camel.spi.ModelineFactory;
import org.apache.camel.spi.PackageScanClassResolver;
import org.apache.camel.spi.Registry;
import org.apache.camel.spi.TypeConverterRegistry;
import org.apache.camel.spi.VariableRepositoryFactory;
import org.apache.camel.support.ResetableClock;
import org.eclipse.microprofile.config.ConfigProvider;

@Recorder
public class CamelContextRecorder {
    public RuntimeValue<CamelContext> createContext(
            RuntimeValue<Registry> registry,
            RuntimeValue<TypeConverterRegistry> typeConverterRegistry,
            RuntimeValue<ModelJAXBContextFactory> contextFactory,
            RuntimeValue<ModelToXMLDumper> xmlModelDumper,
            RuntimeValue<ModelToYAMLDumper> yamlModelDumper,
            RuntimeValue<FactoryFinderResolver> factoryFinderResolver,
            RuntimeValue<ComponentNameResolver> componentNameResolver,
            RuntimeValue<PackageScanClassResolver> packageScanClassResolver,
            RuntimeValue<ModelReifierFactory> modelReifierFactory,
            RuntimeValue<Clock> bootClock,
            BeanContainer beanContainer,
            String version,
            CamelConfig config) {

        FastCamelContext context = new FastCamelContext(
                version,
                xmlModelDumper.getValue(),
                yamlModelDumper.getValue());

        context.getClock().add(ContextEvents.BOOT, bootClock.getValue());

        final ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        // Set ClassLoader first as some actions depend on it being available
        ExtendedCamelContext extendedCamelContext = context.getCamelContextExtension();
        context.setApplicationContextClassLoader(tccl);
        extendedCamelContext.addContextPlugin(FactoryFinderResolver.class, factoryFinderResolver.getValue());
        extendedCamelContext.addContextPlugin(RuntimeCamelCatalog.class, new CamelRuntimeCatalog(config.runtimeCatalog));
        //variable repository factory depends on factoryFinder and classLoader, therefore has to be initialized afterwards
        extendedCamelContext.addContextPlugin(VariableRepositoryFactory.class, new DefaultVariableRepositoryFactory(context));
        extendedCamelContext.setRegistry(registry.getValue());

        context.setModelReifierFactory(modelReifierFactory.getValue());

        TypeConverterRegistry typeConverterRegistryValue = typeConverterRegistry.getValue();
        typeConverterRegistryValue.setInjector(new FastTypeConverterInjector(context));
        context.setTypeConverterRegistry(typeConverterRegistryValue);
        context.setLoadTypeConverters(false);

        extendedCamelContext.addContextPlugin(ModelJAXBContextFactory.class, contextFactory.getValue());
        extendedCamelContext.addContextPlugin(PackageScanClassResolver.class, packageScanClassResolver.getValue());
        context.build();
        extendedCamelContext.addContextPlugin(ComponentNameResolver.class, componentNameResolver.getValue());

        // register to the container
        beanContainer.beanInstance(CamelProducers.class).setContext(context);

        return new RuntimeValue<>(context);
    }

    public void customize(RuntimeValue<CamelContext> context, RuntimeValue<CamelContextCustomizer> contextCustomizer) {
        contextCustomizer.getValue().configure(context.getValue());
    }

    public void customizeDevModeCamelContext(RuntimeValue<CamelContext> camelContextRuntimeValue, boolean isModeLineDslAbsent) {
        CamelContext camelContext = camelContextRuntimeValue.getValue();

        // If no graceful timeout is set in development mode, graceful shutdown is replaced with NoShutdownStrategy
        Optional<String> shutdownTimeout = ConfigProvider.getConfig().getOptionalValue("camel.main.shutdownTimeout",
                String.class);
        if (shutdownTimeout.isEmpty()) {
            camelContext.setShutdownStrategy(new NoShutdownStrategy());
        }

        if (isModeLineDslAbsent) {
            // Camel dev profile attempts to enable modeline support, so we need to provide a NoOp impl if the modeline extension is not present
            camelContext.getCamelContextExtension().addContextPlugin(ModelineFactory.class, new NoOpModelineFactory());
        }
    }

    public RuntimeValue<CamelContextCustomizer> createSourceLocationEnabledCustomizer() {
        return new RuntimeValue<>(context -> context.setSourceLocationEnabled(true));
    }

    public void registerLifecycleEventBridge(RuntimeValue<CamelContext> context, Set<String> observedLifecycleEvents) {
        context.getValue().addLifecycleStrategy(new CamelLifecycleEventBridge(observedLifecycleEvents));
    }

    public void registerManagementEventBridge(RuntimeValue<CamelContext> camelContext, Set<String> observedManagementEvents) {
        camelContext.getValue()
                .getManagementStrategy()
                .addEventNotifier(new CamelManagementEventBridge(observedManagementEvents));
    }

    public RuntimeValue<CamelContextCustomizer> createBacklogTracerCustomizer(CamelConfig config) {
        return new RuntimeValue<>(context -> {
            // must enable source location so tracer tooling knows to map breakpoints to source code
            context.setSourceLocationEnabled(true);

            // enable tracer on camel
            context.setBacklogTracing(config.trace.enabled);
            context.setBacklogTracingStandby(config.trace.standby);
            context.setBacklogTracingTemplates(config.trace.traceTemplates);

            BacklogTracer tracer = BacklogTracer.createTracer(context);
            tracer.setEnabled(config.trace.enabled);
            tracer.setStandby(config.trace.standby);
            tracer.setBacklogSize(config.trace.backlogSize);
            tracer.setRemoveOnDump(config.trace.removeOnDump);
            tracer.setBodyMaxChars(config.trace.bodyMaxChars);
            tracer.setBodyIncludeStreams(config.trace.bodyIncludeStreams);
            tracer.setBodyIncludeFiles(config.trace.bodyIncludeFiles);
            tracer.setIncludeExchangeProperties(config.trace.includeExchangeProperties);
            tracer.setIncludeExchangeVariables(config.trace.includeExchangeVariables);
            tracer.setIncludeException(config.trace.includeException);
            tracer.setTraceRests(config.trace.traceRests);
            tracer.setTraceTemplates(config.trace.traceTemplates);
            tracer.setTracePattern(config.trace.tracePattern.orElse(null));
            tracer.setTraceFilter(config.trace.traceFilter.orElse(null));

            context.getCamelContextExtension().addContextPlugin(BacklogTracer.class, tracer);
        });
    }

    public RuntimeValue<Clock> createBootClock(boolean isNativeImage) {
        Clock clock;
        if (isNativeImage) {
            // Avoid recording boot times since 'boot' is done during the native image build
            clock = new Clock() {
                @Override
                public long elapsed() {
                    return 0;
                }

                @Override
                public long getCreated() {
                    return 0;
                }
            };
        } else {
            clock = new ResetableClock();
        }
        return new RuntimeValue<>(clock);
    }
}
