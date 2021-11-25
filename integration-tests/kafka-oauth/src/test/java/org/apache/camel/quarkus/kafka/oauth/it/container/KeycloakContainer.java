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

import com.github.dockerjava.api.command.InspectContainerResponse;
import org.testcontainers.containers.FixedHostPortGenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.MountableFile;

/**
 * Inspired from https://github.com/quarkusio/quarkus/tree/main/integration-tests/kafka-oauth-keycloak/
 */
public class KeycloakContainer extends FixedHostPortGenericContainer<KeycloakContainer> {

    public KeycloakContainer() {
        super("quay.io/keycloak/keycloak:15.0.2");
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
        withCreateContainerCmdModifier(cmd -> {
            cmd.withEntrypoint("");
            cmd.withCmd("/bin/bash", "-c", "cd /opt/jboss/keycloak " +
                    "&& bin/jboss-cli.sh --file=ssl/keycloak-ssl.cli " +
                    "&& rm -rf standalone/configuration/standalone_xml_history/current " +
                    "&& cd .. " +
                    "&& /opt/jboss/tools/docker-entrypoint.sh -Dkeycloak.profile.feature.upload_scripts=enabled -b 0.0.0.0");
        });
    }

    @Override
    protected void containerIsStarting(InspectContainerResponse containerInfo, boolean reused) {
        super.containerIsStarting(containerInfo);
        copyFileToContainer(MountableFile.forClasspathResource("certificates/ca-truststore.p12"),
                "/opt/jboss/keycloak/standalone/configuration/certs/ca-truststore.p12");
        copyFileToContainer(MountableFile.forClasspathResource("certificates/keycloak.server.keystore.p12"),
                "/opt/jboss/keycloak/standalone/configuration/certs/keycloak.server.keystore.p12");
        copyFileToContainer(MountableFile.forClasspathResource("keycloak/scripts/keycloak-ssl.cli"),
                "/opt/jboss/keycloak/ssl/keycloak-ssl.cli");
        copyFileToContainer(MountableFile.forClasspathResource("keycloak/realms/kafka-authz-realm.json"),
                "/opt/jboss/keycloak/realms/kafka-authz-realm.json");
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
