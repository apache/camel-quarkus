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

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.apache.camel.util.CollectionHelper;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.TestcontainersConfiguration;

public class InfinispanServerTestResource implements QuarkusTestResourceLifecycleManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(InfinispanServerTestResource.class);
    private static final String CONTAINER_IMAGE = System.getProperty("infinispan.container.image", "infinispan/server:13.0");
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
                    .withClasspathResourceMapping("infinispan.xml", "/user-config/infinispan.xml", BindMode.READ_ONLY)
                    .withCommand("-c", "/user-config/infinispan.xml")
                    .waitingFor(Wait.forListeningPort());

            container.start();

            String serverList = String.format("%s:%s", container.getContainerIpAddress(),
                    container.getMappedPort(HOTROD_PORT));

            // Create 2 sets of configuration to test scenarios:
            // - Quarkus Infinispan client bean being autowired into the Camel Infinispan component
            // - Component configuration where the Infinispan client is managed by Camel (E.g Infinispan client autowiring disabled)
            Map<String, String> result = CollectionHelper.mapOf(
                    // quarkus
                    "quarkus.infinispan-client.server-list", serverList,
                    "quarkus.infinispan-client.auth-username", USER,
                    "quarkus.infinispan-client.auth-password", PASS,
                    "quarkus.infinispan-client.auth-realm", "default",
                    "quarkus.infinispan-client.sasl-mechanism", "DIGEST-MD5",
                    "quarkus.infinispan-client.auth-server-name", "infinispan",
                    // camel
                    "camel.component.infinispan.autowired-enabled", "false",
                    "camel.component.infinispan.hosts", serverList,
                    "camel.component.infinispan.username", USER,
                    "camel.component.infinispan.password", PASS,
                    "camel.component.infinispan.secure", "true",
                    "camel.component.infinispan.security-realm", "default",
                    "camel.component.infinispan.sasl-mechanism", "DIGEST-MD5",
                    "camel.component.infinispan.security-server-name",
                    "infinispan");
            if (SystemUtils.IS_OS_MAC || SystemUtils.IS_OS_WINDOWS) {
                /* Fix for https://github.com/apache/camel-quarkus/issues/2840 */
                result.put("quarkus.infinispan-client.client-intelligence", "BASIC");
            }
            return result;
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
