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
package org.apache.camel.quarkus.component.lra.it;

import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.apache.camel.quarkus.test.AvailablePortFinder;
import org.apache.camel.util.CollectionHelper;
import org.apache.commons.lang3.SystemUtils;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

public class LraTestResource implements QuarkusTestResourceLifecycleManager {

    private static final Integer LRA_PORT = AvailablePortFinder.getNextAvailable();
    private static final String LRA_IMAGE = "jbosstm/lra-coordinator:5.12.0.Final";

    private GenericContainer container;
    private String hostname;
    private int lraPort;

    @Override
    public Map<String, String> start() {
        try {
            if (SystemUtils.IS_OS_LINUX) {
                hostname = "localhost";
                container = new GenericContainer(LRA_IMAGE)
                        .withNetworkMode("host")
                        .withEnv("JAVA_OPTS", "-Dquarkus.http.port=" + LRA_PORT)
                        .waitingFor(Wait.forLogMessage(".*lra-coordinator-quarkus.*", 1));
                container.start();
                lraPort = LRA_PORT;
            } else {
                hostname = "host.docker.internal";
                container = new GenericContainer(LRA_IMAGE)
                        .withNetworkMode("bridge")
                        .withExposedPorts(LRA_PORT)
                        .withEnv("JAVA_OPTS", "-Dquarkus.http.port=" + LRA_PORT)
                        .waitingFor(Wait.forLogMessage(".*lra-coordinator-quarkus.*", 1));
                container.start();
                lraPort = container.getMappedPort(LRA_PORT);
            }

            return CollectionHelper.mapOf(
                    "camel.lra.coordinator-url",
                    String.format("http://%s:%d", container.getContainerIpAddress(), lraPort),
                    "camel.lra.local-participant-url",
                    String.format("http://%s:%s", hostname,
                            System.getProperty("quarkus.http.test-port", "8081")));
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
