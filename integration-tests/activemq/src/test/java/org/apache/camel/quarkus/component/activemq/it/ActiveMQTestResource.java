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

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.broker.TransportConnector;
import org.apache.camel.quarkus.test.AvailablePortFinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ActiveMQTestResource implements QuarkusTestResourceLifecycleManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(ActiveMQTestResource.class);
    private BrokerService broker;

    @Override
    public Map<String, String> start() {
        try {
            final int port = AvailablePortFinder.getNextAvailable();
            final String brokerUrl = String.format("tcp://127.0.0.1:%d", port);
            final File dataDirectory = Files.createTempDirectory("activemq-data-").toFile();

            TransportConnector connector = new TransportConnector();
            connector.setUri(new URI(brokerUrl));

            broker = new BrokerService();
            broker.addConnector(connector);
            broker.setDataDirectoryFile(dataDirectory);
            broker.start();

            return Collections.singletonMap("camel.component.activemq.brokerUrl", brokerUrl);
        } catch (Exception e) {
            LOGGER.error("Starting the ActiveMQ broker with exception.", e);
            throw new RuntimeException("Starting the ActiveMQ broker with exception.", e);
        }
    }

    @Override
    public void stop() {
        try {
            if (broker != null) {
                broker.stop();
            }
        } catch (Exception e) {
            LOGGER.error("Stopping the ActiveMQ broker with exception.", e);
        }

        try {
            if (broker != null) {
                Files.walk(broker.getDataDirectoryFile().toPath())
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
        } catch (IOException e) {
            LOGGER.error("Error cleaning up ActiveMQ data directory", e);
        }
    }
}
