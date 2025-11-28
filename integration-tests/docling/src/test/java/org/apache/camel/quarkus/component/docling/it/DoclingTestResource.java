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
package org.apache.camel.quarkus.component.docling.it;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.eclipse.microprofile.config.ConfigProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

public class DoclingTestResource implements QuarkusTestResourceLifecycleManager {

    private static final Logger LOG = LoggerFactory.getLogger(DoclingTestResource.class);

    private static final String CONTAINER_IMAGE = ConfigProvider.getConfig().getValue("docling.container.image", String.class);
    private static final int CONTAINER_PORT = 5001;

    private GenericContainer<?> container;

    @Override
    public Map<String, String> start() {
        try {
            LOG.info("Starting Docling Serve container: {}", CONTAINER_IMAGE);

            container = new GenericContainer<>(CONTAINER_IMAGE)
                    .withExposedPorts(CONTAINER_PORT)
                    .waitingFor(Wait.forListeningPorts(CONTAINER_PORT))
                    .withStartupTimeout(Duration.ofMinutes(3));

            container.start();

            String doclingUrl = String.format("http://%s:%d",
                    container.getHost(),
                    container.getMappedPort(CONTAINER_PORT));

            LOG.info("Docling Serve container started at: {}", doclingUrl);

            Map<String, String> config = new HashMap<>();
            config.put("docling.serve.url", doclingUrl);

            return config;
        } catch (Exception e) {
            LOG.error("Failed to start Docling Serve container", e);
            throw new RuntimeException("Failed to start Docling Serve container", e);
        }
    }

    @Override
    public void stop() {
        try {
            if (container != null) {
                LOG.info("Stopping Docling Serve container");
                container.stop();
            }
        } catch (Exception e) {
            LOG.warn("Error stopping Docling Serve container", e);
        }
    }
}
