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
package org.apache.camel.quarkus.component.ssh.it;

import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.apache.camel.util.CollectionHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.TestcontainersConfiguration;

public class SshTestResource implements QuarkusTestResourceLifecycleManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(SshTestResource.class);

    private static final int SSH_PORT = 2222;
    private static final String SSH_IMAGE = "linuxserver/openssh-server";

    private GenericContainer container;

    @Override
    public Map<String, String> start() {
        LOGGER.info(TestcontainersConfiguration.getInstance().toString());
        LOGGER.info("Starting SSH container");

        try {
            container = new GenericContainer(SSH_IMAGE)
                    .withExposedPorts(SSH_PORT)
                    .withEnv("PASSWORD_ACCESS", "true")
                    .withEnv("USER_NAME", "test")
                    .withEnv("USER_PASSWORD", "password")
                    .waitingFor(Wait.forListeningPort());

            container.start();

            LOGGER.info("Started SSH container to {}:{}", container.getHost(),
                    container.getMappedPort(SSH_PORT).toString());

            return CollectionHelper.mapOf(
                    "quarkus.ssh.host",
                    container.getHost(),
                    "quarkus.ssh.port",
                    container.getMappedPort(SSH_PORT).toString());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void stop() {
        LOGGER.info("Stopping SSH container");

        try {
            if (container != null) {
                container.stop();
            }
        } catch (Exception e) {
            // ignored
        }
    }
}
