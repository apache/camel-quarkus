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

package org.apache.camel.quarkus.component.cyberark.vault.it;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.apache.camel.quarkus.test.mock.backend.MockBackendUtils;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

/**
 * Test resource is using opensource conjur. See the instructions from
 * https://github.com/cyberark/conjur-quickstart?tab=readme-ov-file#setting-up-an-environment
 */
public class CyberarkVaultTestResource implements QuarkusTestResourceLifecycleManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(CyberarkVaultTestResource.class);
    private static final String POSTGRES_PASSWORD = "SuperSecretPg";
    private static final String POSTGRES_USERNAME = "postgres";
    private static final String CONJUR_DATA_KEY = "changeitchangeitchangeitchangeitchangeitIhc=";
    private static final String CONJUR_ACCOUNT = "myConjurAccount";
    private static final String POSTGRES_CONTAINER_IMAGE = ConfigProvider.getConfig().getValue("postgres.container.image",
            String.class);
    private static final String CONJUR_CONTAINER_IMAGE = ConfigProvider.getConfig().getValue("cyberark-conjur.container.image",
            String.class);
    private static final String CONJUR_CLI_CONTAINER_IMAGE = ConfigProvider.getConfig()
            .getValue("cyberark-conjur-cli.container.image", String.class);
    private static final String NGINX_CONTAINER_IMAGE = ConfigProvider.getConfig().getValue("cyberark-nginx.container.image",
            String.class);
    private static final int CONJUR_PORT = 80;
    private static final int NGINX_PORT = 443;

    private Network network;
    private PostgreSQLContainer postgresContainer;
    private GenericContainer<?> conjurContainer;
    private GenericContainer<?> nginxContainer;
    private GenericContainer<?> clientContainer;

    @Override
    public Map<String, String> start() {
        final Map<String, String> result = new LinkedHashMap<>();

        //if env properties are defined, use the real account
        List<String> missingExternalProperties = Stream
                .of("CQ_CONJUR_URL", "CQ_CONJUR_ACCOUNT", "CQ_CONJUR_READ_USER", "CQ_CONJUR_READ_USER_API_KEY",
                        "CQ_CONJUR_READ_WRITE_USER", "CQ_CONJUR_READ_WRITE_USER_API_KEY")
                .filter(prop -> {
                    String value = System.getenv(prop);
                    return value == null || value.isEmpty();
                })
                .toList();
        if (missingExternalProperties.isEmpty()) {
            MockBackendUtils.logRealBackendUsed();

            result.put("quarkus.http.port", "0");
            result.put("quarkus.http.test-port", "0");
            return result;
        }

        if (missingExternalProperties.size() < 6) {
            throw new RuntimeException(
                    "Several environmental properties are missing (you have to provide either all of them or none." +
                            "Missing properties are: " + String.join(",", missingExternalProperties));
        }
        MockBackendUtils.logMockBackendUsed();

        try {
            // Create a shared network for all containers
            network = Network.newNetwork();

            // Start PostgreSQL container
            startPostgresContainer();

            // Start Conjur container
            startConjurContainer();

            // Start Nginx proxy container
            startNginxContainer();

            // Start Conjur CLI client container
            startClientContainer();

            // Initialize Conjur and load policies
            initializeConjur(result);
        } catch (Exception e) {
            throw new RuntimeException("Failed to start Conjur test environment", e);
        }

        result.put("conjur.account", CONJUR_ACCOUNT);
        result.put("conjur.url", "http://localhost:" + conjurContainer.getMappedPort(80));

        return result;
    }

    private void startPostgresContainer() {
        LOGGER.info("Starting PostgreSQL container...");

        DockerImageName postgresImageName = DockerImageName.parse(POSTGRES_CONTAINER_IMAGE)
                .asCompatibleSubstituteFor("postgres");
        postgresContainer = new PostgreSQLContainer(postgresImageName)
                .withNetwork(network)
                .withNetworkAliases("database")
                .withDatabaseName("postgres")
                .withUsername(POSTGRES_USERNAME)
                .withPassword(POSTGRES_PASSWORD)
                .withLogConsumer(new Slf4jLogConsumer(LOGGER).withPrefix("POSTGRES"));

        postgresContainer.start();

        LOGGER.info("PostgreSQL container started");
    }

    private void startConjurContainer() {
        LOGGER.info("Starting Conjur container...");

        String databaseUrl = String.format("postgres://postgres:%s@database/postgres", POSTGRES_PASSWORD);

        conjurContainer = new GenericContainer<>(DockerImageName.parse(CONJUR_CONTAINER_IMAGE))
                .withNetwork(network)
                .withNetworkAliases("conjur")
                .withCommand("server")
                .withEnv("DATABASE_URL", databaseUrl)
                .withEnv("CONJUR_DATA_KEY", CONJUR_DATA_KEY)
                .withEnv("CONJUR_AUTHENTICATORS", "")
                .withEnv("CONJUR_TELEMETRY_ENABLED", "false")
                .withEnv("CONJUR_API_RESOURCE_LIST_LIMIT_MAX", "5000")
                .withExposedPorts(CONJUR_PORT)
                .withLogConsumer(new Slf4jLogConsumer(LOGGER).withPrefix("CONJUR"))
                .waitingFor(Wait.forLogMessage(".*Listening on http.*", 1)
                        .withStartupTimeout(Duration.ofMinutes(2)))
                .dependsOn(postgresContainer);

        conjurContainer.start();

        LOGGER.info("Conjur container started on port {}", conjurContainer.getMappedPort(80));
    }

    private void startNginxContainer() throws Exception {
        LOGGER.info("Starting Nginx proxy container...");

        // Get paths to generated certificates and config files
        Path certsDir = Paths.get("target/certs");
        Path nginxCert = certsDir.resolve("nginx.crt");
        Path nginxKey = certsDir.resolve("nginx.key");

        if (!Files.exists(nginxCert) || !Files.exists(nginxKey)) {
            throw new RuntimeException("SSL certificates not found in target/certs.");
        }

        nginxContainer = new GenericContainer<>(DockerImageName.parse(NGINX_CONTAINER_IMAGE))
                .withNetwork(network)
                .withNetworkAliases("proxy")
                .withCopyFileToContainer(MountableFile.forClasspathResource("conf/default.conf"),
                        "/etc/nginx/conf.d/default.conf")
                .withCopyFileToContainer(MountableFile.forHostPath(nginxCert), "/etc/nginx/tls/nginx.crt")
                .withCopyFileToContainer(MountableFile.forHostPath(nginxKey), "/etc/nginx/tls/nginx.key")
                .withExposedPorts(NGINX_PORT)
                .withLogConsumer(new Slf4jLogConsumer(LOGGER).withPrefix("NGINX"))
                .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(1)))
                .dependsOn(conjurContainer);

        nginxContainer.start();

        LOGGER.info("Nginx proxy container started on port {}", nginxContainer.getMappedPort(443));
    }

    private void startClientContainer() throws Exception {
        LOGGER.info("Starting Conjur CLI client container...");

        clientContainer = new GenericContainer<>(DockerImageName.parse(CONJUR_CLI_CONTAINER_IMAGE))
                .withNetwork(network)
                .withNetworkAliases("client")
                .withCreateContainerCmdModifier(cmd -> {
                    cmd.withEntrypoint("sleep");
                })
                .withCommand("infinity")
                .withCopyFileToContainer(
                        org.testcontainers.utility.MountableFile.forClasspathResource("conf/policy/BotApp.yml"),
                        "/policy/BotApp.yml")
                .withLogConsumer(new Slf4jLogConsumer(LOGGER).withPrefix("CLIENT"))
                .withStartupTimeout(Duration.ofSeconds(5))
                .dependsOn(nginxContainer);

        clientContainer.start();

        LOGGER.info("Conjur CLI client container started");
    }

    private void initializeConjur(Map<String, String> result) throws Exception {
        LOGGER.info("Initializing Conjur account...");

        // Verify containers are running
        if (!conjurContainer.isRunning()) {
            throw new RuntimeException("Conjur container is not running");
        }

        if (!clientContainer.isRunning()) {
            throw new RuntimeException("Client container is not running");
        }

        LOGGER.info("All containers verified as running");

        // Create Conjur account
        Container.ExecResult er = conjurContainer.execInContainer(
                "conjurctl", "account", "create", CONJUR_ACCOUNT);
        Assertions.assertEquals(0, er.getExitCode(), "Creation of account failed with: " + er.getStderr());

        // Extract admin API key (last word from stdout)
        String adminKey = new LinkedList<>(Arrays.asList(er.getStdout().split("\\s"))).getLast();
        LOGGER.info("Conjur account created with admin key");

        // Initialize Conjur CLI client
        er = clientContainer.execInContainer(
                "conjur", "init", "oss", "-u", "https://proxy", "-a", CONJUR_ACCOUNT, "--self-signed");
        Assertions.assertEquals(0, er.getExitCode(), "Client init failed with: " + er.getStderr());
        LOGGER.info("Conjur CLI client initialized");

        // Login as admin
        er = clientContainer.execInContainer(
                "conjur", "login", "-i", "admin", "-p", adminKey);
        Assertions.assertEquals(0, er.getExitCode(), "Client login failed with: " + er.getStderr());
        LOGGER.info("Logged in as admin");

        // Load policy
        er = clientContainer.execInContainer(
                "conjur", "policy", "load", "-b", "root", "-f", "/policy/BotApp.yml");
        Assertions.assertEquals(0, er.getExitCode(), "Policy load failed with: " + er.getStderr());
        LOGGER.info("Policy loaded successfully");

        // Parse policy output to extract API keys
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(er.getStdout());

        result.put("conjur.read.username", "host/BotApp/myDemoApp");
        result.put("conjur.read.apiKey",
                jsonNode.get("created_roles").get(CONJUR_ACCOUNT + ":host:BotApp/myDemoApp").get("api_key").textValue());
        result.put("conjur.write.username", "user/Dave@BotApp");
        result.put("conjur.write.apiKey",
                jsonNode.get("created_roles").get(CONJUR_ACCOUNT + ":user:Dave@BotApp").get("api_key").textValue());

        // Logout
        clientContainer.execInContainer("conjur", "logout");

        LOGGER.info("Conjur initialization complete");
    }

    @Override
    public void stop() {
        try {
            if (clientContainer != null) {
                clientContainer.stop();
            }
            if (nginxContainer != null) {
                nginxContainer.stop();
            }
            if (conjurContainer != null) {
                conjurContainer.stop();
            }
            if (postgresContainer != null) {
                postgresContainer.stop();
            }
            if (network != null) {
                network.close();
            }
        } catch (Exception e) {
            LOGGER.warn("Error during cleanup", e);
        }
    }
}
