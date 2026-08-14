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
package org.apache.camel.quarkus.component.console;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.camel.spi.RuntimePropertiesProvider;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.spi.ConfigSource;

public class QuarkusRuntimePropertiesProvider implements RuntimePropertiesProvider {

    private volatile Collection<Property> cached;

    @Override
    public Collection<Property> getProperties() {
        Collection<Property> result = cached;
        if (result == null) {
            result = loadProperties();
            cached = result;
        }
        return result;
    }

    private static Collection<Property> loadProperties() {
        Collection<Property> answer = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();

        for (ConfigSource source : ConfigProvider.getConfig().getConfigSources()) {
            String sourceName = source.getName();
            if (!isPropertiesSource(sourceName)) {
                continue;
            }
            for (String name : source.getPropertyNames()) {
                if (seen.add(name)) {
                    try {
                        String value = source.getValue(name);
                        if (value != null) {
                            answer.add(new Property(name, value, "Quarkus"));
                        }
                    } catch (Exception e) {
                        // ignore properties that cannot be resolved
                    }
                }
            }
        }
        return List.copyOf(answer);
    }

    private static boolean isPropertiesSource(String name) {
        return name != null && name.startsWith("PropertiesConfigSource");
    }
}
