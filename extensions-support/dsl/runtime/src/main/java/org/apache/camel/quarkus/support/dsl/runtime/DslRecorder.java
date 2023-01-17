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
package org.apache.camel.quarkus.support.dsl.runtime;

import java.lang.reflect.Constructor;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import org.apache.camel.CamelContext;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.builder.endpoint.EndpointRouteBuilder;
import org.apache.camel.support.ResourceHelper;

@Recorder
public class DslRecorder {

    @SuppressWarnings("unchecked")
    public void registerRoutesBuilder(RuntimeValue<CamelContext> camelContext, String className, String location,
            boolean instantiateWithCamelContext)
            throws Exception {
        Class<?> clazz = Class.forName(className);
        Class<?>[] constructorParameterTypes = instantiateWithCamelContext
                ? new Class<?>[] { EndpointRouteBuilder.class, CamelContext.class }
                : new Class<?>[] { EndpointRouteBuilder.class };
        Constructor<? extends RoutesBuilderConfigurer> constructor = (Constructor<? extends RoutesBuilderConfigurer>) clazz
                .getDeclaredConstructor(constructorParameterTypes);
        CamelContext context = camelContext.getValue();
        context.addRoutes(
                new EndpointRouteBuilder() {

                    @Override
                    public void configure() {
                        setCamelContext(context);
                        setResource(ResourceHelper.fromString(location, ""));
                        try {
                            RoutesBuilderConfigurer configurer = instantiateWithCamelContext
                                    ? constructor.newInstance(this, context) : constructor.newInstance(this);
                            configurer.configure();
                        } catch (Exception e) {
                            throw new RuntimeCamelException("Cannot create instance of class: " + className, e);
                        }
                    }
                });
    }
}
