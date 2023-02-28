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
package org.apache.camel.quarkus.component.infinispan.common;

import java.util.HashMap;
import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.TestcontainersConfiguration;

public class InfinispanCommonServerTestResource implements QuarkusTestResourceLifecycleManager {
    protected static final String USER = "camel";
    protected static final String PASS = "camel";
    private static final Logger LOGGER = LoggerFactory.getLogger(InfinispanCommonServerTestResource.class);
    private static final String CONTAINER_IMAGE = System.getProperty("infinispan.container.image", "infinispan/server:14.0");
    private static final int HOTROD_PORT = 11222;

    private GenericContainer<?> container;

    @Override
    public Map<String, String> start() {
        LOGGER.info(TestcontainersConfiguration.getInstance().toString());

        container = new GenericContainer<>(CONTAINER_IMAGE)
                .withExposedPorts(HOTROD_PORT)
                .withEnv("USER", USER)
                .withEnv("PASS", PASS)
                .withClasspathResourceMapping("infinispan.xml", "/user-config/infinispan.xml", BindMode.READ_ONLY)
                .withCommand("-c", "/user-config/infinispan.xml")
                .waitingFor(Wait.forListeningPort());

        container.start();

        return new HashMap<>();
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

    protected String getServerList() {
        return String.format("%s:%s", container.getHost(), container.getMappedPort(HOTROD_PORT));
    }
}
