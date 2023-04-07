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
package org.apache.camel.quarkus.dsl.java.joor.runtime;

import java.lang.reflect.Modifier;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import org.apache.camel.CamelContext;
import org.apache.camel.CamelContextAware;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.spi.ResourceAware;
import org.apache.camel.support.ResourceHelper;
import org.apache.camel.util.ObjectHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Recorder
public class JavaJoorDslRecorder {

    private static final Logger LOG = LoggerFactory.getLogger(JavaJoorDslRecorder.class);

    public RoutesBuilder registerRoutes(RuntimeValue<CamelContext> context, String className, String location)
            throws Exception {
        Class<?> clazz = Class.forName(className);
        boolean skip = clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers())
                || Modifier.isPrivate(clazz.getModifiers());
        // must have a default no-arg constructor to be able to create an instance
        boolean ctr = ObjectHelper.hasDefaultNoArgConstructor(clazz);
        if (ctr && !skip) {
            // create a new instance of the class
            try {
                Object obj = context.getValue().getInjector().newInstance(clazz);
                if (obj instanceof RoutesBuilder builder) {
                    // inject context and resource
                    CamelContextAware.trySetCamelContext(obj, context.getValue());
                    ResourceAware.trySetResource(obj, ResourceHelper.fromString(location, ""));
                    context.getValue().addRoutes(builder);
                    return builder;
                } else {
                    LOG.warn("Ignoring the class {} as it is not of type RoutesBuilder", className);
                }
            } catch (Exception e) {
                throw new RuntimeCamelException("Cannot create instance of class: " + className, e);
            }
        } else {
            LOG.warn("Ignoring the class {} as it cannot be instantiated with the default constructor", className);
        }
        return null;
    }

    public void registerTemplatedRoutes(RuntimeValue<CamelContext> camelContext, RoutesBuilder builder) throws Exception {
        if (builder != null) {
            camelContext.getValue().addTemplatedRoutes(builder);
        }
    }
}
