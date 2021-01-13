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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.UUID;

import com.microsoft.azure.storage.StorageCredentials;
import com.microsoft.azure.storage.StorageCredentialsAccountAndKey;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import io.quarkus.runtime.configuration.ConfigUtils;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.smallrye.config.SmallRyeConfig;
import org.apache.camel.quarkus.test.mock.backend.MockBackendUtils;
import org.apache.camel.util.CollectionHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;

public class AzureTestResource implements QuarkusTestResourceLifecycleManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(AzureTestResource.class);
    private static final String AZURITE_IMAGE = "mcr.microsoft.com/azure-storage/azurite:3.9.0";
    private static final int BLOB_SERVICE_PORT = 10000;
    private static final int QUEUE_SERVICE_PORT = 10001;

    private GenericContainer<?> container;
    private CloudBlobContainer blobContainer;

    @Override
    public Map<String, String> start() {

        final SmallRyeConfig config = ConfigUtils.configBuilder(true).build();

        final String realAzureStorageAccountName = System.getenv("AZURE_STORAGE_ACCOUNT_NAME");
        final boolean realCredentialsProvided = realAzureStorageAccountName != null
                && System.getenv("AZURE_STORAGE_ACCOUNT_KEY") != null;

        final String azureBlobContainername = "camel-quarkus-" + UUID.randomUUID().toString();

        final String azureStorageAccountName = config
                .getValue("camel.component.azure-blob.credentials-account-name", String.class);
        final String azureStorageAccountKey = config
                .getValue("camel.component.azure-blob.credentials-account-key", String.class);
        final Map<String, String> result;
        final boolean startMockBackend = MockBackendUtils.startMockBackend(false);
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

                result = CollectionHelper.mapOf(
                        "azure.blob.container.name", azureBlobContainername,
                        "azure.blob.service.url", blobServiceUrl,
                        "azure.queue.service.url", queueServiceUrl);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            if (!startMockBackend && !realCredentialsProvided) {
                throw new IllegalStateException(
                        "Set AZURE_STORAGE_ACCOUNT_NAME and AZURE_STORAGE_ACCOUNT_KEY env vars if you set CAMEL_QUARKUS_START_MOCK_BACKEND=false");
            }
            MockBackendUtils.logRealBackendUsed();
            result = CollectionHelper.mapOf(
                    "azure.blob.container.name", azureBlobContainername,
                    "azure.blob.service.url",
                    "https://" + realAzureStorageAccountName + ".blob.core.windows.net/" + azureBlobContainername,
                    "azure.queue.service.url", "https://" + realAzureStorageAccountName + ".queue.core.windows.net");
        }

        final StorageCredentials credentials = new StorageCredentialsAccountAndKey(azureStorageAccountName,
                azureStorageAccountKey);
        try {
            blobContainer = new CloudBlobContainer(new URI(result.get("azure.blob.service.url")), credentials);
            blobContainer.create();
        } catch (StorageException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    @Override
    public void stop() {
        if (blobContainer != null) {
            try {
                blobContainer.delete();
            } catch (StorageException e) {
                throw new RuntimeException(e);
            }
        }
        try {
            if (container != null) {
                container.stop();
            }
        } catch (Exception e) {
            // ignored
        }
    }
}
