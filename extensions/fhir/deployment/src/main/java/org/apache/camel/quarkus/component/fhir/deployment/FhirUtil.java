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
package org.apache.camel.quarkus.component.fhir.deployment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class FhirUtil {

    private static final String RESOURCE_PREFIX = "resource.";

    private FhirUtil() {
    }

    public static Collection<String> getModelClasses(Map<String, String> properties) {
        return getInnerClasses(properties.values().toArray(new String[0]));
    }

    public static Collection<String> getResourceDefinitions(Map<String, String> properties) {
        List<String> resources = new ArrayList<>();
        for (String stringPropertyName : properties.keySet()) {
            if (stringPropertyName.contains(RESOURCE_PREFIX)) {
                resources.add(stringPropertyName.substring(RESOURCE_PREFIX.length()));
            }
        }
        return resources;
    }

    public static Collection<String> getInnerClasses(String... classList) {
        try {
            Set<String> classes = new HashSet<>();
            for (Object value : classList) {
                String clazz = (String) value;
                final Class[] parent = Class.forName(clazz).getClasses();
                for (Class aClass : parent) {
                    String name = aClass.getName();
                    classes.add(name);
                }
                classes.add(clazz);
            }
            return classes;
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Please ensure FHIR is on the classpath", e);
        }
    }
}
