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

package org.apache.camel.quarkus.component.activemq.it;

import java.util.Collections;
import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.TestcontainersConfiguration;

public class ActiveMQTestResource implements QuarkusTestResourceLifecycleManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(ActiveMQTestResource.class);

    private static final String ACTIVEMQ_IMAGE = "rmohr/activemq:5.15.9-alpine";
    private static final int TCP_PORT = 61616;

    private GenericContainer<?> container;

    @Override
    public Map<String, String> start() {
        LOGGER.info(TestcontainersConfiguration.getInstance().toString());

        try {
            container = new GenericContainer<>(ACTIVEMQ_IMAGE)
                    .withExposedPorts(TCP_PORT)
                    .withLogConsumer(new Slf4jLogConsumer(LOGGER))
                    .waitingFor(Wait.forLogMessage(".*ActiveMQ.*started.*", 1));

            container.start();

            return Collections.singletonMap(
                    "camel.component.activemq.broker-url",
                    String.format(
                            "tcp://%s:%d?connectionTimeout=5000&tcpNoDelay=false&socket.OOBInline=false" +
                                    "&jms.checkForDuplicates=false&jms.redeliveryPolicy.redeliveryDelay=1000" +
                                    "&jms.prefetchPolicy.queuePrefetch=1000&jms.blobTransferPolicy.bufferSize=102400",
                            container.getHost(),
                            container.getMappedPort(TCP_PORT)));
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
