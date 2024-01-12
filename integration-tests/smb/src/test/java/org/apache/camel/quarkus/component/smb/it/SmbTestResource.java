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
package org.apache.camel.quarkus.component.smb.it;

import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;

public class SmbTestResource implements QuarkusTestResourceLifecycleManager {

    private static final int SMB_PORT = 445;

    private GenericContainer<?> container;

    @Override
    public Map<String, String> start() {
        try {
            container = new GenericContainer<>(new ImageFromDockerfile("localhost/samba:camel", false)
                    .withFileFromClasspath(".", "/"))
                    .withExposedPorts(SMB_PORT)
                    .waitingFor(Wait.forListeningPort());
            container.start();

            String smbHost = container.getHost();
            int smbPort = container.getMappedPort(SMB_PORT);

            return Map.of(
                    "smb.host", smbHost,
                    "smb.port", Integer.toString(smbPort),
                    "smb.share", "data-rw",
                    "smb.username", "camel",
                    "smb.password", "camelTester123");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void stop() {
        if (container != null) {
            container.stop();
        }
    }
}
