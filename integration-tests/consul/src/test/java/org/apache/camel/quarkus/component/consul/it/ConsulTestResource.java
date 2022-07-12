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
package org.apache.camel.quarkus.component.consul.it;

import java.util.Map;

import com.orbitz.consul.Consul;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.apache.camel.util.CollectionHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.TestcontainersConfiguration;

public class ConsulTestResource implements QuarkusTestResourceLifecycleManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConsulTestResource.class);
    private static final int CONTAINER_PORT = Consul.DEFAULT_HTTP_PORT;
    private static final String CONTAINER_IMAGE = "consul:1.6";

    private GenericContainer container;

    @Override
    public Map<String, String> start() {
        LOGGER.info(TestcontainersConfiguration.getInstance().toString());

        try {
            container = new GenericContainer(CONTAINER_IMAGE)
                    .withExposedPorts(CONTAINER_PORT)
                    .withCommand("agent", "-dev", "-server", "-bootstrap", "-client", "0.0.0.0", "-log-level", "trace")
                    .waitingFor(Wait.forLogMessage(".*Synced node info.*", 1));

            container.start();

            return CollectionHelper.mapOf(
                    "camel.consul.test-url",
                    String.format("http://%s:%d",
                            container.getHost(),
                            container.getMappedPort(CONTAINER_PORT)));
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
