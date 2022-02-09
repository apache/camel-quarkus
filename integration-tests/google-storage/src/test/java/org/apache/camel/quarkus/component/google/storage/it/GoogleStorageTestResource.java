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
package org.apache.camel.quarkus.component.google.storage.it;

import java.util.HashMap;
import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.apache.camel.quarkus.test.AvailablePortFinder;
import org.testcontainers.containers.FixedHostPortGenericContainer;
import org.testcontainers.containers.GenericContainer;

public class GoogleStorageTestResource implements QuarkusTestResourceLifecycleManager {

    public static final int PORT = AvailablePortFinder.getNextAvailable();
    public static final String CONTAINER_NAME = "fsouza/fake-gcs-server";

    private GenericContainer<?> container;

    @Override
    public Map<String, String> start() {

        Map<String, String> properties = new HashMap<>();

        if (GoogleStorageHelper.usingMockBackend()) {
            container = new FixedHostPortGenericContainer<>(CONTAINER_NAME)
                    .withFixedExposedPort(PORT, PORT)
                    .withCreateContainerCmdModifier(
                            it -> it.withEntrypoint("/bin/fake-gcs-server", "-scheme", "http", "-port", String.valueOf(PORT)));
            container.start();

            properties.put(GoogleStorageResource.PARAM_PORT, String.valueOf(PORT));
        }

        return properties;
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
