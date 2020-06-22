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
import org.apache.camel.RoutesBuilder;
import org.apache.camel.catalog.RuntimeCamelCatalog;
import org.apache.camel.spi.FactoryFinderResolver;
import org.apache.camel.spi.ModelJAXBContextFactory;
import org.apache.camel.spi.ModelToXMLDumper;
import org.apache.camel.spi.Registry;
import org.apache.camel.spi.TypeConverterRegistry;
import org.apache.camel.spi.XMLRoutesDefinitionLoader;

@Recorder
public class CamelContextRecorder {
    public RuntimeValue<CamelContext> createContext(
            RuntimeValue<Registry> registry,
            RuntimeValue<TypeConverterRegistry> typeConverterRegistry,
            RuntimeValue<ModelJAXBContextFactory> contextFactory,
            RuntimeValue<XMLRoutesDefinitionLoader> xmlLoader,
            RuntimeValue<ModelToXMLDumper> xmlModelDumper,
            RuntimeValue<FactoryFinderResolver> factoryFinderResolver,
            BeanContainer beanContainer,
            String version,
            CamelConfig config) {

        FastCamelContext context = new FastCamelContext(
                factoryFinderResolver.getValue(),
                version,
                xmlLoader.getValue(),
                xmlModelDumper.getValue());

        context.setDefaultExtension(RuntimeCamelCatalog.class, () -> new CamelRuntimeCatalog(config.runtimeCatalog));
        context.setRegistry(registry.getValue());
        context.setTypeConverterRegistry(typeConverterRegistry.getValue());
        context.setLoadTypeConverters(false);
        context.setModelJAXBContextFactory(contextFactory.getValue());
        context.build();
        context.addLifecycleStrategy(new CamelLifecycleEventBridge());
        context.getManagementStrategy().addEventNotifier(new CamelEventBridge());

        // register to the container
        beanContainer.instance(CamelProducers.class).setContext(context);

        return new RuntimeValue<>(context);
    }

    public void customize(RuntimeValue<CamelContext> context, RuntimeValue<CamelContextCustomizer> contextCustomizer) {
        contextCustomizer.getValue().customize(context.getValue());
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
            for (RoutesBuilder builder : context.getValue().getRegistry().findByType(RoutesBuilder.class)) {
                context.getValue().addRoutes(builder);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
