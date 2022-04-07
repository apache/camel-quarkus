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
package org.apache.camel.quarkus.component.mail;

import java.util.HashMap;
import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.utility.DockerImageName;

public class MailTestResource implements QuarkusTestResourceLifecycleManager {

    private static final String GREENMAIL_IMAGE_NAME = "greenmail/standalone:1.6.7";
    private GenericContainer<?> container;

    @Override
    public Map<String, String> start() {
        container = new GenericContainer<>(DockerImageName.parse(GREENMAIL_IMAGE_NAME))
                .withExposedPorts(MailProtocol.allPorts())
                .waitingFor(new HttpWaitStrategy()
                        .forPort(MailProtocol.API.getPort())
                        .forPath("/api/service/readiness")
                        .forStatusCode(200));

        container.start();

        Map<String, String> options = new HashMap<>();
        for (MailProtocol protocol : MailProtocol.values()) {
            String optionName = String.format("mail.%s.port", protocol.name().toLowerCase());
            Integer mappedPort = container.getMappedPort(protocol.getPort());
            options.put(optionName, mappedPort.toString());
        }

        return options;
    }

    @Override
    public void stop() {
        if (container != null) {
            container.stop();
        }
    }

    enum MailProtocol {
        SMTP(3025),
        POP3(3110),
        IMAP(3143),
        SMTPS(3465),
        IMAPS(3993),
        POP3s(3995),
        API(8080);

        private final int port;

        MailProtocol(int port) {
            this.port = port;
        }

        public int getPort() {
            return port;
        }

        public static Integer[] allPorts() {
            MailProtocol[] values = values();
            Integer[] ports = new Integer[values.length];
            for (int i = 0; i < values.length; i++) {
                ports[i] = values[i].getPort();
            }
            return ports;
        }
    }
}
