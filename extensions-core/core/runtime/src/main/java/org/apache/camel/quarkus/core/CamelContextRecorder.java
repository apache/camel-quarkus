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

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import org.apache.camel.CamelContext;
import org.apache.camel.ExtendedCamelContext;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.LambdaRouteBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.catalog.RuntimeCamelCatalog;
import org.apache.camel.spi.CamelContextCustomizer;
import org.apache.camel.spi.FactoryFinderResolver;
import org.apache.camel.spi.ModelJAXBContextFactory;
import org.apache.camel.spi.ModelToXMLDumper;
import org.apache.camel.spi.Registry;
import org.apache.camel.spi.StartupStepRecorder;
import org.apache.camel.spi.TypeConverterRegistry;

@Recorder
public class CamelContextRecorder {
    public RuntimeValue<CamelContext> createContext(
            RuntimeValue<Registry> registry,
            RuntimeValue<TypeConverterRegistry> typeConverterRegistry,
            RuntimeValue<ModelJAXBContextFactory> contextFactory,
            RuntimeValue<ModelToXMLDumper> xmlModelDumper,
            RuntimeValue<FactoryFinderResolver> factoryFinderResolver,
            RuntimeValue<StartupStepRecorder> startupStepRecorder,
            BeanContainer beanContainer,
            String version,
            CamelConfig config) {

        FastCamelContext context = new FastCamelContext(
                factoryFinderResolver.getValue(),
                version,
                xmlModelDumper.getValue());

        final ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        // Set ClassLoader first as some actions depend on it being available
        context.setApplicationContextClassLoader(tccl);
        context.setDefaultExtension(RuntimeCamelCatalog.class, () -> new CamelRuntimeCatalog(config.runtimeCatalog));
        context.setRegistry(registry.getValue());
        context.setTypeConverterRegistry(typeConverterRegistry.getValue());
        context.setLoadTypeConverters(false);
        context.setModelJAXBContextFactory(contextFactory.getValue());
        context.adapt(ExtendedCamelContext.class).setStartupStepRecorder(startupStepRecorder.getValue());
        context.build();
        context.addLifecycleStrategy(new CamelLifecycleEventBridge());
        context.getManagementStrategy().addEventNotifier(new CamelManagementEventBridge());

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

    public void addRoutes(RuntimeValue<CamelContext> context, String typeName) {
        try {
            addRoutes(
                    context,
                    context.getValue().getClassResolver().resolveClass(typeName, RoutesBuilder.class));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void addRoutes(RuntimeValue<CamelContext> context, Class<? extends RoutesBuilder> type) {
        try {
            context.getValue().addRoutes(
                    context.getValue().getInjector().newInstance(type));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void addRoutesFromContainer(RuntimeValue<CamelContext> context) {
        try {
            for (LambdaRouteBuilder builder : context.getValue().getRegistry().findByType(LambdaRouteBuilder.class)) {
                context.getValue().addRoutes(new RouteBuilder() {
                    @Override
                    public void configure() throws Exception {
                        builder.accept(this);
                    }
                });
            }
            for (RoutesBuilder builder : context.getValue().getRegistry().findByType(RoutesBuilder.class)) {
                context.getValue().addRoutes(builder);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
