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

import java.util.Map;

import org.apache.camel.impl.console.PropertiesDevConsole;
import org.apache.camel.util.json.JsonArray;
import org.apache.camel.util.json.JsonObject;

/**
 * Temporary override of the Camel {@link PropertiesDevConsole} that filters out entries originating from
 * {@code CamelMicroProfilePropertiesSource}. Those entries duplicate the application properties that Quarkus already
 * provides through the {@code RuntimePropertiesProvider} SPI and would otherwise appear twice in the console output.
 */
public class QuarkusPropertiesDevConsole extends PropertiesDevConsole {

    private static final String MICRO_PROFILE_SOURCE = "CamelMicroProfilePropertiesSource";

    @Override
    protected JsonObject doCallJson(Map<String, Object> options) {
        JsonObject root = super.doCallJson(options);
        Object props = root.get("properties");
        if (props instanceof JsonArray arr) {
            arr.removeIf(entry -> entry instanceof JsonObject obj
                    && obj.get("location") instanceof String loc
                    && loc.startsWith(MICRO_PROFILE_SOURCE));
        }
        return root;
    }

    @Override
    protected String doCallText(Map<String, Object> options) {
        StringBuilder filtered = new StringBuilder();
        for (String line : super.doCallText(options).split("\n")) {
            if (!line.contains(MICRO_PROFILE_SOURCE)) {
                filtered.append(line).append('\n');
            }
        }
        return filtered.toString();
    }
}
