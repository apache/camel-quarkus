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

import java.util.List;
import java.util.Map;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsString;

@QuarkusTest
@QuarkusTestResource(KeycloakTestResource.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class KeycloakSecurityPolicyTest extends KeycloakSecurityPolicyTestBase {

    @Test
    @Order(1)
    public void testSetup() {
        createRealm();
        createClient();

        createRole(ADMIN_ROLE);
        createRole(USER_ROLE);

        createUser(ADMIN_USER);
        createUser(NORMAL_USER);
        createUser(ATTACKER_USER);

        resetPassword(ADMIN_USER, ADMIN_PASSWORD);
        resetPassword(NORMAL_USER, NORMAL_PASSWORD);
        resetPassword(ATTACKER_USER, ATTACKER_PASSWORD);

        assignRole(ADMIN_USER, ADMIN_ROLE);
        assignRole(NORMAL_USER, USER_ROLE);
        assignRole(ATTACKER_USER, USER_ROLE);
    }

    @Test
    @Order(2)
    public void testPropertyTokenWorks() {
        String propertyToken = getAccessToken(NORMAL_USER, NORMAL_PASSWORD);
        given()
                .when()
                .queryParam("propertyToken", propertyToken)
                .get("/keycloak/secure-policy/user-with-token-in-property")
                .then()
                .statusCode(200)
                .body(is("Access granted - secure default"));
    }

    @Test
    @Order(3)
    public void testHeaderTokenWorks() {
        String headerToken = getAccessToken(NORMAL_USER, NORMAL_PASSWORD);
        given()
                .when()
                .queryParam("headerToken", headerToken)
                .get("/keycloak/secure-policy/user-with-token-in-header")
                .then()
                .statusCode(200)
                .body(is("Access granted - secure default"));
    }

    @Test
    @Order(4)
    public void testPropertyPreferredOverHeaderTokenWorks() {
        String propertyToken = getAccessToken(NORMAL_USER, NORMAL_PASSWORD);
        String headerToken = getAccessToken(ATTACKER_USER, ATTACKER_PASSWORD);

        given()
                .when()
                .queryParam("propertyToken", propertyToken)
                .queryParam("headerToken", headerToken)
                .get("/keycloak/secure-policy/user-with-token-in-property-and-header")
                .then()
                .statusCode(200)
                .body(is("Access granted - secure default"));
    }

    @Test
    @Order(5)
    public void testInvalidHeaderIgnoredWhenPropertyValid() {
        String propertyToken = getAccessToken(NORMAL_USER, NORMAL_PASSWORD);
        String headerToken = "invalid.token";

        given()
                .when()
                .queryParam("propertyToken", propertyToken)
                .queryParam("headerToken", headerToken)
                .get("/keycloak/secure-policy/user-with-token-in-property-and-header")
                .then()
                .statusCode(200)
                .body(is("Access granted - secure default"));
    }

    @Test
    @Order(6)
    public void testHeaderRejectedWhenHeadersDisabled() {
        String headerToken = getAccessToken(NORMAL_USER, NORMAL_PASSWORD);

        given()
                .when()
                .queryParam("headerToken", headerToken)
                .get("/keycloak/secure-policy/max-security")
                .then()
                .statusCode(500)
                .body(containsString("Access token not found in exchange"));
    }

    @Test
    @Order(7)
    public void testPropertyWorksWhenHeadersDisabled() {
        String propertyToken = getAccessToken(NORMAL_USER, NORMAL_PASSWORD);

        given()
                .when()
                .queryParam("propertyToken", propertyToken)
                .get("/keycloak/secure-policy/max-security")
                .then()
                .statusCode(200)
                .body(is("Access granted - max security"));
    }

    @Test
    @Order(8)
    public void testPropertyTokenUsedNotHeader() {
        String propertyToken = getAccessToken(NORMAL_USER, NORMAL_PASSWORD);
        String headerToken = getAccessToken(ADMIN_USER, ADMIN_PASSWORD);

        given()
                .when()
                .queryParam("propertyToken", propertyToken)
                .queryParam("headerToken", headerToken)
                .get("/keycloak/secure-policy/user-with-token-in-property-and-header")
                .then()
                .statusCode(200)
                .body(is("Access granted - secure default"));
    }

    @Test
    @Order(9)
    public void testAttackScenario_SessionHijacking() {
        String propertyToken = getAccessToken(NORMAL_USER, NORMAL_PASSWORD);
        String attackerToken = getAccessToken(ATTACKER_USER, ATTACKER_PASSWORD);

        given()
                .when()
                .queryParam("propertyToken", propertyToken)
                .queryParam("headerToken", attackerToken)
                .get("/keycloak/secure-policy/user-with-token-in-property-and-header")
                .then()
                .statusCode(200)
                .body(is("Access granted - secure default"));
    }

    @Test
    @Order(10)
    public void testAttackScenario_LegacyUnsafe() {
        String propertyToken = getAccessToken(NORMAL_USER, NORMAL_PASSWORD);
        String attackerToken = getAccessToken(ATTACKER_USER, ATTACKER_PASSWORD);

        given()
                .when()
                .queryParam("propertyToken", propertyToken)
                .queryParam("headerToken", attackerToken)
                .get("/keycloak/secure-policy/legacy-unsafe")
                .then()
                .statusCode(500)
                .body(containsString("Token mismatch detected"));
    }

    @Test
    @Order(11)
    public void testAuthorizationHeaderFormat() {
        String headerToken = getAccessToken(NORMAL_USER, NORMAL_PASSWORD);

        given()
                .when()
                .queryParam("headerToken", headerToken)
                .get("/keycloak/secure-policy/authorization-header-format")
                .then()
                .statusCode(200)
                .body(is("Access granted - secure default"));
    }

    @Test
    @Order(12)
    public void testNoTokenRejected() {
        given()
                .when()
                .get("/keycloak/secure-policy/max-security")
                .then()
                .statusCode(500)
                .body(containsString("Access token not found in exchange"));
    }

    @Test
    @Order(13)
    public void testAdminOnly() {
        String propertyToken = getAccessToken(ADMIN_USER, ADMIN_PASSWORD);
        String headerToken = getAccessToken(NORMAL_USER, NORMAL_PASSWORD);

        given()
                .when()
                .queryParam("propertyToken", propertyToken)
                .queryParam("headerToken", headerToken)
                .get("/keycloak/secure-policy/admin-only")
                .then()
                .statusCode(200)
                .body(is("Admin access granted"));
    }

    @Test
    @Order(100)
    public void testCleanup_DeleteRealm() {
        KeycloakRealmLifecycle.deleteRealm(config("test.realm"));
    }

    protected String getAccessToken(String username, String password) {
        try (Client client = ClientBuilder.newClient()) {
            String tokenUrl = String.format("%s/realms/%s/protocol/openid-connect/token",
                    ConfigProvider.getConfig().getValue("keycloak.url", String.class), config("test.realm"));

            Form form = new Form()
                    .param("grant_type", "password")
                    .param("client_id", config("test.client.id"))
                    .param("client_secret", TEST_CLIENT_SECRET)
                    .param("username", username)
                    .param("password", password);

            try (Response response = client.target(tokenUrl)
                    .request(MediaType.APPLICATION_JSON)
                    .post(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED))) {

                if (response.getStatus() == 200) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> tokenResponse = response.readEntity(Map.class);
                    return (String) tokenResponse.get("access_token");
                } else {
                    String error = response.readEntity(String.class);
                    throw new RuntimeException("Failed to get token: " + error);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error obtaining access token for " + username, e);
        }
    }

    protected String config(String name) {
        return ConfigProvider.getConfig().getValue(name, String.class);
    }

    protected void createRealm() {
        KeycloakRealmLifecycle.createRealmWithSmtp(config("test.realm"));
    }

    protected void createClient() {
        ClientRepresentation client = new ClientRepresentation();
        client.setClientId(config("test.client.id"));
        client.setSecret(TEST_CLIENT_SECRET);
        client.setPublicClient(false);
        client.setDirectAccessGrantsEnabled(true);
        client.setStandardFlowEnabled(true);
        client.setFullScopeAllowed(true);

        given()
                .contentType(ContentType.JSON)
                .body(client)
                .post("/keycloak/client/{realmName}/pojo", config("test.realm"))
                .then()
                .statusCode(201);
    }

    protected void createRole(String roleName) {
        given()
                .queryParam("description", "Test role for integration testing")
                .when()
                .post("/keycloak/role/{realmName}/{roleName}", config("test.realm"), roleName)
                .then()
                .statusCode(200);
    }

    protected void createUser(String username) {
        UserRepresentation user = new UserRepresentation();
        user.setUsername(username);
        user.setEmail(username + "@test.com");
        user.setFirstName(username);
        user.setLastName("User");
        user.setEnabled(true);

        given()
                .contentType(ContentType.JSON)
                .body(List.of(user))
                .when()
                .post("/keycloak/user/{realmName}", config("test.realm"))
                .then()
                .statusCode(200);
    }

    private void resetPassword(String username, String password) {
        given()
                .queryParam("password", password)
                .queryParam("temporary", false)
                .when()
                .post("/keycloak/user/{realmName}/{username}/reset-password", config("test.realm"), username)
                .then()
                .statusCode(200);
    }

    protected void assignRole(String username, String role) {
        given()
                .when()
                .post("/keycloak/user-role/{realmName}/{username}/{roleName}",
                        config("test.realm"), username, role)
                .then()
                .statusCode(200);
    }
}
