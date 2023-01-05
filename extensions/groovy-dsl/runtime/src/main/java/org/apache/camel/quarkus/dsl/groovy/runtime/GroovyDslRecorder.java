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
package org.apache.camel.quarkus.dsl.groovy.runtime;

import java.lang.reflect.Constructor;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import org.apache.camel.CamelContext;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.builder.endpoint.EndpointRouteBuilder;
import org.apache.camel.support.ResourceHelper;

@Recorder
public class GroovyDslRecorder {
    public void registerRoutesBuilder(RuntimeValue<CamelContext> camelContext, String className, String location)
            throws Exception {
        Constructor<? extends Configurer> constructor = (Constructor<? extends Configurer>) Class.forName(className)
                .getDeclaredConstructor(EndpointRouteBuilder.class);
        camelContext.getValue().addRoutes(
                new EndpointRouteBuilder() {

                    @Override
                    public void configure() throws Exception {
                        setCamelContext(camelContext.getValue());
                        setResource(ResourceHelper.fromString(location, ""));
                        try {
                            constructor.newInstance(this).configure();
                        } catch (Exception e) {
                            throw new RuntimeCamelException("Cannot create instance of class: " + className, e);
                        }
                    }
                });
    }
}
