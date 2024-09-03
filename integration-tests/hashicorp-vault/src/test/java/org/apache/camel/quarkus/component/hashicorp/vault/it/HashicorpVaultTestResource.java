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
package org.apache.camel.quarkus.component.hashicorp.vault.it;

import java.util.Map;
import java.util.UUID;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.eclipse.microprofile.config.ConfigProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

public class HashicorpVaultTestResource implements QuarkusTestResourceLifecycleManager {
    private static final Logger LOG = LoggerFactory.getLogger(HashicorpVaultTestResource.class);
    private static final String DOCKER_IMAGE_NAME = ConfigProvider.getConfig().getValue("hashicorp-vault.container.image",
            String.class);
    private static final String VAULT_TOKEN = UUID.randomUUID().toString();
    private static final int VAULT_PORT = 8300;
    private static final String VAULT_CONFIG = """
            {
              "listener": [
                {
                  "tcp": {
                    "address": "0.0.0.0:8300",
                    "tls_disable": "0",
                    "tls_cert_file": "/ssl/hashicorp-vault.crt",
                    "tls_key_file": "/ssl/hashicorp-vault.key"
                  }
                }
              ]
            }""";
    private GenericContainer<?> container;

    @Override
    public Map<String, String> start() {

        container = new GenericContainer<>(DockerImageName.parse(DOCKER_IMAGE_NAME));
        container.withEnv("VAULT_DEV_ROOT_TOKEN_ID", VAULT_TOKEN);
        container.withEnv("VAULT_LOCAL_CONFIG", VAULT_CONFIG.trim());
        container.addExposedPort(VAULT_PORT);
        container.withCopyFileToContainer(MountableFile.forHostPath("target/certs/hashicorp-vault.crt"),
                "/ssl/hashicorp-vault.crt");
        container.withCopyFileToContainer(MountableFile.forHostPath("target/certs/hashicorp-vault.key"),
                "/ssl/hashicorp-vault.key");
        container.waitingFor(Wait.forListeningPort());
        container.withLogConsumer(new Slf4jLogConsumer(LOG));
        container.waitingFor(Wait.forLogMessage(".*Development.*mode.*should.*", 1));

        container.start();

        return Map.of(
                "camel.component.hashicorp-vault.autowired-enabled", "false",
                "camel.vault.hashicorp.token", VAULT_TOKEN,
                "camel.vault.hashicorp.host", container.getHost(),
                "camel.vault.hashicorp.port", String.valueOf(container.getMappedPort(VAULT_PORT)),
                "camel.vault.hashicorp.scheme", "https");
    }

    @Override
    public void stop() {
        if (container != null) {
            container.stop();
        }
    }
}
