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
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.spi.Registry;
import org.graalvm.nativeimage.ImageInfo;

@Recorder
public class CamelRecorder {
    public RuntimeValue<Registry> createRegistry() {
        return new RuntimeValue<>(new RuntimeRegistry());
    }

    @SuppressWarnings("unchecked")
    public RuntimeValue<CamelContext> createContext(RuntimeValue<Registry> registry, BeanContainer beanContainer, CamelConfig.BuildTime buildTimeConfig) {
        FastCamelContext context = new FastCamelContext();
        context.setRegistry(registry.getValue());
        context.setLoadTypeConverters(false);
        context.getTypeConverterRegistry().setInjector(context.getInjector());

        try {
            // The creation of the JAXB context is very time consuming, so always prepare it
            // when running in native mode, but lazy create it in java mode so that we don't
            // waste time if using java routes
            if (buildTimeConfig.disableJaxb) {
                context.adapt(ExtendedCamelContext.class).setModelJAXBContextFactory(() -> {
                    throw new UnsupportedOperationException();
                });
            } else if (ImageInfo.inImageBuildtimeCode()) {
                context.adapt(ExtendedCamelContext.class).getModelJAXBContextFactory().newJAXBContext();
            }
        } catch (Exception e) {
            throw RuntimeCamelException.wrapRuntimeCamelException(e);
        }

        FastModel model = new FastModel(context);

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
        Class<?> type) {

        try {
            runtime.getValue().bind(name, type, type.newInstance());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
