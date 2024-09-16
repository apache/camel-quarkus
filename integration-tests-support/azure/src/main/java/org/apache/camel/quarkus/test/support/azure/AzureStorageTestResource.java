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

import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Ports;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.configuration.ConfigUtils;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.smallrye.config.SmallRyeConfig;
import org.apache.camel.quarkus.test.mock.backend.MockBackendUtils;
import org.eclipse.microprofile.config.ConfigProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.MountableFile;

public class AzureStorageTestResource implements QuarkusTestResourceLifecycleManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(AzureStorageTestResource.class);
    private static final String AZURITE_IMAGE = ConfigProvider.getConfig().getValue("azurite.container.image", String.class);
    private static final String EVENTHUBS_EMULATOR_IMAGE = ConfigProvider.getConfig()
            .getValue("eventhubs-emulator.container.image", String.class);
    private static final int EVENTHUBS_EMULATOR_PORT = 5672;
    private Map<String, String> initArgs = new LinkedHashMap<>();
    private GenericContainer<?> container;
    private GenericContainer<?> eventHubsEmulatorContainer;
    private Network network = Network.newNetwork();

    public enum AzuriteService {
        blob(10000),
        queue(10001),
        datalake(-1, "dfs"); // Datalake not supported by Azurite https://github.com/Azure/Azurite/issues/553

        private final int azuritePort;
        private final String azureServiceCode;

        AzuriteService(int port) {
            this(port, null);
        }

        AzuriteService(int port, String azureServiceCode) {
            this.azuritePort = port;
            this.azureServiceCode = azureServiceCode;
        }

        public static Integer[] getAzuritePorts() {
            return Stream.of(values())
                    .mapToInt(AzuriteService::getAzuritePort)
                    .filter(p -> p >= 0)
                    .boxed()
                    .toArray(Integer[]::new);
        }

        public int getAzuritePort() {
            return azuritePort;
        }

        public String getAzureServiceCode() {
            return azureServiceCode == null ? name() : azureServiceCode;
        }
    }

    @Override
    public void init(Map<String, String> initArgs) {
        this.initArgs = initArgs;
    }

    @Override
    public Map<String, String> start() {
        final SmallRyeConfig config = ConfigUtils.configBuilder(true, LaunchMode.NORMAL).build();

        final String realAzureStorageAccountName = System.getenv("AZURE_STORAGE_ACCOUNT_NAME");
        final boolean realCredentialsProvided = realAzureStorageAccountName != null
                && System.getenv("AZURE_STORAGE_ACCOUNT_KEY") != null;

        final String azureBlobContainername = "camel-quarkus-" + UUID.randomUUID();

        final String azureStorageAccountName = config
                .getValue("azure.storage.account-name", String.class);
        final boolean startMockBackend = MockBackendUtils.startMockBackend(false);
        final Map<String, String> result = new LinkedHashMap<>();
        if (startMockBackend && !realCredentialsProvided) {
            MockBackendUtils.logMockBackendUsed();
            try {
                container = new GenericContainer<>(AZURITE_IMAGE)
                        .withNetworkAliases("azurite")
                        .withNetwork(network)
                        .withExposedPorts(AzuriteService.getAzuritePorts())
                        .withLogConsumer(new Slf4jLogConsumer(LOGGER))
                        .waitingFor(Wait.forListeningPort());
                container.start();

                result.put("azure.blob.container.name", azureBlobContainername);
                Stream.of(AzuriteService.values())
                        .forEach(s -> {
                            result.put(
                                    "azure." + s.name() + ".service.url",
                                    "http://" + container.getHost() + ":"
                                            + (s.azuritePort >= 0 ? container.getMappedPort(s.azuritePort) : s.azuritePort)
                                            + "/"
                                            + azureStorageAccountName);
                        });

                String eventHubs = initArgs.get("eventHubs");
                if (eventHubs != null && eventHubs.equals("true")) {
                    result.put("azure.event.hubs.blob.container.name", azureBlobContainername);

                    eventHubsEmulatorContainer = new GenericContainer<>(
                            EVENTHUBS_EMULATOR_IMAGE)
                            .withNetwork(network)
                            .withCreateContainerCmdModifier(createContainerCmd -> {
                                Ports portBindings = new Ports();
                                portBindings.bind(ExposedPort.tcp(EVENTHUBS_EMULATOR_PORT),
                                        Ports.Binding.bindPort(EVENTHUBS_EMULATOR_PORT));
                                HostConfig hostConfig = HostConfig.newHostConfig()
                                        .withPortBindings(portBindings)
                                        .withNetworkMode(network.getId());
                                createContainerCmd.withHostName("eventhubs-emulator").withHostConfig(hostConfig);
                            })
                            .withExposedPorts(EVENTHUBS_EMULATOR_PORT)
                            .withEnv("BLOB_SERVER", "azurite")
                            .withEnv("METADATA_SERVER", "azurite")
                            .withEnv("ACCEPT_EULA", "Y")
                            .withCopyFileToContainer(MountableFile.forClasspathResource("config.json"),
                                    "/Eventhubs_Emulator/ConfigFiles/Config.json")
                            .withLogConsumer(new Slf4jLogConsumer(LOGGER))
                            .waitingFor(Wait.forLogMessage(".*Emulator Service is Successfully Up.*", 1));
                    eventHubsEmulatorContainer.start();

                    String connectionString = "Endpoint=sb://%s;SharedAccessKeyName=RootManageSharedAccessKey;SharedAccessKey=SAS_KEY_VALUE;UseDevelopmentEmulator=true;EntityPath=eh1"
                            .formatted(container.getHost());
                    result.put("azure.event.hubs.connection.string", connectionString);
                }
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
            Stream.of(AzuriteService.values())
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
            if (eventHubsEmulatorContainer != null) {
                eventHubsEmulatorContainer.stop();
            }

            if (container != null) {
                container.stop();
            }

            if (network != null) {
                network.close();
            }
        } catch (Exception e) {
            // ignored
        }
    }
}
