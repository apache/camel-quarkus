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
package org.apache.camel.quarkus.component.infinispan;

import java.util.Map;

import org.apache.camel.quarkus.testcontainers.ContainerResourceLifecycleManager;
import org.apache.camel.util.CollectionHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.TestcontainersConfiguration;

import static org.apache.camel.quarkus.testcontainers.ContainerSupport.getHostAndPort;

public class InfinispanServerTestResource implements ContainerResourceLifecycleManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(InfinispanServerTestResource.class);
    private static final String CONTAINER_IMAGE = "docker.io/infinispan/server:10.1.5.Final";
    private static final int HOTROD_PORT = 11222;
    private static final String USER = "camel";
    private static final String PASS = "camel";

    private GenericContainer<?> container;

    @Override
    public Map<String, String> start() {
        LOGGER.info(TestcontainersConfiguration.getInstance().toString());

        try {
            container = new GenericContainer<>(CONTAINER_IMAGE)
                    .withExposedPorts(HOTROD_PORT)
                    .withEnv("USER", USER)
                    .withEnv("PASS", PASS)
                    .waitingFor(Wait.forListeningPort());

            container.start();

            return CollectionHelper.mapOf(
                    "quarkus.infinispan-client.server-list", getHostAndPort(container, HOTROD_PORT),
                    "quarkus.infinispan-client.near-cache-max-entries", "3",
                    "quarkus.infinispan-client.auth-username", USER,
                    "quarkus.infinispan-client.auth-password", PASS,
                    "quarkus.infinispan-client.auth-realm", "default",
                    "quarkus.infinispan-client.sasl-mechanism", "DIGEST-MD5",
                    "quarkus.infinispan-client.auth-server-name", "infinispan");
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
