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
package org.apache.camel.quarkus.component.oauth.it;

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

import static org.apache.camel.oauth.OAuth.*;

/**
 * Inspired from
 * https://github.com/apache/camel-quarkus/blob/main/integration-tests/kafka-oauth/src/test/java/org/apache/camel/quarkus/kafka/oauth/it/KafkaKeycloakTestResource.java
 */
public class OauthKeycloakTestResource implements QuarkusTestResourceLifecycleManager {

    private static final Logger LOG = LoggerFactory.getLogger(OauthKeycloakTestResource.class);
    private static final String REALM_JSON = "keycloak/realms/camel-realm.json";
    private static final String REALM_NAME = "camel";

    private KeycloakContainer keycloak;

    @Override
    public Map<String, String> start() {
        System.setProperty("keycloak.docker.image",
                ConfigProvider.getConfig().getValue("keycloak.container.image", String.class));

        //Start keycloak container
        keycloak = new KeycloakContainer()
                .withStartupTimeout(Duration.ofMinutes(5))
                .withLogConsumer(new Slf4jLogConsumer(LOG))
                .withCopyFileToContainer(
                        MountableFile.forClasspathResource("/camel-realm.json"),
                        "/opt/keycloak/data/import/camel-realm.json")
                .withCommand("start", "--import-realm", "--verbose");

        keycloak.start();

        return Collections.singletonMap("cq.ouath.test.keycloak.uri", keycloak.getServerUrl() + "/realms/" + REALM_NAME);
    }

    @Override
    public void stop() {
        if (keycloak != null) {
            keycloak.stop();
        }
    }
}
