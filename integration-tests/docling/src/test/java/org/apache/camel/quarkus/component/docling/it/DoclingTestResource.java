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
import java.util.Map;

import org.apache.camel.quarkus.test.mock.backend.MockBackendUtils;
import org.apache.camel.quarkus.test.wiremock.WireMockTestResourceLifecycleManager;
import org.eclipse.microprofile.config.ConfigProvider;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

public class DoclingTestResource extends WireMockTestResourceLifecycleManager {

    private static final String CONTAINER_IMAGE = ConfigProvider.getConfig().getValue("docling.container.image",
            String.class);
    private static final int CONTAINER_PORT = 5001;

    private GenericContainer<?> container;
    private String containerUrl;

    @Override
    public Map<String, String> start() {
        // The real container is needed when recording stubs or when mock backend is disabled
        if (!isMockingEnabled() || isWireMockRecordingEnabled()) {
            startContainer();
        }

        Map<String, String> properties = super.start();

        // In mock/record mode, point the app at WireMock; otherwise point directly at the container
        String doclingUrl = properties.getOrDefault("wiremock.url", containerUrl);
        properties.put("docling.serve.url", doclingUrl);
        return properties;
    }

    @Override
    public void stop() {
        try {
            super.stop();
        } finally {
            if (container != null) {
                container.stop();
            }
        }
    }

    @Override
    protected String getRecordTargetBaseUrl() {
        return containerUrl;
    }

    @Override
    protected boolean isMockingEnabled() {
        return MockBackendUtils.startMockBackend();
    }

    private void startContainer() {
        LOG.infof("Starting Docling Serve container: %s", CONTAINER_IMAGE);
        container = new GenericContainer<>(CONTAINER_IMAGE)
                .withExposedPorts(CONTAINER_PORT)
                .waitingFor(Wait.forListeningPorts(CONTAINER_PORT))
                .withStartupTimeout(Duration.ofMinutes(3));
        container.start();
        containerUrl = "http://%s:%d".formatted(container.getHost(), container.getMappedPort(CONTAINER_PORT));
        LOG.infof("Docling Serve container started at: %s", containerUrl);
    }

    private static boolean isWireMockRecordingEnabled() {
        return "true".equals(System.getProperty("wiremock.record", System.getenv("WIREMOCK_RECORD")));
    }
}
