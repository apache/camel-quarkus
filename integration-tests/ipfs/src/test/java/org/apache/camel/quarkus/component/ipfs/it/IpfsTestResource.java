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
package org.apache.camel.quarkus.component.ipfs.it;

import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.apache.camel.util.CollectionHelper;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

public class IpfsTestResource implements QuarkusTestResourceLifecycleManager {

    private static final String IPFS_IMAGE = "ipfs/go-ipfs:v0.4.11";
    private static final int IPFS_PORT = 5001;

    private GenericContainer<?> container;

    @Override
    public Map<String, String> start() {
        container = new GenericContainer<>(IPFS_IMAGE)
                .withExposedPorts(IPFS_PORT)
                .waitingFor(Wait.forLogMessage(".*Daemon is ready.*", 1));
        container.start();

        return CollectionHelper.mapOf("camel.component.ipfs.ipfs-host", "127.0.0.1",
                "camel.component.ipfs.ipfs-port", container.getMappedPort(IPFS_PORT).toString());
    }

    @Override
    public void stop() {
        try {
            if (container != null) {
                container.stop();
            }
        } catch (Exception e) {
            // Ignored
        }
    }
}
