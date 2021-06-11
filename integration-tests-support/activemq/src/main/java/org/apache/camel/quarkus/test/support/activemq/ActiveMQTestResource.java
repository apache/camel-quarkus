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
package org.apache.camel.quarkus.test.support.activemq;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ;
import org.apache.camel.quarkus.test.AvailablePortFinder;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ActiveMQTestResource implements QuarkusTestResourceLifecycleManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ActiveMQTestResource.class);
    private static final String ACTIVEMQ_USERNAME = "artemis";
    private static final String ACTIVEMQ_PASSWORD = "simetraehcapa";

    private QuarkusEmbeddedMQ embedded;
    private String[] modules;

    @Override
    public void init(Map<String, String> initArgs) {
        initArgs.forEach((name, value) -> {
            if (name.equals("modules")) {
                modules = value.split(",");
            }
        });
    }

    @Override
    public Map<String, String> start() {
        LOGGER.info("start embedded ActiveMQ server");

        try {
            FileUtils.deleteDirectory(Paths.get("./target/artemis").toFile());
            int port = AvailablePortFinder.getNextAvailable();
            String brokerUrlTcp = String.format("tcp://127.0.0.1:%d", port);
            String brokerUrlWs = String.format("ws://127.0.0.1:%d", port);
            String brokerUrlAmqp = String.format("amqp://127.0.0.1:%d", port);

            embedded = new QuarkusEmbeddedMQ();

            embedded.init();
            embedded.getActiveMQServer().getConfiguration().addAcceptorConfiguration("activemq", brokerUrlTcp);
            embedded.getActiveMQServer().getConfiguration().addConnectorConfiguration("activemq", brokerUrlTcp);
            embedded.start();

            Map<String, String> result = new LinkedHashMap<>();

            if (modules != null) {
                Arrays.stream(modules).forEach(module -> {
                    if (module.equals("quarkus.artemis")) {
                        LOGGER.info("add module " + module);
                        result.put("quarkus.artemis.url", brokerUrlTcp);
                        result.put("quarkus.artemis.username", ACTIVEMQ_USERNAME);
                        result.put("quarkus.artemis.password", ACTIVEMQ_PASSWORD);
                    } else if (module.equals("quarkus.qpid-jms")) {
                        result.put("quarkus.qpid-jms.url", brokerUrlAmqp);
                        result.put("quarkus.qpid-jms.username", ACTIVEMQ_USERNAME);
                        result.put("quarkus.qpid-jms.password", ACTIVEMQ_PASSWORD);
                    } else if (module.startsWith("camel.component")) {
                        result.put(module + ".brokerUrl", brokerUrlTcp);
                        result.put(module + ".username", ACTIVEMQ_USERNAME);
                        result.put(module + ".password", ACTIVEMQ_PASSWORD);
                    } else if (module.equals("broker-url.ws")) {
                        result.put("broker-url.ws", brokerUrlWs);
                    }
                });
            }

            return result;
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

class QuarkusEmbeddedMQ extends EmbeddedActiveMQ {
    public void init() throws Exception {
        super.initStart();
    }
}
