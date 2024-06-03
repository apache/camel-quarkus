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
package org.apache.camel.quarkus.component.paho.mqtt5.it;

import java.util.HashMap;
import java.util.Map;

import com.github.dockerjava.api.model.Ulimit;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.apache.camel.quarkus.test.AvailablePortFinder;
import org.apache.camel.util.CollectionHelper;
import org.eclipse.microprofile.config.ConfigProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.FixedHostPortGenericContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.TestcontainersConfiguration;

public class PahoMqtt5TestResource implements QuarkusTestResourceLifecycleManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(PahoMqtt5TestResource.class);
    private static final String IMAGE = ConfigProvider.getConfig().getValue("eclipse-mosquitto.container.image", String.class);
    private static final int TCP_PORT = 1883;
    private static final int SSL_PORT = 8883;
    private static final int WS_PORT = 9001;
    private static final String MQTT_USERNAME = "quarkus";
    private static final String MQTT_PASSWORD = "quarkus";

    private GenericContainer<?> container;
    private boolean useFixedPort = false;
    private boolean startContainer = true;

    @Override
    public void init(Map<String, String> initArgs) {
        initArgs.forEach((name, value) -> {
            if (name.equals("useFixedPort")) {
                useFixedPort = Boolean.parseBoolean(value);
            } else if (name.equals("startContainer")) {
                startContainer = Boolean.parseBoolean(value);
            }
        });
    }

    @Override
    public void inject(TestInjector testInjector) {
        testInjector.injectIntoFields(container,
                new TestInjector.AnnotatedAndMatchesType(InjectPahoContainer.class, GenericContainer.class));
    }

    @Override
    public Map<String, String> start() {
        LOGGER.info(TestcontainersConfiguration.getInstance().toString());

        try {
            Map<String, String> result = new HashMap<>();

            if (useFixedPort) {
                int port = AvailablePortFinder.getNextAvailable();

                container = new FixedHostPortGenericContainer<>(IMAGE)
                        .withFixedExposedPort(port, TCP_PORT)
                        .withClasspathResourceMapping("mosquitto-no-auth.conf", "/mosquitto/config/mosquitto.conf",
                                BindMode.READ_ONLY);

                result = CollectionHelper.mapOf(
                        "paho5.broker.tcp.url", "tcp://" + container.getHost() + ":" + port);
            } else {
                container = new GenericContainer<>(IMAGE)
                        .withExposedPorts(TCP_PORT, WS_PORT, SSL_PORT)
                        .withClasspathResourceMapping("mosquitto.conf", "/mosquitto/config/mosquitto.conf", BindMode.READ_ONLY)
                        .withClasspathResourceMapping("password.conf", "/etc/mosquitto/password", BindMode.READ_ONLY)
                        .withClasspathResourceMapping("certs/paho-mqtt5-ca.crt", "/etc/mosquitto/certs/paho-mqtt5-ca.crt",
                                BindMode.READ_ONLY)
                        .withClasspathResourceMapping("certs/paho-mqtt5.crt", "/etc/mosquitto/certs/paho-mqtt5.crt",
                                BindMode.READ_ONLY)
                        .withClasspathResourceMapping("certs/paho-mqtt5.key", "/etc/mosquitto/certs/paho-mqtt5.key",
                                BindMode.READ_ONLY);
            }

            container.withLogConsumer(new Slf4jLogConsumer(LOGGER))
                    .waitingFor(Wait.forLogMessage(".* mosquitto version .* running.*", 1))
                    .withCreateContainerCmdModifier(
                            cmd -> cmd.getHostConfig().withUlimits(new Ulimit[] { new Ulimit("nofile", 512L, 512L) }));

            if (startContainer) {
                container.start();
            }

            if (!useFixedPort) {
                result = CollectionHelper.mapOf(
                        "camel.component.paho-mqtt5.username", MQTT_USERNAME,
                        "camel.component.paho-mqtt5.password", MQTT_PASSWORD,
                        "paho5.broker.host", container.getHost(),
                        "paho5.broker.tcp.url",
                        String.format("tcp://%s:%d", container.getHost(), container.getMappedPort(TCP_PORT)),
                        "paho5.broker.ssl.url",
                        String.format("ssl://%s:%d", container.getHost(), container.getMappedPort(SSL_PORT)),
                        "paho5.broker.ws.url",
                        String.format("ws://%s:%d", container.getHost(), container.getMappedPort(WS_PORT)));
            }

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
            // ignored
        }
    }
}
