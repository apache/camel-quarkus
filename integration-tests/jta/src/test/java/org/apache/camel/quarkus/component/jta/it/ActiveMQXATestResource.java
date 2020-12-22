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
package org.apache.camel.quarkus.component.jta.it;

import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.apache.camel.util.CollectionHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.TestcontainersConfiguration;

public class ActiveMQXATestResource implements QuarkusTestResourceLifecycleManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ActiveMQXATestResource.class);
    private static final String ACTIVEMQ_IMAGE = "vromero/activemq-artemis:2.11.0-alpine";
    private static final String ACTIVEMQ_USERNAME = "artemis";
    private static final String ACTIVEMQ_PASSWORD = "simetraehcapa";
    private static final int ACTIVEMQ_PORT = 61616;

    private GenericContainer<?> container;

    @Override
    public Map<String, String> start() {
        LOGGER.info(TestcontainersConfiguration.getInstance().toString());

        try {
            container = new GenericContainer<>(ACTIVEMQ_IMAGE)
                    .withExposedPorts(ACTIVEMQ_PORT)
                    .withLogConsumer(new Slf4jLogConsumer(LOGGER))
                    .withEnv("BROKER_CONFIG_MAX_DISK_USAGE", "100")
                    .waitingFor(Wait.forListeningPort());

            container.start();

            String brokerUrlTcp = String.format("tcp://%s:%d/", container.getContainerIpAddress(),
                    container.getMappedPort(ACTIVEMQ_PORT));

            return CollectionHelper.mapOf(
                    "quarkus.artemis.url", brokerUrlTcp,
                    "quarkus.artemis.username", ACTIVEMQ_USERNAME,
                    "quarkus.artemis.password", ACTIVEMQ_PASSWORD);

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
