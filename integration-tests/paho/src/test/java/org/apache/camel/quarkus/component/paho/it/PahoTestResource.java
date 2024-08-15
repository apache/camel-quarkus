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
package org.apache.camel.quarkus.component.paho.it;

import java.util.HashMap;
import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.apache.camel.quarkus.test.support.certificate.CertificatesUtil;
import org.apache.camel.util.CollectionHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.MountableFile;
import org.testcontainers.utility.TestcontainersConfiguration;

public class PahoTestResource implements QuarkusTestResourceLifecycleManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(PahoTestResource.class);
    //    private static final String IMAGE = ConfigProvider.getConfig().getValue("eclipse-mosquitto.container.image", String.class);
    private static final String IMAGE = "docker.io/eclipse-mosquitto:2.0.18";
    private static final int TCP_PORT = 1883;
    private static final int SSL_PORT = 8883;
    private static final int WS_PORT = 9001;
    private static final String MQTT_USERNAME = "quarkus";
    private static final String MQTT_PASSWORD = "quarkus";

    private GenericContainer<?> container;

    @Override
    public Map<String, String> start() {
        LOGGER.info(TestcontainersConfiguration.getInstance().toString());

        try {
            Map<String, String> result = new HashMap<>();

            container = new GenericContainer<>(IMAGE).withExposedPorts(TCP_PORT, WS_PORT, SSL_PORT)
                    .withClasspathResourceMapping("mosquitto.conf", "/mosquitto/config/mosquitto.conf", BindMode.READ_ONLY)
                    .withClasspathResourceMapping("password.conf", "/etc/mosquitto/password", BindMode.READ_ONLY)
                    .withCopyToContainer(MountableFile.forHostPath(CertificatesUtil.caCrt("paho")),
                            "/etc/mosquitto/certs/paho-ca.crt")
                    .withCopyToContainer(MountableFile.forHostPath(CertificatesUtil.crt("paho")),
                            "/etc/mosquitto/certs/paho.crt")
                    .withCopyToContainer(MountableFile.forHostPath(CertificatesUtil.key("paho")),
                            "/etc/mosquitto/certs/paho.key");
            container.withLogConsumer(new Slf4jLogConsumer(LOGGER))
                    .waitingFor(Wait.forLogMessage(".* mosquitto version .* running", 1)).waitingFor(Wait.forListeningPort());

            container.start();

            result = CollectionHelper.mapOf(
                    "camel.component.paho.username", MQTT_USERNAME,
                    "camel.component.paho.password", MQTT_PASSWORD,
                    "paho.broker.host", container.getHost(),
                    "paho.broker.tcp.url", String.format("tcp://%s:%d", container.getHost(), container.getMappedPort(TCP_PORT)),
                    "paho.broker.ssl.url", String.format("ssl://%s:%d", container.getHost(), container.getMappedPort(SSL_PORT)),
                    "paho.broker.ws.url", String.format("ws://%s:%d", container.getHost(), container.getMappedPort(WS_PORT)));

            return result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void stop() {
        try {
            if (container != null) {
                container.stop();
            }
        } catch (Exception e) {
            LOGGER.warn("Exception caught while stopping the container", e);
        }
    }
}
