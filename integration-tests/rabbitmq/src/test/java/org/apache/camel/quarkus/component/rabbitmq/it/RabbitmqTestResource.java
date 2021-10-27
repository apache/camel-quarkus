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
package org.apache.camel.quarkus.component.rabbitmq.it;

import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.apache.camel.util.CollectionHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.TestcontainersConfiguration;

public class RabbitmqTestResource implements QuarkusTestResourceLifecycleManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(RabbitmqTestResource.class);
    private static final String RABBITMQ_IMAGE = "rabbitmq:3.8.7-alpine";
    private static final int RABBITMQ_PORT = 5672;
    private static final String RABBITMQ_USERNAME = "guest";
    private static final String RABBITMQ_PASSWORD = "guest";

    private GenericContainer<?> container;

    @Override
    public Map<String, String> start() {
        LOGGER.info(TestcontainersConfiguration.getInstance().toString());

        try {
            container = new GenericContainer<>(RABBITMQ_IMAGE)
                    .withExposedPorts(RABBITMQ_PORT)
                    .withLogConsumer(new Slf4jLogConsumer(LOGGER))
                    .waitingFor(Wait.forLogMessage(".*Server startup complete.*", 1));

            container.start();

            return CollectionHelper.mapOf(
                    "camel.component.rabbitmq.port-number", container.getMappedPort(RABBITMQ_PORT).toString(),
                    "camel.component.rabbitmq.hostname", container.getHost(),
                    "camel.component.rabbitmq.username", RABBITMQ_USERNAME,
                    "camel.component.rabbitmq.password", RABBITMQ_PASSWORD,
                    "camel.component.rabbitmq.declare", "true");
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
            // Ignored
        }
    }
}
