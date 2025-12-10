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

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
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
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.wait.strategy.Wait;

/**
 * Test resource is using opensource conjur. See the instructions from
 * https://github.com/cyberark/conjur-quickstart?tab=readme-ov-file#setting-up-an-environment
 *
 * Important note. the docker-compose.yml, from the conjur, has to be stripped from container_name attributes.
 */
public class CyberarkVaultTestResource implements QuarkusTestResourceLifecycleManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(CyberarkVaultTestResource.class);
    private ComposeContainer container;

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
            //copy docker-compose to tmp location
            File dockerComposeFile, configFile;
            //create tmp folder in target
            Path targetDir = Paths.get("target");
            Path tempDir = Files.createTempDirectory(targetDir, "docker-compose-");
            try (InputStream inYaml = getClass().getClassLoader().getResourceAsStream("docker-compose.yml");) {
                dockerComposeFile = File.createTempFile("docker-compose-", ".yml", tempDir.toFile());
                Files.copy(inYaml, dockerComposeFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            FileUtils.copyDirectory(new File(getClass().getResource("/conf").getFile()), tempDir.resolve("conf").toFile());

            container = new ComposeContainer(dockerComposeFile)
                    .withLocalCompose(true)
                    .withExposedService("conjur", 80)
                    .waitingFor("conjur", Wait.forLogMessage(".* Listening on http.*", 1));

            container.start();

            Container.ExecResult er = container.getContainerByServiceName("conjur").get()
                    .execInContainer("conjurctl", "account", "create", "myConjurAccount");
            Assertions.assertEquals(0, er.getExitCode(), "Creation of account failed with: " + er.getStderr());
            //admin key is the last word from stdout
            String adminKey = new LinkedList<>(Arrays.asList(er.getStdout().split("\\s"))).getLast();

            er = container.getContainerByServiceName("client").get()
                    .execInContainer("conjur", "init", "oss", "-u", "https://proxy",
                            "-a", "myConjurAccount", "--self-signed");
            Assertions.assertEquals(0, er.getExitCode(), "Client init failed with: " + er.getStderr());

            er = container.getContainerByServiceName("client").get()
                    .execInContainer("conjur", "login", "-i", "admin", "-p", adminKey);
            Assertions.assertEquals(0, er.getExitCode(), "Client login failed with: " + er.getStderr());

            er = container.getContainerByServiceName("client").get()
                    .execInContainer("conjur", "policy", "load", "-b", "root", "-f", "policy/BotApp.yml");
            Assertions.assertEquals(0, er.getExitCode(), "Policy load failed with: " + er.getStderr());

            ObjectMapper objectMapper = new ObjectMapper();
            try {
                // Read JSON from a file
                JsonNode jsonNode = objectMapper.readTree(er.getStdout());
                jsonNode.get("created_roles").get("myConjurAccount:host:BotApp/myDemoApp").get("id");

                result.put("conjur.read.username", "host/BotApp/myDemoApp");
                result.put("conjur.read.apiKey",
                        jsonNode.get("created_roles").get("myConjurAccount:host:BotApp/myDemoApp").get("api_key").textValue());
                result.put("conjur.write.username", "user/Dave@BotApp");
                result.put("conjur.write.apiKey",
                        jsonNode.get("created_roles").get("myConjurAccount:user:Dave@BotApp").get("api_key").textValue());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            container.getContainerByServiceName("client").get()
                    .execInContainer("conjur", "logout");

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        result.put("conjur.account", "myConjurAccount");
        result.put("conjur.url", "http://localhost:" + container.getServicePort("conjur", 80));

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
