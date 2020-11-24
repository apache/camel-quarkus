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
package org.apache.camel.quarkus.component.nats.it;

import java.util.Map;

import org.apache.camel.quarkus.testcontainers.ContainerResourceLifecycleManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.TestcontainersConfiguration;

import static org.apache.camel.quarkus.component.nats.it.NatsConfiguration.NATS_BROKER_URL_BASIC_AUTH_CONFIG_KEY;
import static org.apache.camel.quarkus.component.nats.it.NatsConfiguration.NATS_BROKER_URL_NO_AUTH_CONFIG_KEY;
import static org.apache.camel.quarkus.component.nats.it.NatsConfiguration.NATS_BROKER_URL_TLS_AUTH_CONFIG_KEY;
import static org.apache.camel.quarkus.component.nats.it.NatsConfiguration.NATS_BROKER_URL_TOKEN_AUTH_CONFIG_KEY;
import static org.apache.camel.util.CollectionHelper.mapOf;

public class NatsTestResource implements ContainerResourceLifecycleManager {

    private static final Logger LOG = LoggerFactory.getLogger(NatsTestResource.class);
    private static final String BASIC_AUTH_USERNAME = "admin";
    private static final String BASIC_AUTH_PASSWORD = "password";
    private static final String NATS_IMAGE = "nats:2.1.9";
    private static final int NATS_SERVER_PORT = 4222;
    private static final String TOKEN_AUTH_TOKEN = "!admin23456";

    private GenericContainer basicAuthContainer, noAuthContainer, tlsAuthContainer, tokenAuthContainer;

    @Override
    public Map<String, String> start() {
        LOG.info(TestcontainersConfiguration.getInstance().toString());

        // Start the container needed for the basic authentication test
        basicAuthContainer = new GenericContainer(NATS_IMAGE).withExposedPorts(NATS_SERVER_PORT)
                .withCommand("-DV", "--user", BASIC_AUTH_USERNAME, "--pass", BASIC_AUTH_PASSWORD)
                .waitingFor(Wait.forLogMessage(".*Server is ready.*", 1));
        basicAuthContainer.start();
        String basicAuthIp = basicAuthContainer.getContainerIpAddress();
        Integer basicAuthPort = basicAuthContainer.getMappedPort(NATS_SERVER_PORT);
        String basicAuthAuthority = BASIC_AUTH_USERNAME + ":" + BASIC_AUTH_PASSWORD;
        String basicAuthBrokerUrl = String.format("%s@%s:%d", basicAuthAuthority, basicAuthIp, basicAuthPort);

        // Start the container needed for tests without authentication
        noAuthContainer = new GenericContainer(NATS_IMAGE).withExposedPorts(NATS_SERVER_PORT)
                .waitingFor(Wait.forLogMessage(".*Listening for route connections.*", 1));
        noAuthContainer.start();
        String noAuthIp = noAuthContainer.getContainerIpAddress();
        Integer noAuthPort = noAuthContainer.getMappedPort(NATS_SERVER_PORT);
        String noAuthBrokerUrl = String.format("%s:%s", noAuthIp, noAuthPort);

        // Start the container needed for the TLS authentication test
        tlsAuthContainer = new GenericContainer(NATS_IMAGE).withExposedPorts(NATS_SERVER_PORT)
                .withClasspathResourceMapping("certs", "/certs", BindMode.READ_ONLY)
                .withClasspathResourceMapping("conf", "/conf", BindMode.READ_ONLY)
                .withCommand(
                        "--config", "/conf/tls.conf",
                        "--tls",
                        "--tlscert=/certs/server.pem",
                        "--tlskey=/certs/key.pem",
                        "--tlsverify",
                        "--tlscacert=/certs/ca.pem")
                .waitingFor(Wait.forLogMessage(".*Server is ready.*", 1));
        tlsAuthContainer.start();
        String tlsAuthIp = tlsAuthContainer.getContainerIpAddress();
        Integer tlsAuthPort = tlsAuthContainer.getMappedPort(NATS_SERVER_PORT);
        String tlsAuthBrokerUrl = String.format("%s:%d", tlsAuthIp, tlsAuthPort);

        // Start the container needed for the token authentication test
        tokenAuthContainer = new GenericContainer(NATS_IMAGE).withExposedPorts(NATS_SERVER_PORT)
                .withCommand("-DV", "-auth", TOKEN_AUTH_TOKEN)
                .waitingFor(Wait.forLogMessage(".*Server is ready.*", 1));
        tokenAuthContainer.start();
        String tokenAuthIp = tokenAuthContainer.getContainerIpAddress();
        Integer tokenAuthPort = tokenAuthContainer.getMappedPort(NATS_SERVER_PORT);
        String tokenAuthBrokerUrl = String.format("%s@%s:%d", TOKEN_AUTH_TOKEN, tokenAuthIp, tokenAuthPort);

        Map<String, String> properties = mapOf(NATS_BROKER_URL_BASIC_AUTH_CONFIG_KEY, basicAuthBrokerUrl);
        properties.put(NATS_BROKER_URL_NO_AUTH_CONFIG_KEY, noAuthBrokerUrl);
        properties.put(NATS_BROKER_URL_TLS_AUTH_CONFIG_KEY, tlsAuthBrokerUrl);
        properties.put(NATS_BROKER_URL_TOKEN_AUTH_CONFIG_KEY, tokenAuthBrokerUrl);
        return properties;
    }

    @Override
    public void stop() {
        stop(basicAuthContainer, "natsBasicAuthContainer");
        stop(noAuthContainer, "natsNoAuthContainer");
        stop(tlsAuthContainer, "natsTlsAuthContainer");
        stop(tokenAuthContainer, "natsTokenAuthContainer");
    }

    private void stop(GenericContainer<?> container, String id) {
        try {
            if (container != null) {
                container.stop();
            }
        } catch (Exception ex) {
            LOG.error("An issue occured while stopping " + id, ex);
        }
    }
}
