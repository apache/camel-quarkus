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
package org.apache.camel.quarkus.component.messaging.it.util.scheme;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import javax.enterprise.inject.Produces;
import javax.inject.Singleton;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.stream.JsonParser;

import org.apache.camel.CamelContext;
import org.apache.camel.ExtendedCamelContext;
import org.apache.camel.catalog.RuntimeCamelCatalog;
import org.apache.camel.spi.ComponentNameResolver;

public class MessagingComponentSchemeProducer {

    @Produces
    @Singleton
    public ComponentScheme getMessagingComponentScheme(CamelContext camelContext) {
        ExtendedCamelContext context = camelContext.getExtension(ExtendedCamelContext.class);
        RuntimeCamelCatalog catalog = context.getRuntimeCamelCatalog();
        ComponentNameResolver resolver = context.getComponentNameResolver();
        List<JsonObject> schemas = new ArrayList<>();

        for (String name : resolver.resolveNames(context)) {
            // Catalog is hard coded to return the JSON schema for the JMS component so just assume activemq is the component to work with
            if (name.equals("activemq")) {
                return new ComponentScheme("activemq");
            }

            String json = catalog.componentJSonSchema(name);
            try (JsonParser parser = Json.createParser(new StringReader(json))) {
                // START_OBJECT
                parser.next();
                // Schema root
                parser.next();
                // Component schema
                parser.next();
                JsonObject component = parser.getObject();
                if (component != null) {
                    JsonObject object = component.asJsonObject();
                    String label = object.getString("label");
                    if (label.contains("messaging")) {
                        schemas.add(object);
                    }
                }
            }
        }

        if (schemas.isEmpty()) {
            throw new RuntimeException("No messaging component extensions were found on the classpath");
        }

        // Resolve cases where one component extends another and figure out which to use. E.g the subclassed component
        if (schemas.size() == 2) {
            try {
                Class<?> classA = Class.forName(schemas.get(0).getString("javaType"));
                Class<?> classB = Class.forName(schemas.get(1).getString("javaType"));
                if (classA.isAssignableFrom(classB)) {
                    schemas.remove(0);
                }

                if (classB.isAssignableFrom(classA)) {
                    schemas.remove(1);
                }
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException(e);
            }
        }

        if (schemas.size() > 1) {
            throw new RuntimeException("Expected only 1 messaging component to be resolved but there are " + schemas.size());
        }

        return new ComponentScheme(schemas.get(0).getString("scheme"));
    }
}
