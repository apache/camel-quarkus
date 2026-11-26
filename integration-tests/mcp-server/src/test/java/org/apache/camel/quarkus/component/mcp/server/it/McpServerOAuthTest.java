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

import java.net.URI;
import java.util.Map;
import java.util.stream.Collectors;

import io.quarkiverse.mcp.server.TextContent;
import io.quarkiverse.mcp.server.ToolResponse;
import io.quarkiverse.mcp.server.test.McpAssured;
import io.quarkiverse.mcp.server.test.McpAssured.McpStreamableTestClient;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Exercises a real Keycloak/OIDC identity through MCP and into a Camel tool route. */
@QuarkusTest
@QuarkusTestResource(value = McpServerKeycloakTestResource.class, restrictToAnnotatedClass = true)
class McpServerOAuthTest {

    private static final String INITIALIZE_REQUEST = "{\"jsonrpc\":\"2.0\",\"id\":1,"
            + "\"method\":\"initialize\",\"params\":{\"protocolVersion\":\"2025-06-18\","
            + "\"capabilities\":{},\"clientInfo\":{\"name\":\"camel-quarkus-keycloak-test\",\"version\":\"1\"}}}";

    @Test
    void anonymousMcpRequestIsRejected() {
        postInitialize(null, 401, false);
    }

    @Test
    void invalidBearerTokenIsRejected() {
        postInitialize("not-a-valid-keycloak-token", 401, false);
    }

    @Test
    void protectedResourceMetadataIsPublished() {
        String issuer = keycloakIssuer();
        String metadata = RestAssured.get("/.well-known/oauth-protected-resource")
                .then()
                .statusCode(200)
                .extract()
                .asString();
        assertThat(metadata).contains("authorization_servers").contains(issuer);
    }

    // This exercises quarkus-mcp-server's loopback DNS-rebinding check, not a general Origin allow-list.
    @Test
    void untrustedOriginIsRejectedEvenWithValidBearerToken() {
        postInitialize(accessToken(), 403, true);
    }

    @Test
    void validKeycloakBearerTokenPropagatesIdentityIntoCamelTool() {
        String token = accessToken();
        int port = ConfigProvider.getConfig().getValue("quarkus.http.test-port", Integer.class);
        McpStreamableTestClient client = McpAssured.newStreamableClient()
                .setBaseUri(URI.create("http://localhost:%d".formatted(port)))
                .setBearerToken(token)
                .build()
                .connect();

        try {
            client.when()
                    .toolsCall("whoami", Map.of(), response -> {
                        String content = textOf(response);
                        assertThat(content).startsWith("alice|alice|").contains("mcp-operator");
                    })
                    .toolsCall("whooauth", Map.of(), response -> {
                        String[] context = textOf(response).split("\\|", -1);
                        assertThat(context).hasSize(5);
                        assertThat(context[0]).isNotBlank();
                        assertThat(context[1]).contains("profile").doesNotContain("\"");
                        assertThat(context[2]).isEqualTo("alice");
                        assertThat(context[3]).isEqualTo("false"); // raw_token must not enter exchange properties
                        assertThat(context[4]).isEqualTo("true"); // JSON-P realm_access is exposed as a Java Map
                    })
                    .thenAssertResults();
        } finally {
            client.disconnect();
        }
    }

    private static String keycloakIssuer() {
        String issuer = System.getProperty(McpServerKeycloakTestResource.ISSUER_PROPERTY);
        if (issuer == null || issuer.isBlank()) {
            throw new IllegalStateException("Keycloak issuer system property was not set by the test resource");
        }
        return issuer;
    }

    private static String accessToken() {
        String issuer = keycloakIssuer();
        return RestAssured.given()
                .contentType("application/x-www-form-urlencoded")
                .formParam("grant_type", "password")
                .formParam("client_id", "mcp-test-client")
                .formParam("client_secret", "mcp-test-secret")
                .formParam("username", "alice")
                .formParam("password", "alice-password")
                .post(issuer + "/protocol/openid-connect/token")
                .then()
                .statusCode(200)
                .extract()
                .path("access_token");
    }

    private static void postInitialize(String token, int expectedStatus, boolean untrustedOrigin) {
        var request = RestAssured.given()
                .header("Accept", "application/json, text/event-stream")
                .contentType("application/json")
                .body(INITIALIZE_REQUEST);
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        if (untrustedOrigin) {
            // Keep Host bound to the real localhost destination; only Origin is hostile.
            request.header("Origin", "http://evil.example.com");
        }
        var response = request.post("/mcp");
        assertThat(response.statusCode()).isEqualTo(expectedStatus);
        if (untrustedOrigin) {
            // With CORS disabled for this module, this message proves the MCP server's
            // loopback DNS-rebinding check (rather than Quarkus CORS) rejected the request.
            assertThat(response.getStatusLine()).endsWith(" 403 Invalid origin");
        }
    }

    private static String textOf(ToolResponse response) {
        return response.content().stream()
                .filter(TextContent.class::isInstance)
                .map(content -> ((TextContent) content).text())
                .collect(Collectors.joining());
    }
}
