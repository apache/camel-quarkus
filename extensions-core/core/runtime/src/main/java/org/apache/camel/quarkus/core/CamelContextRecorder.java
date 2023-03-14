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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import org.apache.camel.CamelContext;
import org.apache.camel.ExtendedCamelContext;
import org.apache.camel.RouteConfigurationsBuilder;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.LambdaRouteBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.builder.endpoint.EndpointRouteBuilder;
import org.apache.camel.builder.endpoint.LambdaEndpointRouteBuilder;
import org.apache.camel.catalog.RuntimeCamelCatalog;
import org.apache.camel.spi.CamelContextCustomizer;
import org.apache.camel.spi.ComponentNameResolver;
import org.apache.camel.spi.FactoryFinderResolver;
import org.apache.camel.spi.ModelJAXBContextFactory;
import org.apache.camel.spi.ModelToXMLDumper;
import org.apache.camel.spi.PackageScanClassResolver;
import org.apache.camel.spi.Registry;
import org.apache.camel.spi.TypeConverterRegistry;

@Recorder
public class CamelContextRecorder {
    public RuntimeValue<CamelContext> createContext(
            RuntimeValue<Registry> registry,
            RuntimeValue<TypeConverterRegistry> typeConverterRegistry,
            RuntimeValue<ModelJAXBContextFactory> contextFactory,
            RuntimeValue<ModelToXMLDumper> xmlModelDumper,
            RuntimeValue<FactoryFinderResolver> factoryFinderResolver,
            RuntimeValue<ComponentNameResolver> componentNameResolver,
            RuntimeValue<PackageScanClassResolver> packageScanClassResolver,
            BeanContainer beanContainer,
            String version,
            CamelConfig config) {

        FastCamelContext context = new FastCamelContext(
                factoryFinderResolver.getValue(),
                version,
                xmlModelDumper.getValue());

        final ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        // Set ClassLoader first as some actions depend on it being available
        ExtendedCamelContext extendedCamelContext = context.getCamelContextExtension();
        context.setApplicationContextClassLoader(tccl);
        context.getCamelContextExtension().addContextPlugin(RuntimeCamelCatalog.class,
                new CamelRuntimeCatalog(config.runtimeCatalog));
        extendedCamelContext.setRegistry(registry.getValue());
        context.setTypeConverterRegistry(typeConverterRegistry.getValue());
        context.setLoadTypeConverters(false);
        extendedCamelContext.addContextPlugin(ModelJAXBContextFactory.class, contextFactory.getValue());
        extendedCamelContext.addContextPlugin(PackageScanClassResolver.class, packageScanClassResolver.getValue());
        context.build();
        extendedCamelContext.addContextPlugin(ComponentNameResolver.class, componentNameResolver.getValue());

        // register to the container
        beanContainer.instance(CamelProducers.class).setContext(context);

        return new RuntimeValue<>(context);
    }

    public void customize(RuntimeValue<CamelContext> context, RuntimeValue<CamelContextCustomizer> contextCustomizer) {
        contextCustomizer.getValue().configure(context.getValue());
    }

    public RuntimeValue<CamelRuntime> createRuntime(BeanContainer beanContainer, RuntimeValue<CamelContext> context) {
        final CamelRuntime runtime = new CamelContextRuntime(context.getValue());

        // register to the container
        beanContainer.instance(CamelProducers.class).setRuntime(runtime);

        return new RuntimeValue<>(runtime);
    }

    public RuntimeValue<CamelContextCustomizer> createNoShutdownStrategyCustomizer() {
        return new RuntimeValue((CamelContextCustomizer) context -> context.setShutdownStrategy(new NoShutdownStrategy()));
    }

    public RuntimeValue<CamelContextCustomizer> createSourceLocationEnabledCustomizer() {
        return new RuntimeValue((CamelContextCustomizer) context -> context.setSourceLocationEnabled(true));
    }

    public void addRoutes(RuntimeValue<CamelContext> context, List<String> nonCdiRoutesBuilderClassNames) {
        List<RoutesBuilder> allRoutesBuilders = new ArrayList<>();

        try {
            for (String nonCdiRoutesBuilderClassName : nonCdiRoutesBuilderClassNames) {
                Class<RoutesBuilder> nonCdiRoutesBuilderClass = context.getValue().getClassResolver()
                        .resolveClass(nonCdiRoutesBuilderClassName, RoutesBuilder.class);
                allRoutesBuilders.add(context.getValue().getInjector().newInstance(nonCdiRoutesBuilderClass));
            }

            for (LambdaRouteBuilder builder : context.getValue().getRegistry().findByType(LambdaRouteBuilder.class)) {
                allRoutesBuilders.add(new RouteBuilder() {
                    @Override
                    public void configure() throws Exception {
                        builder.accept(this);
                    }
                });
            }

            for (LambdaEndpointRouteBuilder builder : context.getValue().getRegistry()
                    .findByType(LambdaEndpointRouteBuilder.class)) {
                allRoutesBuilders.add(new EndpointRouteBuilder() {
                    @Override
                    public void configure() throws Exception {
                        builder.accept(this);
                    }
                });
            }

            for (RoutesBuilder cdiRoutesBuilder : context.getValue().getRegistry().findByType(RoutesBuilder.class)) {
                allRoutesBuilders.add(cdiRoutesBuilder);
            }

            // Add RouteConfigurationsBuilders before RoutesBuilders
            for (RoutesBuilder routesBuilder : allRoutesBuilders) {
                if (routesBuilder instanceof RouteConfigurationsBuilder) {
                    context.getValue().addRoutesConfigurations((RouteConfigurationsBuilder) routesBuilder);
                }
            }
            for (RoutesBuilder routesBuilder : allRoutesBuilders) {
                if (!(routesBuilder instanceof RouteConfigurationsBuilder)) {
                    context.getValue().addRoutes(routesBuilder);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void registerLifecycleEventBridge(RuntimeValue<CamelContext> context, Set<String> observedLifecycleEvents) {
        context.getValue().addLifecycleStrategy(new CamelLifecycleEventBridge(observedLifecycleEvents));
    }

    public void registerManagementEventBridge(RuntimeValue<CamelContext> camelContext, Set<String> observedManagementEvents) {
        camelContext.getValue()
                .getManagementStrategy()
                .addEventNotifier(new CamelManagementEventBridge(observedManagementEvents));
    }
}
