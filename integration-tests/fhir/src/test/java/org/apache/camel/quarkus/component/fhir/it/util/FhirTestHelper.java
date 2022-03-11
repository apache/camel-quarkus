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
package org.apache.camel.quarkus.component.fhir.it.util;

import java.io.IOException;
import java.util.Properties;

import org.apache.camel.quarkus.component.fhir.it.AbstractFhirRouteBuilder;

public final class FhirTestHelper {

    private FhirTestHelper() {
        // Utility class
    }

    public static boolean isFhirVersionEnabled(String version) {
        try {
            Properties properties = new Properties();
            properties.load(AbstractFhirRouteBuilder.class.getResourceAsStream("/application.properties"));
            String key = "quarkus.camel.fhir.enable-" + version.toLowerCase();
            String envKey = key.toUpperCase().replace('.', '_');
            String value = (String) properties.getOrDefault(key, System.getProperty(key, System.getenv(envKey)));
            return Boolean.valueOf(value);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
