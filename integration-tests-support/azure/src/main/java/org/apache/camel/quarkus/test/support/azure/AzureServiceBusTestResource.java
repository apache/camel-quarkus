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

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;

import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.configuration.ConfigUtils;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.smallrye.config.SmallRyeConfig;
import org.apache.camel.quarkus.test.mock.backend.MockBackendUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;

public class AzureServiceBusTestResource implements QuarkusTestResourceLifecycleManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(AzureServiceBusTestResource.class);
    private static final int SERVICEBUS_INNER_PORT = 5672;
    private Map<String, String> initArgs = new LinkedHashMap<>();
    private ComposeContainer container;

    @Override
    public void init(Map<String, String> initArgs) {
        this.initArgs = initArgs;
    }

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
                //copy docker-compose to tmp location
                File dockerComposeFile, configFile;
                try (InputStream inYaml = getClass().getClassLoader().getResourceAsStream("servicebus-docker-compose.yaml");
                        InputStream inJson = getClass().getClassLoader().getResourceAsStream("servicebus-config.json")) {
                    dockerComposeFile = File.createTempFile("servicebus-docker-compose-", ".yaml");
                    configFile = File.createTempFile("servicebus-config-", ".json");
                    Files.copy(inYaml, dockerComposeFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    Files.copy(inJson, configFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }

                container = new ComposeContainer(dockerComposeFile)
                        .withEnv("ACCEPT_EULA", "Y")
                        .withEnv("SERVICEBUS_EMULATOR_IMAGE",
                                config.getValue("servicebus-emulator.container.image", String.class))
                        .withEnv("SQL_EDGE_IMAGE", config.getValue("azure-sql-edge.container.image", String.class))
                        .withEnv("CONFIG_FILE", configFile.getAbsolutePath())
                        .withEnv("MSSQL_SA_PASSWORD", "12345678923456y!43")
                        .withExposedService("emulator", SERVICEBUS_INNER_PORT)
                        .withLocalCompose(true)
                        .withLogConsumer("emulator", new Slf4jLogConsumer(LOGGER))
                        .waitingFor("emulator", Wait.forLogMessage(".*Emulator Service is Successfully Up!.*", 1));

                container.start();

                String connectionString = "Endpoint=sb://%s:%d;SharedAccessKeyName=RootManageSharedAccessKey;SharedAccessKey=SAS_KEY_VALUE;UseDevelopmentEmulator=true;"
                        .formatted(container.getServiceHost("emulator", SERVICEBUS_INNER_PORT),
                                container.getServicePort("emulator", SERVICEBUS_INNER_PORT));
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

            if (container != null) {
                container.stop();
            }

        } catch (Exception e) {
            // ignored
        }
    }

}
