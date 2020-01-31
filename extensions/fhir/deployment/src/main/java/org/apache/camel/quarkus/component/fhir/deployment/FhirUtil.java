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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import ca.uhn.fhir.context.FhirContext;

public final class FhirUtil {

    private FhirUtil() {
    }

    public static Properties loadProperties(String path) {
        try (InputStream str = FhirContext.class.getResourceAsStream(path)) {
            Properties prop = new Properties();
            prop.load(str);
            return prop;
        } catch (Exception e) {
            throw new RuntimeException("Please ensure FHIR is on the classpath", e);
        }
    }

    public static Collection<String> getModelClasses(Properties properties) {
        return getInnerClasses(properties.values().toArray(new String[0]));
    }

    public static Collection<String> getResourceDefinitions(Properties properties) {
        List<String> resources = new ArrayList<>();
        for (String stringPropertyName : properties.stringPropertyNames()) {
            if (stringPropertyName.contains("resource.")) {
                resources.add(stringPropertyName.replace("resource.", ""));
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
