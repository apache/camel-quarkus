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
import java.util.stream.Stream;

import io.quarkus.runtime.LaunchMode;
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
    private static final String AZURITE_IMAGE = "mcr.microsoft.com/azure-storage/azurite:3.14.2";

    public enum Service {
        blob(10000),
        queue(10001),
        datalake(-1, "dfs") // Datalake not supported by Azurite https://github.com/Azure/Azurite/issues/553
        ;

        private final int azuritePort;
        private final String azureServiceCode;

        Service(int port) {
            this(port, null);
        }

        Service(int port, String azureServiceCode) {
            this.azuritePort = port;
            this.azureServiceCode = azureServiceCode;
        }

        public static Integer[] getAzuritePorts() {
            return Stream.of(values())
                    .mapToInt(Service::getAzuritePort)
                    .filter(p -> p >= 0)
                    .mapToObj(p -> Integer.valueOf(p))
                    .toArray(Integer[]::new);
        }

        public int getAzuritePort() {
            return azuritePort;
        }

        public String getAzureServiceCode() {
            return azureServiceCode == null ? name() : azureServiceCode;
        }
    }

    private GenericContainer<?> container;

    @Override
    public Map<String, String> start() {

        final SmallRyeConfig config = ConfigUtils.configBuilder(true, LaunchMode.NORMAL).build();

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
                        .withExposedPorts(Service.getAzuritePorts())
                        .withLogConsumer(new Slf4jLogConsumer(LOGGER))
                        .waitingFor(Wait.forListeningPort());
                container.start();

                result.put("azure.blob.container.name", azureBlobContainername);
                Stream.of(Service.values())
                        .forEach(s -> {
                            result.put(
                                    "azure." + s.name() + ".service.url",
                                    "http://" + container.getContainerIpAddress() + ":"
                                            + (s.azuritePort >= 0 ? container.getMappedPort(s.azuritePort) : s.azuritePort)
                                            + "/"
                                            + azureStorageAccountName);
                        });
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
            Stream.of(Service.values())
                    .forEach(s -> {
                        result.put(
                                "azure." + s.name() + ".service.url",
                                "https://" + realAzureStorageAccountName + "." + s.getAzureServiceCode() + ".core.windows.net");
                    });
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
