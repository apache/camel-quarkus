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
package org.apache.camel.quarkus.support.debezium.deployment;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Modifier;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import io.debezium.config.Field;
import org.apache.kafka.common.config.ConfigDef;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Fails when a Debezium upgrade adds a configuration option whose default value names a class that
 * {@link DebeziumSupportProcessor} does not register for reflection. Only options of type
 * {@link ConfigDef.Type#CLASS} are checked: class names held in options of other types, such as
 * {@link ConfigDef.Type#STRING}, are not covered.
 */
class DebeziumConfigurablePluginsTest {
    private static final String SERVICES_PATH = "META-INF/services/";
    // Sink connectors are not supported by the Camel Debezium components
    private static final String SINK_PACKAGE = ".sink.";

    @Test
    void configurationClassDefaultsAreRegisteredForReflection() throws Exception {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Set<String> serviceProviders = new HashSet<>();
        Set<String> checked = new HashSet<>();
        Set<String> unregistered = new TreeSet<>();

        for (JarFile jar : debeziumJars(classLoader)) {
            try (jar) {
                for (JarEntry entry : Collections.list(jar.entries())) {
                    String entryName = entry.getName();
                    if (entryName.startsWith(SERVICES_PATH) && entryName.length() > SERVICES_PATH.length()
                            && !DebeziumSupportProcessor.EXCLUDED_SERVICES
                                    .contains(entryName.substring(SERVICES_PATH.length()))) {
                        serviceProviders.addAll(readServiceProviders(jar, entry));
                    } else if (entryName.endsWith(".class") && !entryName.contains("-")
                            && !entryName.replace('/', '.').contains(SINK_PACKAGE)) {
                        String className = entryName.substring(0, entryName.length() - 6).replace('/', '.');
                        for (String defaultClassName : classDefaults(className, classLoader)) {
                            checked.add(defaultClassName);
                            if (!isRegistered(defaultClassName, classLoader)) {
                                unregistered.add(className + " -> " + defaultClassName);
                            }
                        }
                    }
                }
            }
        }

        assertFalse(checked.isEmpty(), "No Debezium configuration options with class defaults were found");
        unregistered.removeIf(entry -> serviceProviders.contains(entry.substring(entry.indexOf(" -> ") + 4)));
        assertTrue(unregistered.isEmpty(),
                "Debezium configuration defaults name classes that are not registered for reflection. "
                        + "Add their plugin type to DebeziumSupportProcessor.CONFIGURABLE_PLUGIN_TYPES "
                        + "or register them individually: " + unregistered);
    }

    private static Set<JarFile> debeziumJars(ClassLoader classLoader) throws IOException, URISyntaxException {
        Set<JarFile> jars = new HashSet<>();
        for (URL url : Collections.list(classLoader.getResources("META-INF/maven/io.debezium"))) {
            if (url.openConnection() instanceof JarURLConnection connection) {
                jars.add(new JarFile(Path.of(connection.getJarFileURL().toURI()).toFile()));
            }
        }
        return jars;
    }

    private static Set<String> readServiceProviders(JarFile jar, JarEntry entry) throws IOException {
        Set<String> providers = new HashSet<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(jar.getInputStream(entry), StandardCharsets.UTF_8))) {
            reader.lines()
                    .map(line -> line.replaceAll("#.*", "").trim())
                    .filter(line -> !line.isEmpty())
                    .forEach(providers::add);
        }
        return providers;
    }

    private static Set<String> classDefaults(String className, ClassLoader classLoader) {
        Set<String> defaults = new HashSet<>();
        try {
            Class<?> clazz = Class.forName(className, false, classLoader);
            for (java.lang.reflect.Field field : clazz.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) && field.getType() == Field.class) {
                    field.setAccessible(true);
                    Field configField = (Field) field.get(null);
                    if (configField != null && configField.type() == ConfigDef.Type.CLASS
                            && configField.defaultValue() != null) {
                        Object defaultValue = configField.defaultValue();
                        defaults.add(defaultValue instanceof Class<?> type ? type.getName() : defaultValue.toString());
                    }
                }
            }
        } catch (Throwable e) {
            // Classes that cannot be loaded or initialized without optional dependencies hold no usable defaults
        }
        return defaults;
    }

    private static boolean isRegistered(String className, ClassLoader classLoader) {
        try {
            Class<?> clazz = Class.forName(className, false, classLoader);
            return DebeziumSupportProcessor.REFLECTIVE_CLASSES.contains(clazz)
                    || DebeziumSupportProcessor.CONFIGURABLE_PLUGIN_TYPES.stream()
                            .anyMatch(type -> type.isAssignableFrom(clazz));
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
