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
package org.apache.camel.quarkus.component.a2a.it;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.quarkus.test.keycloak.server.KeycloakContainer;
import org.eclipse.microprofile.config.ConfigProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.utility.MountableFile;

public class A2aKeycloakTestResource implements QuarkusTestResourceLifecycleManager {

    private static final Logger LOG = LoggerFactory.getLogger(A2aKeycloakTestResource.class);
    private static final String REALM_NAME = "camel";

    private KeycloakContainer keycloak;

    @Override
    public Map<String, String> start() {
        System.setProperty("keycloak.docker.image",
                ConfigProvider.getConfig().getValue("keycloak.container.image", String.class));

        keycloak = new KeycloakContainer()
                .withStartupTimeout(Duration.ofMinutes(5))
                .withLogConsumer(new Slf4jLogConsumer(LOG))
                .withCopyFileToContainer(
                        MountableFile.forClasspathResource("/camel-realm.json"),
                        "/opt/keycloak/data/import/camel-realm.json")
                .withCommand("start", "--import-realm", "--verbose");

        keycloak.start();

        return Collections.singletonMap("cq.a2a.test.keycloak.url",
                keycloak.getServerUrl() + "/realms/" + REALM_NAME);
    }

    @Override
    public void stop() {
        if (keycloak != null) {
            keycloak.stop();
        }
    }
}
