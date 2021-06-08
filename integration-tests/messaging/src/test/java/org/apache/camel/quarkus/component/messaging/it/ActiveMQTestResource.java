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
package org.apache.camel.quarkus.component.messaging.it;

import java.nio.file.Paths;
import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ;
import org.apache.camel.util.CollectionHelper;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ActiveMQTestResource implements QuarkusTestResourceLifecycleManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ActiveMQTestResource.class);
    private static final String ACTIVEMQ_USERNAME = "artemis";
    private static final String ACTIVEMQ_PASSWORD = "simetraehcapa";
    private static final int ACTIVEMQ_PORT = 61616;

    private EmbeddedActiveMQ embedded;

    @Override
    public Map<String, String> start() {
        LOGGER.info("start embedded ActiveMQ server");

        try {
            FileUtils.deleteDirectory(Paths.get("./target/artemis").toFile());
            embedded = new EmbeddedActiveMQ();
            embedded.start();

            String brokerUrlTcp = String.format("tcp://127.0.0.1:%d", ACTIVEMQ_PORT);
            String brokerUrlWs = String.format("ws://127.0.0.1:%d", ACTIVEMQ_PORT);

            return CollectionHelper.mapOf(
                    "quarkus.artemis.url", brokerUrlTcp,
                    "quarkus.artemis.username", ACTIVEMQ_USERNAME,
                    "quarkus.artemis.password", ACTIVEMQ_PASSWORD,
                    "camel.component.paho.brokerUrl", brokerUrlTcp,
                    "camel.component.paho.username", ACTIVEMQ_USERNAME,
                    "camel.component.paho.password", ACTIVEMQ_PASSWORD,
                    "broker-url.ws", brokerUrlWs);

        } catch (Exception e) {
            throw new RuntimeException("Could not start embedded ActiveMQ server", e);
        }
    }

    @Override
    public void stop() {
        if (embedded == null) {
            return;
        }
        try {
            embedded.stop();
        } catch (Exception e) {
            throw new RuntimeException("Could not stop embedded ActiveMQ server", e);
        }
    }
}
