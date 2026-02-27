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
import java.util.UUID;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.quarkus.test.keycloak.server.KeycloakContainer;
import org.eclipse.microprofile.config.ConfigProvider;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;

public class KeycloakTestResource implements QuarkusTestResourceLifecycleManager {

    private static final String GREENMAIL_IMAGE_NAME = ConfigProvider.getConfig()
            .getValue("greenmail.container.image", String.class);
    private static final int SMTP_PORT = 3025;
    private static final int API_PORT = 8080;

    private KeycloakContainer keycloak;
    private GenericContainer<?> greenMail;
    private Network network;

    @Override
    public Map<String, String> start() {
        // Create a shared network for containers to communicate
        network = Network.newNetwork();

        // Start GreenMail container
        greenMail = new GenericContainer<>(GREENMAIL_IMAGE_NAME)
                .withNetwork(network)
                .withNetworkAliases("greenmail")
                .withExposedPorts(SMTP_PORT, API_PORT)
                .waitingFor(new HttpWaitStrategy()
                        .forPort(API_PORT)
                        .forPath("/api/service/readiness")
                        .forStatusCode(200));
        greenMail.start();

        // Set the Keycloak docker image property
        System.setProperty("keycloak.docker.image",
                ConfigProvider.getConfig().getValue("keycloak.container.image", String.class));

        // Start Keycloak container with network access to GreenMail
        keycloak = new KeycloakContainer();
        keycloak.withNetwork(network);
        keycloak.withStartupTimeout(Duration.ofMinutes(5));
        keycloak.start();

        // Get the configuration from the started containers
        Map<String, String> properties = new HashMap<>();
        properties.put("keycloak.url", keycloak.getServerUrl());
        properties.put("keycloak.username", "admin");
        properties.put("keycloak.password", "admin");
        properties.put("keycloak.realm", "master");
        properties.put("test.client.secret", "test-client-secret");
        properties.put("test.client.id", "token-binding-client-" + UUID.randomUUID().toString().substring(0, 8));
        properties.put("test.realm", "token-binding-realm-" + UUID.randomUUID().toString().substring(0, 8));

        // GreenMail SMTP configuration (accessible from host)
        properties.put("mail.smtp.host", greenMail.getHost());
        properties.put("mail.smtp.port", greenMail.getMappedPort(SMTP_PORT).toString());
        properties.put("mail.api.url",
                String.format("http://%s:%d", greenMail.getHost(), greenMail.getMappedPort(API_PORT)));

        return properties;
    }

    @Override
    public void stop() {
        if (keycloak != null) {
            keycloak.stop();
        }
        if (greenMail != null) {
            greenMail.stop();
        }
        if (network != null) {
            network.close();
        }
    }
}
