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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.stream.Stream;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import org.apache.camel.CamelContext;
import org.apache.camel.Consume;
import org.apache.camel.Endpoint;
import org.apache.camel.NoTypeConversionAvailableException;
import org.apache.camel.TypeConversionException;
import org.apache.camel.model.Model;
import org.apache.camel.model.RoutesDefinition;

/**
 * Support for Camel {@link Consume} annotation.
 */
@Recorder
public class ConsumeRecorder {

    public RuntimeValue<RoutesDefinition> createRoutesDefinition() {
        final RoutesDefinition routesDefinition = new RoutesDefinition();
        return new RuntimeValue<RoutesDefinition>(routesDefinition);
    }

    public void addConsumeRoute(
            RuntimeValue<CamelContext> camelContext,
            RuntimeValue<RoutesDefinition> routesDefinition,
            String uri,
            RuntimeValue<Object> runtimeUriOrEndpoint,
            String beanName,
            String method) {
        final RoutesDefinition routes = routesDefinition.getValue();
        if (uri != null) {
            routes.from(uri).bean(beanName, method);
        } else {
            Object uriOrEndpoint = runtimeUriOrEndpoint.getValue();
            if (uriOrEndpoint instanceof Endpoint) {
                routes.from((Endpoint) uriOrEndpoint).bean(beanName, method);
            } else {
                try {
                    final String uriOrRef = camelContext.getValue().getTypeConverter().mandatoryConvertTo(String.class,
                            uriOrEndpoint);
                    routes.from(uriOrRef).bean(beanName, method);
                } catch (TypeConversionException | NoTypeConversionAvailableException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public void addConsumeRoutesToContext(RuntimeValue<CamelContext> camelContext,
            RuntimeValue<RoutesDefinition> routesDefinition) {
        try {
            final RoutesDefinition routes = routesDefinition.getValue();
            routes.setCamelContext(camelContext.getValue());
            camelContext.getValue().getCamelContextExtension().getContextPlugin(Model.class)
                    .addRouteDefinitions(routes.getRoutes());
        } catch (Exception e) {
            throw new RuntimeException("Could not add routes to context", e);
        }
    }

    public RuntimeValue<Object> getEndpointUri(RuntimeValue<CamelContext> camelContext, String beanName,
            String endpointMethodName) {
        /* Possible improvement: Instead of using reflection, we could generate this method at build time
         * to call the bean method directly */
        Object bean = camelContext.getValue().getRegistry().lookupByName(beanName);
        Method method = null;
        try {
            Class<?> cl = bean.getClass();
            do {
                method = Stream.of(cl.getDeclaredMethods())
                        .filter(m -> m.getName().equals(endpointMethodName) && m.getParameterCount() == 0)
                        .findFirst()
                        .orElse(null);
                cl = cl.getSuperclass();
            } while (method == null && cl != Object.class);
            Object result = method.invoke(bean);
            return new RuntimeValue<>(result);
        } catch (SecurityException | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

}
