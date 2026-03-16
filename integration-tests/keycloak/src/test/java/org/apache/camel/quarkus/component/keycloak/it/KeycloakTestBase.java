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
package org.apache.camel.quarkus.component.keycloak.it;

import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.common.QuarkusTestResource;
import io.restassured.RestAssured;
import io.restassured.config.ObjectMapperConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.http.ContentType;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.BeforeAll;
import org.keycloak.representations.idm.ClientRepresentation;

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.is;

/**
 * Base class for Keycloak integration tests.
 * Provides common test data and RestAssured configuration.
 */
@QuarkusTestResource(KeycloakTestResource.class)
public abstract class KeycloakTestBase {

    // Test data - use unique names to avoid conflicts
    protected static final String TEST_REALM_NAME = "test-realm-" + UUID.randomUUID().toString().substring(0, 8);
    protected static final String TEST_USER_NAME = "test-user-" + UUID.randomUUID().toString().substring(0, 8);
    protected static final String TEST_USER_PASSWORD = "Test@password123";
    protected static final String TEST_ROLE_NAME = "test-role-" + UUID.randomUUID().toString().substring(0, 8);
    protected static final String TEST_GROUP_NAME = "test-group-" + UUID.randomUUID().toString().substring(0, 8);
    protected static final String TEST_CLIENT_ID = "test-client-" + UUID.randomUUID().toString().substring(0, 8);
    protected static final String TEST_CLIENT_SECRET = "test-client-secret";
    protected static final String TEST_CLIENT_ROLE_NAME = "test-client-role-"
            + UUID.randomUUID().toString().substring(0, 8);
    protected static final String TEST_CLIENT_SCOPE_NAME = "test-scope-" + UUID.randomUUID().toString().substring(0, 8);
    protected static final String TEST_IDP_ALIAS = "test-idp-" + UUID.randomUUID().toString().substring(0, 8);
    protected static final String TEST_AUTHZ_CLIENT_ID = "test-authz-client-"
            + UUID.randomUUID().toString().substring(0, 8);
    protected static String TEST_RESOURCE_ID; // Set after creation
    protected static String TEST_POLICY_ID; // Set after creation
    protected static String TEST_PERMISSION_ID; // Set after creation

    @BeforeAll
    public static void configureRestAssured() {
        // Configure REST-assured to ignore unknown properties when deserializing
        // This is needed because the Keycloak server may return newer fields
        // that the client representation classes don't know about
        RestAssured.config = RestAssuredConfig.config().objectMapperConfig(
                ObjectMapperConfig.objectMapperConfig().jackson2ObjectMapperFactory(
                        (cls, charset) -> {
                            ObjectMapper mapper = new ObjectMapper();
                            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                            return mapper;
                        }));
    }

    protected String config(String name) {
        return ConfigProvider.getConfig().getValue(name, String.class);
    }

    /**
     * Helper method to create a Keycloak client via REST API.
     * Reduces duplication when creating multiple clients in tests.
     */
    protected void createClient(ClientRepresentation client) {
        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(client)
                .post("/keycloak/client/{realmName}/pojo", config("test.realm"))
                .then()
                .statusCode(anyOf(is(200), is(201)));
    }

    /**
     * Obtain access token using Resource Owner Password Credentials grant (username/password).
     * Returns only the access_token string.
     */
    protected String getAccessToken(String username, String password,
            String clientId, String clientSecret) {
        Map<String, Object> tokenResponse = getTokenResponse(username, password, clientId, clientSecret);
        return (String) tokenResponse.get("access_token");
    }

    /**
     * Obtain full token response using Resource Owner Password Credentials grant.
     * Returns map containing access_token, refresh_token, expires_in, etc.
     */
    protected Map<String, Object> getTokenResponse(String username, String password,
            String clientId, String clientSecret) {
        try (Client client = ClientBuilder.newClient()) {
            String tokenUrl = String.format("%s/realms/%s/protocol/openid-connect/token",
                    config("keycloak.url"), config("test.realm"));

            Form form = new Form()
                    .param("grant_type", "password")
                    .param("client_id", clientId)
                    .param("client_secret", clientSecret)
                    .param("username", username)
                    .param("password", password);

            try (Response response = client.target(tokenUrl)
                    .request(MediaType.APPLICATION_JSON)
                    .post(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED))) {

                if (response.getStatus() == 200) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> body = response.readEntity(Map.class);
                    return body;
                }
                throw new RuntimeException("Failed to get token for " + username
                        + " [" + response.getStatus() + "]: " + response.readEntity(String.class));
            }
        }
    }

    /**
     * Obtain service account access token (service-to-service authentication).
     * Uses the OAuth 2.0 Client Credentials grant type.
     * This grant type does not require user credentials.
     */
    protected String getServiceAccountToken(String clientId, String clientSecret) {
        Map<String, Object> tokenResponse = getServiceAccountTokenResponse(clientId, clientSecret);
        return (String) tokenResponse.get("access_token");
    }

    /**
     * Obtain full service account token response.
     * Uses the OAuth 2.0 Client Credentials grant type.
     * Returns map containing access_token, expires_in, etc.
     */
    protected Map<String, Object> getServiceAccountTokenResponse(String clientId, String clientSecret) {
        try (Client client = ClientBuilder.newClient()) {
            String tokenUrl = String.format("%s/realms/%s/protocol/openid-connect/token",
                    config("keycloak.url"), config("test.realm"));

            Form form = new Form()
                    .param("grant_type", "client_credentials")
                    .param("client_id", clientId)
                    .param("client_secret", clientSecret);

            try (Response response = client.target(tokenUrl)
                    .request(MediaType.APPLICATION_JSON)
                    .post(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED))) {

                if (response.getStatus() == 200) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> body = response.readEntity(Map.class);
                    return body;
                }
                throw new RuntimeException("Failed to get token via client credentials for " + clientId
                        + " [" + response.getStatus() + "]: " + response.readEntity(String.class));
            }
        }
    }

    /**
     * Get a refreshed access token using a refresh token.
     * Uses the OAuth 2.0 Refresh Token grant type.
     * This allows obtaining a new access token without re-authenticating.
     */
    protected String getRefreshedAccessToken(String refreshToken, String clientId, String clientSecret) {
        Map<String, Object> tokenResponse = getRefreshedTokenResponse(refreshToken, clientId, clientSecret);
        return (String) tokenResponse.get("access_token");
    }

    /**
     * Get full refreshed token response using a refresh token.
     * Uses the OAuth 2.0 Refresh Token grant type.
     * Returns map containing new access_token, refresh_token, expires_in, etc.
     */
    protected Map<String, Object> getRefreshedTokenResponse(String refreshToken, String clientId, String clientSecret) {
        try (Client client = ClientBuilder.newClient()) {
            String tokenUrl = String.format("%s/realms/%s/protocol/openid-connect/token",
                    config("keycloak.url"), config("test.realm"));

            Form form = new Form()
                    .param("grant_type", "refresh_token")
                    .param("client_id", clientId)
                    .param("client_secret", clientSecret)
                    .param("refresh_token", refreshToken);

            try (Response response = client.target(tokenUrl)
                    .request(MediaType.APPLICATION_JSON)
                    .post(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED))) {

                if (response.getStatus() == 200) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> body = response.readEntity(Map.class);
                    return body;
                }
                throw new RuntimeException("Failed to refresh token"
                        + " [" + response.getStatus() + "]: " + response.readEntity(String.class));
            }
        }
    }
}
