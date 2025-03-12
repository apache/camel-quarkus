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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

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

public class AzureServiceBusTestResource implements QuarkusTestResourceLifecycleManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(AzureServiceBusTestResource.class);
    private static final String SQL_IMAGE = ConfigProvider.getConfig().getValue("sql-server.container.image",
            String.class);
    private static final String EMULATOR_IMAGE = ConfigProvider.getConfig().getValue("servicebus-emulator.container.image",
            String.class);

    private static final int SERVICEBUS_INNER_PORT = 5672;
    private static final String MSSQL_PASSWORD = "12345678923456y!43";
    private GenericContainer<?> emulatorContainer, sqlContainer;

    @Override
    public Map<String, String> start() {
        final SmallRyeConfig config = ConfigUtils.configBuilder(true, LaunchMode.NORMAL).build();

        final boolean realCredentialsProvided = System.getenv("AZURE_SERVICEBUS_CONNECTION_STRING") != null
                && System.getenv("AZURE_SERVICEBUS_QUEUE_NAME") != null;

        final boolean startMockBackend = MockBackendUtils.startMockBackend(false);
        final Map<String, String> result = new LinkedHashMap<>();
        if (startMockBackend && !realCredentialsProvided) {
            MockBackendUtils.logMockBackendUsed();

            try {
                Network azureNetwork = Network.newNetwork();

                sqlContainer = new GenericContainer<>(SQL_IMAGE)
                        .withEnv(Collections.singletonMap("MSSQL_AGENT_ENABLED", "True"))
                        .withNetwork(azureNetwork)
                        .withLogConsumer(new Slf4jLogConsumer(LOGGER))
                        .withEnv("ACCEPT_EULA", "Y")
                        .withEnv("MSSQL_SA_PASSWORD", MSSQL_PASSWORD)
                        .withNetworkAliases("sql-edge")
                        .waitingFor(
                                Wait.forLogMessage(".*xp_sqlagent_notify.*", 1));
                sqlContainer.start();

                emulatorContainer = new GenericContainer<>(EMULATOR_IMAGE)
                        .withNetwork(azureNetwork)
                        .withExposedPorts(SERVICEBUS_INNER_PORT)
                        .withLogConsumer(new Slf4jLogConsumer(LOGGER))
                        .withEnv("ACCEPT_EULA", "Y")
                        .withEnv("MSSQL_SA_PASSWORD", MSSQL_PASSWORD)
                        .withEnv("SQL_SERVER", "sql-edge")
                        .withCopyFileToContainer(MountableFile.forClasspathResource("servicebus-config.json"),
                                "/ServiceBus_Emulator/ConfigFiles/Config.json")
                        .waitingFor(Wait.forLogMessage(".*Emulator Service is Successfully Up!.*", 1));
                emulatorContainer.start();

                String connectionString = "Endpoint=sb://%s:%d;SharedAccessKeyName=RootManageSharedAccessKey;SharedAccessKey=SAS_KEY_VALUE;UseDevelopmentEmulator=true;"
                        .formatted("localhost", emulatorContainer.getMappedPort(SERVICEBUS_INNER_PORT));
                result.put("azure.servicebus.connection.string", connectionString);
                result.put("azure.servicebus.queue.name", "queue.1");
                result.put("azure.servicebus.topic.name", "topic.1");
                result.put("azure.servicebus.topic.subscription.name", "subscription.1");

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            if (!startMockBackend && !realCredentialsProvided) {
                throw new IllegalStateException(
                        "Set AZURE_SERVICEBUS_CONNECTION_STRING and AZURE_SERVICEBUS_QUEUE_NAME env vars if you set CAMEL_QUARKUS_START_MOCK_BACKEND=false");
            }
        }

        return result;
    }

    @Override
    public void stop() {
        try {

            if (emulatorContainer != null) {
                emulatorContainer.stop();
            }
            if (sqlContainer != null) {
                sqlContainer.stop();
            }

        } catch (Exception e) {
            // ignored
        }
    }

}
