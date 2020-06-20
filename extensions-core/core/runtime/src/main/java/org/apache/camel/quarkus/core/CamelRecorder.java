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

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import org.apache.camel.impl.engine.DefaultReactiveExecutor;
import org.apache.camel.model.ValidateDefinition;
import org.apache.camel.model.validator.PredicateValidatorDefinition;
import org.apache.camel.quarkus.core.FastFactoryFinderResolver.Builder;
import org.apache.camel.reifier.ProcessorReifier;
import org.apache.camel.reifier.validator.ValidatorReifier;
import org.apache.camel.spi.FactoryFinderResolver;
import org.apache.camel.spi.ModelJAXBContextFactory;
import org.apache.camel.spi.ModelToXMLDumper;
import org.apache.camel.spi.ReactiveExecutor;
import org.apache.camel.spi.Registry;
import org.apache.camel.spi.TypeConverterLoader;
import org.apache.camel.spi.TypeConverterRegistry;
import org.apache.camel.spi.XMLRoutesDefinitionLoader;

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
            loader.getConstructor().newInstance().load(registry.getValue());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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

    public RuntimeValue<XMLRoutesDefinitionLoader> newDisabledXMLRoutesDefinitionLoader() {
        return new RuntimeValue<>(new DisabledXMLRoutesDefinitionLoader());
    }

    public RuntimeValue<ModelToXMLDumper> newDisabledModelToXMLDumper() {
        return new RuntimeValue<>(new DisabledModelToXMLDumper());
    }

    public RuntimeValue<RegistryRoutesLoader> newDefaultRegistryRoutesLoader() {
        return new RuntimeValue<>(new RegistryRoutesLoaders.Default());
    }

    public RuntimeValue<Builder> factoryFinderResolverBuilder() {
        return new RuntimeValue<>(new FastFactoryFinderResolver.Builder());
    }

    public void factoryFinderResolverEntry(RuntimeValue<Builder> builder, String resourcePath, Class<?> cl) {
        builder.getValue().entry(resourcePath, cl);
    }

    public RuntimeValue<FactoryFinderResolver> factoryFinderResolver(RuntimeValue<Builder> builder) {
        return new RuntimeValue<>(builder.getValue().build());
    }

    public RuntimeValue<ReactiveExecutor> createReactiveExecutor() {
        return new RuntimeValue<>(new DefaultReactiveExecutor());
    }
}
