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
package org.apache.camel.quarkus.component.jsch.it;

import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.apache.camel.util.CollectionHelper;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

public class JschTestResource implements QuarkusTestResourceLifecycleManager {
    private static final String JSCH_IMAGE = "linuxserver/openssh-server";
    private static final String USERNAME = "admin";
    private static final String PASSWORD = "2s3cr3t!";
    private static final int JSCH_PORT = 2222;

    private GenericContainer container;

    @Override
    public Map<String, String> start() {
        try {
            container = new GenericContainer(JSCH_IMAGE)
                    .withExposedPorts(JSCH_PORT)
                    .withEnv("DOCKER_MODS", "linuxserver/mods:openssh-server-openssh-client")
                    .withEnv("PASSWORD_ACCESS", "true")
                    .withEnv("USER_NAME", USERNAME)
                    .withEnv("USER_PASSWORD", PASSWORD)
                    .waitingFor(Wait.forListeningPort());

            container.start();

            return CollectionHelper.mapOf(
                    "jsch.host", container.getHost(),
                    "jsch.port", container.getMappedPort(JSCH_PORT).toString(),
                    "jsch.username", USERNAME,
                    "jsch.password", PASSWORD);
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
