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
package org.apache.camel.quarkus.kafka.oauth.it.container;

import java.io.FileWriter;

import org.testcontainers.containers.FixedHostPortGenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.MountableFile;

/**
 * Inspired from https://github.com/quarkusio/quarkus/tree/main/integration-tests/kafka-oauth-keycloak/
 */
public class KeycloakContainer extends FixedHostPortGenericContainer<KeycloakContainer> {

    public KeycloakContainer() {
        super("quay.io/keycloak/keycloak:16.1.1");
        withExposedPorts(8443);
        withFixedExposedPort(8080, 8080);
        withEnv("KEYCLOAK_USER", "admin");
        withEnv("KEYCLOAK_PASSWORD", "admin");
        withEnv("KEYCLOAK_HTTPS_PORT", "8443");
        withEnv("PROXY_ADDRESS_FORWARDING", "true");
        withEnv("KEYCLOAK_IMPORT", "/opt/jboss/keycloak/realms/kafka-authz-realm.json");
        waitingFor(Wait.forLogMessage(".*WFLYSRV0025.*", 1));
        withNetwork(Network.SHARED);
        withNetworkAliases("keycloak");
        withCopyFileToContainer(MountableFile.forClasspathResource("keycloak/realms/kafka-authz-realm.json"),
                "/opt/jboss/keycloak/realms/kafka-authz-realm.json");
        withCommand("-Dkeycloak.profile.feature.upload_scripts=enabled", "-b", "0.0.0.0");
    }

    public void createHostsFile() {
        try (FileWriter fileWriter = new FileWriter("target/hosts")) {
            String dockerHost = this.getHost();
            if ("localhost".equals(dockerHost)) {
                fileWriter.write("127.0.0.1 keycloak");
            } else {
                fileWriter.write(dockerHost + " keycloak");
            }
            fileWriter.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
