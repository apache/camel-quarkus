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
import org.apache.camel.model.ValidateDefinition;
import org.apache.camel.model.validator.PredicateValidatorDefinition;
import org.apache.camel.reifier.ProcessorReifier;
import org.apache.camel.reifier.validator.ValidatorReifier;
import org.apache.camel.spi.ModelJAXBContextFactory;
import org.apache.camel.spi.Registry;
import org.apache.camel.spi.TypeConverterLoader;
import org.apache.camel.spi.TypeConverterRegistry;

@Recorder
public class CamelRecorder {
    public RuntimeValue<Registry> createRegistry() {
        return new RuntimeValue<>(new RuntimeRegistry());
    }

    public RuntimeValue<TypeConverterRegistry> createTypeConverterRegistry() {
        return new RuntimeValue<>(new FastTypeConverter());
    }

    public void addTypeConverterLoader(RuntimeValue<TypeConverterRegistry> registry, RuntimeValue<TypeConverterLoader> loader) {
        loader.getValue().load(registry.getValue());
    }

    public void addTypeConverterLoader(RuntimeValue<TypeConverterRegistry> registry,
            Class<? extends TypeConverterLoader> loader) {
        try {
            loader.newInstance().load(registry.getValue());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public RuntimeValue<CamelContext> createContext(
            RuntimeValue<Registry> registry,
            RuntimeValue<TypeConverterRegistry> typeConverterRegistry,
            RuntimeValue<ModelJAXBContextFactory> contextFactory,
            RuntimeValue<XmlRoutesLoader> xmlLoader,
            BeanContainer beanContainer) {
        FastCamelContext context = new FastCamelContext();
        context.setRegistry(registry.getValue());
        context.setTypeConverterRegistry(typeConverterRegistry.getValue());
        context.setLoadTypeConverters(false);
        context.setModelJAXBContextFactory(contextFactory.getValue());

        FastModel model = new FastModel(context, xmlLoader.getValue());
        context.setModel(model);
        context.init();

        // register to the container
        beanContainer.instance(CamelProducers.class).setContext(context);

        return new RuntimeValue<>(context);
    }

    public void bind(
            RuntimeValue<Registry> runtime,
            String name,
            Class<?> type,
            Object instance) {

        runtime.getValue().bind(name, type, instance);
    }

    public void bind(
            RuntimeValue<Registry> runtime,
            String name,
            Class<?> type,
            RuntimeValue<?> instance) {

        runtime.getValue().bind(name, type, instance.getValue());
    }

    public void bind(
            RuntimeValue<Registry> runtime,
            String name,
            Class<?> type) {

        try {
            runtime.getValue().bind(name, type, type.newInstance());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void disableXmlReifiers() {
        ProcessorReifier.registerReifier(ValidateDefinition.class, DisabledValidateReifier::new);
        ValidatorReifier.registerReifier(PredicateValidatorDefinition.class, DisabledPredicateValidatorReifier::new);
    }

    public RuntimeValue<ModelJAXBContextFactory> newDisabledModelJAXBContextFactory() {
        return new RuntimeValue<>(new DisabledModelJAXBContextFactory());
    }

    public RuntimeValue<XmlRoutesLoader> newDisabledXmlRoutesLoader() {
        return new RuntimeValue<>(new DisabledXmlRoutesLoader());
    }

    public RuntimeValue<RegistryRoutesLoader> newDefaultRegistryRoutesLoader() {
        return new RuntimeValue<>(new RegistryRoutesLoaders.Default());
    }
}
