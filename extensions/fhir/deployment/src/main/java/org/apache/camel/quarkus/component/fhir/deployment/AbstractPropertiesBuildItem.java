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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import ca.uhn.fhir.context.FhirContext;
import io.quarkus.builder.item.SimpleBuildItem;

public abstract class AbstractPropertiesBuildItem extends SimpleBuildItem {

    private final Map<String, String> properties;

    protected AbstractPropertiesBuildItem(String path) {
        this.properties = Collections.unmodifiableMap(loadProperties(path));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    static Map<String, String> loadProperties(String path) {
        try (InputStream str = FhirContext.class.getClassLoader().getResourceAsStream(path)) {
            Properties prop = new Properties();
            prop.load(str);
            return new HashMap<String, String>((Map) prop);
        } catch (Exception e) {
            throw new RuntimeException("Please ensure FHIR is on the classpath", e);
        }
    }

    public Map<String, String> getProperties() {
        return properties;
    }
}
