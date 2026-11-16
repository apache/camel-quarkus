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
package org.apache.camel.quarkus.component.mcp.server.it;

import java.time.Duration;
import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.quarkus.test.keycloak.server.KeycloakContainer;
import org.eclipse.microprofile.config.ConfigProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.utility.MountableFile;

/** Starts a real Keycloak container for MCP/OIDC integration tests. */
public class McpServerKeycloakTestResource implements QuarkusTestResourceLifecycleManager {

    private static final Logger LOG = LoggerFactory.getLogger(McpServerKeycloakTestResource.class);
    private static final String REALM_NAME = "mcp";

    private KeycloakContainer keycloak;

    @Override
    public Map<String, String> start() {
        System.setProperty("keycloak.docker.image",
                ConfigProvider.getConfig().getValue("keycloak.container.image", String.class));

        keycloak = new KeycloakContainer()
                .withStartupTimeout(Duration.ofMinutes(5))
                .withLogConsumer(new Slf4jLogConsumer(LOG))
                .withCopyFileToContainer(
                        MountableFile.forClasspathResource("/mcp-realm.json"),
                        "/opt/keycloak/data/import/mcp-realm.json")
                .withCommand("start", "--import-realm", "--verbose");
        keycloak.start();

        String issuer = keycloak.getServerUrl() + "/realms/" + REALM_NAME;
        return Map.ofEntries(
                Map.entry("quarkus.oidc.enabled", "true"),
                Map.entry("quarkus.oidc.auth-server-url", issuer),
                Map.entry("quarkus.oidc.client-id", "mcp-test-client"),
                Map.entry("quarkus.oidc.credentials.secret", "mcp-test-secret"),
                Map.entry("quarkus.oidc.application-type", "service"),
                Map.entry("quarkus.oidc.token.principal-claim", "preferred_username"),
                Map.entry("quarkus.oidc.roles.role-claim-path", "realm_access/roles"),
                Map.entry("quarkus.oidc.tenant-paths", "/mcp,/mcp/*"),
                Map.entry("quarkus.oidc.resource-metadata.enabled", "true"),
                Map.entry("quarkus.oidc.resource-metadata.force-https-scheme", "false"),
                Map.entry("quarkus.http.host", "localhost"),
                Map.entry("quarkus.http.auth.permission.mcp.paths", "/mcp,/mcp/*"),
                Map.entry("quarkus.http.auth.permission.mcp.policy", "authenticated"));
    }

    @Override
    public void stop() {
        if (keycloak != null) {
            keycloak.stop();
        }
    }
}
