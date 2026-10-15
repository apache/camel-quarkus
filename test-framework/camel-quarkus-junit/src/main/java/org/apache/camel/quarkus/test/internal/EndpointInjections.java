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
package org.apache.camel.quarkus.test.internal;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;

import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.EndpointInject;
import org.junit.platform.commons.support.AnnotationSupport;

/**
 * Re-resolves the {@link EndpointInject} endpoint fields of test instances.
 * <p>
 * Camel removes the endpoints used only by a route when the route is removed, which happens whenever a route is
 * advised or restored. Endpoints injected before that no longer receive messages, so they are resolved again.
 */
final class EndpointInjections {

    private EndpointInjections() {
        // Utility class
    }

    @SuppressWarnings("unchecked")
    static void refresh(CamelContext context, List<Object> testInstances) {
        for (Object testInstance : testInstances) {
            for (Field field : AnnotationSupport.findAnnotatedFields(testInstance.getClass(), EndpointInject.class)) {
                EndpointInject endpointInject = field.getAnnotation(EndpointInject.class);
                if (endpointInject.value().isEmpty() || Modifier.isStatic(field.getModifiers())
                        || !Endpoint.class.isAssignableFrom(field.getType())) {
                    continue;
                }
                String uri = context.resolvePropertyPlaceholders(endpointInject.value());
                Endpoint endpoint = context.getEndpoint(uri, (Class<Endpoint>) field.getType());
                try {
                    field.setAccessible(true);
                    field.set(testInstance, endpoint);
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException("Failed to inject " + uri + " into " + field, e);
                }
            }
        }
    }
}
