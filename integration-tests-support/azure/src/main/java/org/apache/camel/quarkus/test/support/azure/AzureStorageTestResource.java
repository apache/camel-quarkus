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

package org.apache.camel.quarkus.test.support.azure;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import io.quarkus.runtime.configuration.ConfigUtils;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.smallrye.config.SmallRyeConfig;
import org.apache.camel.quarkus.test.mock.backend.MockBackendUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;

public class AzureStorageTestResource implements QuarkusTestResourceLifecycleManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(AzureStorageTestResource.class);
    private static final String AZURITE_IMAGE = "mcr.microsoft.com/azure-storage/azurite:3.9.0";
    private static final int BLOB_SERVICE_PORT = 10000;
    private static final int QUEUE_SERVICE_PORT = 10001;

    private GenericContainer<?> container;

    @Override
    public Map<String, String> start() {

        final SmallRyeConfig config = ConfigUtils.configBuilder(true).build();

        final String realAzureStorageAccountName = System.getenv("AZURE_STORAGE_ACCOUNT_NAME");
        final boolean realCredentialsProvided = realAzureStorageAccountName != null
                && System.getenv("AZURE_STORAGE_ACCOUNT_KEY") != null;

        final String azureBlobContainername = "camel-quarkus-" + UUID.randomUUID().toString();

        final String azureStorageAccountName = config
                .getValue("azure.storage.account-name", String.class);
        final boolean startMockBackend = MockBackendUtils.startMockBackend(false);
        final Map<String, String> result = new LinkedHashMap<>();
        if (startMockBackend && !realCredentialsProvided) {
            MockBackendUtils.logMockBackendUsed();
            try {
                container = new GenericContainer<>(AZURITE_IMAGE)
                        .withExposedPorts(BLOB_SERVICE_PORT, QUEUE_SERVICE_PORT)
                        .withLogConsumer(new Slf4jLogConsumer(LOGGER))
                        .waitingFor(Wait.forListeningPort());
                container.start();

                final String blobServiceUrl = "http://" + container.getContainerIpAddress() + ":"
                        + container.getMappedPort(BLOB_SERVICE_PORT) + "/" + azureStorageAccountName + "/"
                        + azureBlobContainername;
                final String queueServiceUrl = "http://" + container.getContainerIpAddress() + ":"
                        + container.getMappedPort(QUEUE_SERVICE_PORT) + "/" + azureStorageAccountName;

                result.put("azure.blob.container.name", azureBlobContainername);
                result.put("azure.blob.service.url", blobServiceUrl);
                result.put("azure.queue.service.url", queueServiceUrl);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            if (!startMockBackend && !realCredentialsProvided) {
                throw new IllegalStateException(
                        "Set AZURE_STORAGE_ACCOUNT_NAME and AZURE_STORAGE_ACCOUNT_KEY env vars if you set CAMEL_QUARKUS_START_MOCK_BACKEND=false");
            }
            MockBackendUtils.logRealBackendUsed();
            result.put("azure.blob.container.name", azureBlobContainername);
            result.put("azure.blob.service.url", "https://" + realAzureStorageAccountName + ".blob.core.windows.net");
            result.put("azure.queue.service.url", "https://" + realAzureStorageAccountName + ".queue.core.windows.net");
        }
        return result;
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
