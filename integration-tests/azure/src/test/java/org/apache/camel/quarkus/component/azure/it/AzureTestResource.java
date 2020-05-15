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

package org.apache.camel.quarkus.component.azure.it;

import java.util.HashMap;
import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;

public class AzureTestResource implements QuarkusTestResourceLifecycleManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(AzureTestResource.class);
    private static final String AZURITE_IMAGE = "mcr.microsoft.com/azure-storage/azurite:3.6.0";
    private static final String AZURITE_CREDENTIALS = "DefaultEndpointsProtocol=http;AccountName="
            + "devstoreaccount1;AccountKey=Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==;"
            + "BlobEndpoint=%s;QueueEndpoint=%s;";
    private static final int BLOB_SERVICE_PORT = 10000;
    private static final int QUEUE_SERVICE_PORT = 10001;

    private GenericContainer<?> container;

    @Override
    public Map<String, String> start() {
        try {
            container = new GenericContainer<>(AZURITE_IMAGE)
                    .withExposedPorts(BLOB_SERVICE_PORT, QUEUE_SERVICE_PORT)
                    .withLogConsumer(new Slf4jLogConsumer(LOGGER))
                    .waitingFor(Wait.forListeningPort());
            container.start();

            String baseServiceUrl = "http://%s:%d/devstoreaccount1/";
            String blobServiceUrl = String.format(baseServiceUrl, container.getContainerIpAddress(),
                    container.getMappedPort(BLOB_SERVICE_PORT));
            String queueServiceUrl = String.format(baseServiceUrl, container.getContainerIpAddress(),
                    container.getMappedPort(QUEUE_SERVICE_PORT));

            Map<String, String> configuration = new HashMap<>();
            configuration.put("azurite.blob.service.url", blobServiceUrl);
            configuration.put("azurite.queue.service.url", queueServiceUrl);
            configuration.put("azurite.credentials", String.format(AZURITE_CREDENTIALS, blobServiceUrl, queueServiceUrl));
            return configuration;
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
