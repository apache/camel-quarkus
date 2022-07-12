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

import java.util.HashMap;
import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.SelinuxContext;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.TestcontainersConfiguration;

import static org.apache.camel.quarkus.component.nats.it.NatsConfiguration.NATS_BROKER_URL_BASIC_AUTH_CONFIG_KEY;
import static org.apache.camel.quarkus.component.nats.it.NatsConfiguration.NATS_BROKER_URL_NO_AUTH_CONFIG_KEY;
import static org.apache.camel.quarkus.component.nats.it.NatsConfiguration.NATS_BROKER_URL_TLS_AUTH_CONFIG_KEY;
import static org.apache.camel.quarkus.component.nats.it.NatsConfiguration.NATS_BROKER_URL_TOKEN_AUTH_CONFIG_KEY;

public class NatsTestResource implements QuarkusTestResourceLifecycleManager {

    private static final Logger LOG = LoggerFactory.getLogger(NatsTestResource.class);
    private static final String BASIC_AUTH_USERNAME = "admin";
    private static final String BASIC_AUTH_PASSWORD = "password";
    private static final String NATS_IMAGE = "nats:2.1.9";
    private static final int NATS_SERVER_PORT = 4222;
    private static final String TOKEN_AUTH_TOKEN = "!admin23456";

    private GenericContainer<?> basicAuthContainer, noAuthContainer, tlsAuthContainer, tokenAuthContainer;

    @Override
    public Map<String, String> start() {
        LOG.info(TestcontainersConfiguration.getInstance().toString());

        Map<String, String> properties = new HashMap<>();

        basicAuthContainer = basicAuthContainer(properties);
        noAuthContainer = noAuthContainer(properties);
        tokenAuthContainer = tokenAuthContainer(properties);

        if ("true".equals(System.getenv("ENABLE_TLS_TESTS"))) {
            LOG.info("TLS tests enabled so starting the TLS auth container");
            tlsAuthContainer = tlsAuthContainer(properties);
        } else {
            LOG.info("TLS tests NOT enabled, so NOT starting the TLS auth container");
        }

        LOG.info("Properties: {}", properties);

        return properties;
    }

    @Override
    public void stop() {
        stop(basicAuthContainer);
        stop(noAuthContainer);
        stop(tlsAuthContainer);
        stop(tokenAuthContainer);
    }

    private void stop(GenericContainer<?> container) {
        try {
            if (container != null) {
                container.stop();
            }
        } catch (Exception ex) {
            LOG.error("An issue occurred while stopping " + container.getNetworkAliases(), ex);
        }
    }

    private static GenericContainer<?> basicAuthContainer(Map<String, String> properties) {
        LOG.info("Starting basicAuthContainer");
        // container needed for the basic authentication test
        GenericContainer<?> container = new GenericContainer<>(NATS_IMAGE)
                .withExposedPorts(NATS_SERVER_PORT)
                .withNetworkAliases("basicAuthContainer")
                .withCommand("-DV", "--user", BASIC_AUTH_USERNAME, "--pass", BASIC_AUTH_PASSWORD)
                .withLogConsumer(new Slf4jLogConsumer(LOG).withPrefix("basicAuthContainer"))
                .waitingFor(Wait.forLogMessage(".*Server is ready.*", 1));

        container.start();

        String basicAuthIp = container.getHost();
        Integer basicAuthPort = container.getMappedPort(NATS_SERVER_PORT);
        String basicAuthAuthority = BASIC_AUTH_USERNAME + ":" + BASIC_AUTH_PASSWORD;
        String basicAuthBrokerUrl = String.format("%s@%s:%d", basicAuthAuthority, basicAuthIp, basicAuthPort);

        properties.put(NATS_BROKER_URL_BASIC_AUTH_CONFIG_KEY, basicAuthBrokerUrl);

        return container;
    }

    private static GenericContainer<?> noAuthContainer(Map<String, String> properties) {
        LOG.info("Starting noAuthContainer");
        // container needed for the basic authentication test
        GenericContainer<?> container = new GenericContainer<>(NATS_IMAGE)
                .withExposedPorts(NATS_SERVER_PORT)
                .withNetworkAliases("noAuthContainer")
                .withLogConsumer(new Slf4jLogConsumer(LOG).withPrefix("noAuthContainer"))
                .waitingFor(Wait.forLogMessage(".*Listening for route connections.*", 1));

        container.start();

        String noAuthIp = container.getHost();
        Integer noAuthPort = container.getMappedPort(NATS_SERVER_PORT);
        String noAuthBrokerUrl = String.format("%s:%s", noAuthIp, noAuthPort);

        properties.put(NATS_BROKER_URL_NO_AUTH_CONFIG_KEY, noAuthBrokerUrl);

        return container;
    }

    private static GenericContainer<?> tlsAuthContainer(Map<String, String> properties) {
        LOG.info("Starting tlsAuthContainer");
        // Start the container needed for the TLS authentication test
        GenericContainer<?> container = new GenericContainer<>(NATS_IMAGE)
                .withExposedPorts(NATS_SERVER_PORT)
                .withNetworkAliases("tlsAuthContainer")
                .withClasspathResourceMapping("certs/ca.pem", "/certs/ca.pem", BindMode.READ_ONLY, SelinuxContext.SHARED)
                .withClasspathResourceMapping("certs/key.pem", "/certs/key.pem", BindMode.READ_ONLY, SelinuxContext.SHARED)
                .withClasspathResourceMapping("certs/server.pem", "/certs/server.pem", BindMode.READ_ONLY,
                        SelinuxContext.SHARED)
                .withClasspathResourceMapping("conf/tls.conf", "/conf/tls.conf", BindMode.READ_ONLY, SelinuxContext.SHARED)
                .withCommand(
                        "--config", "/conf/tls.conf",
                        "--tls",
                        "--tlscert=/certs/server.pem",
                        "--tlskey=/certs/key.pem",
                        "--tlsverify",
                        "--tlscacert=/certs/ca.pem")
                .withLogConsumer(new Slf4jLogConsumer(LOG).withPrefix("tlsAuthContainer"))
                .waitingFor(Wait.forLogMessage(".*Server is ready.*", 1));
        try {
            container.start();
        } catch (Exception ex) {
            throw new RuntimeException("An issue occurred while starting tlsAuthContainer: " + container.getLogs(), ex);
        }

        container.start();

        String tlsAuthIp = container.getHost();
        Integer tlsAuthPort = container.getMappedPort(NATS_SERVER_PORT);
        String tlsAuthBrokerUrl = String.format("%s:%d", tlsAuthIp, tlsAuthPort);

        properties.put(NATS_BROKER_URL_TLS_AUTH_CONFIG_KEY, tlsAuthBrokerUrl);

        return container;
    }

    private static GenericContainer<?> tokenAuthContainer(Map<String, String> properties) {
        LOG.info("Starting tokenAuthContainer");
        // Start the container needed for the token authentication test
        GenericContainer<?> container = new GenericContainer<>(NATS_IMAGE)
                .withExposedPorts(NATS_SERVER_PORT)
                .withNetworkAliases("tokenAuthContainer")
                .withCommand("-DV", "-auth", TOKEN_AUTH_TOKEN)
                .withLogConsumer(new Slf4jLogConsumer(LOG).withPrefix("tokenAuthContainer"))
                .waitingFor(Wait.forLogMessage(".*Server is ready.*", 1));

        container.start();

        String tokenAuthIp = container.getHost();
        Integer tokenAuthPort = container.getMappedPort(NATS_SERVER_PORT);
        String tokenAuthBrokerUrl = String.format("%s@%s:%d", TOKEN_AUTH_TOKEN, tokenAuthIp, tokenAuthPort);

        properties.put(NATS_BROKER_URL_TOKEN_AUTH_CONFIG_KEY, tokenAuthBrokerUrl);

        return container;
    }
}
