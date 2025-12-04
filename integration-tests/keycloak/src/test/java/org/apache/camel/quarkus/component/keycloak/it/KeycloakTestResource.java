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
package org.apache.camel.quarkus.component.keycloak.it;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.quarkus.test.keycloak.server.KeycloakContainer;
import org.eclipse.microprofile.config.ConfigProvider;

public class KeycloakTestResource implements QuarkusTestResourceLifecycleManager {

    private KeycloakContainer keycloak;

    @Override
    public Map<String, String> start() {
        // Set the Keycloak docker image property
        System.setProperty("keycloak.docker.image",
                ConfigProvider.getConfig().getValue("keycloak.container.image", String.class));

        // Start Keycloak container
        keycloak = new KeycloakContainer();
        keycloak.withStartupTimeout(Duration.ofMinutes(5));
        keycloak.start();

        // Get the configuration from the started container
        Map<String, String> properties = new HashMap<>();
        properties.put("keycloak.url", keycloak.getServerUrl());
        properties.put("keycloak.username", "admin");
        properties.put("keycloak.password", "admin");
        properties.put("keycloak.realm", "master");

        return properties;
    }

    @Override
    public void stop() {
        if (keycloak != null) {
            keycloak.stop();
        }
    }
}
