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

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

/**
 * Tests for Keycloak client operations including:
 * - Client CRUD
 * - Client roles
 * - Client scopes
 * - Client secrets
 */
@QuarkusTest
@QuarkusTestResource(KeycloakTestResource.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class KeycloakClientTest extends KeycloakTestBase {

    @Test
    @Order(1)
    public void testSetup_CreateRealm() {
        KeycloakRealmLifecycle.createRealmWithSmtp(TEST_REALM_NAME);

        // Create a test user for client role assignments
        given()
                .queryParam("email", TEST_USER_NAME + "@test.com")
                .queryParam("firstName", "Test")
                .queryParam("lastName", "User")
                .when()
                .post("/keycloak/user/{realmName}/{username}", TEST_REALM_NAME, TEST_USER_NAME)
                .then()
                .statusCode(201);
    }

    // ==================== Client Operations ====================

    @Test
    @Order(2)
    public void testCreateClientWithHeaders() {
        given()
                .when()
                .post("/keycloak/client/{realmName}/{clientId}", TEST_REALM_NAME, TEST_CLIENT_ID)
                .then()
                .statusCode(201)
                .body(is("Client created successfully"));
    }

    @Test
    @Order(2)
    public void testCreateClientWithPojo() {
        String pojoClientId = TEST_CLIENT_ID + "-pojo";

        ClientRepresentation client = new ClientRepresentation();
        client.setClientId(pojoClientId);
        client.setEnabled(true);
        client.setPublicClient(true);

        given()
                .contentType(ContentType.JSON)
                .body(client)
                .when()
                .post("/keycloak/client/{realmName}/pojo", TEST_REALM_NAME)
                .then()
                .statusCode(201)
                .body(is("Client created successfully"));
    }

    @Test
    @Order(3)
    public void testListClients() {
        List<ClientRepresentation> clients = given()
                .when()
                .get("/keycloak/client/{realmName}", TEST_REALM_NAME)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract()
                .body()
                .jsonPath()
                .getList(".", ClientRepresentation.class);

        assertThat(clients, notNullValue());
        assertThat(clients.size(), greaterThanOrEqualTo(2));
    }

    @Test
    @Order(4)
    public void testGetClient() {
        ClientRepresentation client = given()
                .when()
                .get("/keycloak/client/{realmName}/{clientId}", TEST_REALM_NAME, TEST_CLIENT_ID)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract()
                .as(ClientRepresentation.class);

        assertThat(client, notNullValue());
        assertThat(client.getClientId(), is(TEST_CLIENT_ID));
    }

    @Test
    @Order(5)
    public void testUpdateClient() {
        ClientRepresentation client = given()
                .when()
                .get("/keycloak/client/{realmName}/{clientId}", TEST_REALM_NAME, TEST_CLIENT_ID)
                .then()
                .statusCode(200)
                .extract()
                .as(ClientRepresentation.class);

        client.setDescription("Updated client description");

        given()
                .contentType(ContentType.JSON)
                .body(client)
                .when()
                .put("/keycloak/client/{realmName}/{clientId}", TEST_REALM_NAME, TEST_CLIENT_ID)
                .then()
                .statusCode(200)
                .body(is("Client updated successfully"));

        ClientRepresentation updatedClient = given()
                .when()
                .get("/keycloak/client/{realmName}/{clientId}", TEST_REALM_NAME, TEST_CLIENT_ID)
                .then()
                .statusCode(200)
                .extract()
                .as(ClientRepresentation.class);

        assertThat(updatedClient.getDescription(), is("Updated client description"));
    }

    // ==================== Client Role Operations ====================

    @Test
    @Order(10)
    public void testCreateClientRoleWithHeaders() {
        given()
                .queryParam("description", "Test client role for integration testing")
                .when()
                .post("/keycloak/client-role/{realmName}/{clientId}/{roleName}",
                        TEST_REALM_NAME, TEST_CLIENT_ID, TEST_CLIENT_ROLE_NAME)
                .then()
                .statusCode(200)
                .body(is("Client role created successfully"));
    }

    @Test
    @Order(11)
    public void testCreateClientRoleWithPojo() {
        String pojoClientRoleName = TEST_CLIENT_ROLE_NAME + "-pojo";

        RoleRepresentation role = new RoleRepresentation();
        role.setName(pojoClientRoleName);
        role.setDescription("Test client role created via POJO");

        given()
                .contentType(ContentType.JSON)
                .body(role)
                .when()
                .post("/keycloak/client-role/{realmName}/{clientId}/pojo", TEST_REALM_NAME, TEST_CLIENT_ID)
                .then()
                .statusCode(200)
                .body(is("Client role created successfully"));
    }

    @Test
    @Order(12)
    public void testListClientRoles() {
        List<RoleRepresentation> clientRoles = given()
                .when()
                .get("/keycloak/client-role/{realmName}/{clientId}", TEST_REALM_NAME, TEST_CLIENT_ID)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract()
                .body()
                .jsonPath()
                .getList(".", RoleRepresentation.class);

        assertThat(clientRoles, notNullValue());
        assertThat(clientRoles.size(), greaterThanOrEqualTo(2));
    }

    @Test
    @Order(13)
    public void testGetClientRole() {
        RoleRepresentation clientRole = given()
                .when()
                .get("/keycloak/client-role/{realmName}/{clientId}/{roleName}",
                        TEST_REALM_NAME, TEST_CLIENT_ID, TEST_CLIENT_ROLE_NAME)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract()
                .as(RoleRepresentation.class);

        assertThat(clientRole, notNullValue());
        assertThat(clientRole.getName(), is(TEST_CLIENT_ROLE_NAME));
        assertThat(clientRole.getDescription(), is("Test client role for integration testing"));
    }

    @Test
    @Order(14)
    public void testUpdateClientRole() {
        RoleRepresentation clientRole = given()
                .when()
                .get("/keycloak/client-role/{realmName}/{clientId}/{roleName}",
                        TEST_REALM_NAME, TEST_CLIENT_ID, TEST_CLIENT_ROLE_NAME)
                .then()
                .statusCode(200)
                .extract()
                .as(RoleRepresentation.class);

        clientRole.setDescription("Updated client role description");

        given()
                .contentType(ContentType.JSON)
                .body(clientRole)
                .when()
                .put("/keycloak/client-role/{realmName}/{clientId}/{roleName}",
                        TEST_REALM_NAME, TEST_CLIENT_ID, TEST_CLIENT_ROLE_NAME)
                .then()
                .statusCode(200)
                .body(is("Client role updated successfully"));

        RoleRepresentation updatedClientRole = given()
                .when()
                .get("/keycloak/client-role/{realmName}/{clientId}/{roleName}",
                        TEST_REALM_NAME, TEST_CLIENT_ID, TEST_CLIENT_ROLE_NAME)
                .then()
                .statusCode(200)
                .extract()
                .as(RoleRepresentation.class);

        assertThat(updatedClientRole.getDescription(), is("Updated client role description"));
    }

    @Test
    @Order(15)
    public void testAssignClientRoleToUser() {
        given()
                .when()
                .post("/keycloak/client-role-user/{realmName}/{clientId}/{username}/{roleName}",
                        TEST_REALM_NAME, TEST_CLIENT_ID, TEST_USER_NAME, TEST_CLIENT_ROLE_NAME)
                .then()
                .statusCode(200)
                .body(is("Client role assigned to user successfully"));
    }

    @Test
    @Order(16)
    public void testRemoveClientRoleFromUser() {
        given()
                .when()
                .delete("/keycloak/client-role-user/{realmName}/{clientId}/{username}/{roleName}",
                        TEST_REALM_NAME, TEST_CLIENT_ID, TEST_USER_NAME, TEST_CLIENT_ROLE_NAME)
                .then()
                .statusCode(200)
                .body(is("Client role removed from user successfully"));
    }

    // ==================== Client Scope Operations ====================

    @Test
    @Order(20)
    public void testCreateClientScopeWithPojo() {
        ClientScopeRepresentation scope = new ClientScopeRepresentation();
        scope.setName(TEST_CLIENT_SCOPE_NAME);
        scope.setProtocol("openid-connect");
        scope.setDescription("Test client scope for integration testing");

        given()
                .contentType(ContentType.JSON)
                .body(scope)
                .when()
                .post("/keycloak/client-scope/{realmName}/pojo", TEST_REALM_NAME)
                .then()
                .statusCode(201)
                .body(is("Client scope created successfully"));
    }

    @Test
    @Order(21)
    public void testListClientScopes() {
        List<ClientScopeRepresentation> scopes = given()
                .when()
                .get("/keycloak/client-scope/{realmName}", TEST_REALM_NAME)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract()
                .body()
                .jsonPath()
                .getList(".", ClientScopeRepresentation.class);

        assertThat(scopes, notNullValue());
        assertThat(scopes.size(), greaterThanOrEqualTo(1));
    }

    @Test
    @Order(22)
    public void testGetClientScope() {
        ClientScopeRepresentation scope = given()
                .when()
                .get("/keycloak/client-scope/{realmName}/{scopeName}", TEST_REALM_NAME, TEST_CLIENT_SCOPE_NAME)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract()
                .as(ClientScopeRepresentation.class);

        assertThat(scope, notNullValue());
        assertThat(scope.getName(), is(TEST_CLIENT_SCOPE_NAME));
    }

    @Test
    @Order(23)
    public void testUpdateClientScope() {
        ClientScopeRepresentation scope = given()
                .when()
                .get("/keycloak/client-scope/{realmName}/{scopeName}", TEST_REALM_NAME, TEST_CLIENT_SCOPE_NAME)
                .then()
                .statusCode(200)
                .extract()
                .as(ClientScopeRepresentation.class);

        scope.setDescription("Updated client scope description");

        given()
                .contentType(ContentType.JSON)
                .body(scope)
                .when()
                .put("/keycloak/client-scope/{realmName}/{scopeName}", TEST_REALM_NAME, TEST_CLIENT_SCOPE_NAME)
                .then()
                .statusCode(200)
                .body(is("Client scope updated successfully"));

        ClientScopeRepresentation updatedScope = given()
                .when()
                .get("/keycloak/client-scope/{realmName}/{scopeName}", TEST_REALM_NAME, TEST_CLIENT_SCOPE_NAME)
                .then()
                .statusCode(200)
                .extract()
                .as(ClientScopeRepresentation.class);

        assertThat(updatedScope.getDescription(), is("Updated client scope description"));
    }

    // ==================== Client Secret Operations ====================

    @Test
    @Order(30)
    public void testGetClientSecret() {
        // First, make the client confidential to have a secret
        ClientRepresentation client = given()
                .when()
                .get("/keycloak/client/{realmName}/{clientId}", TEST_REALM_NAME, TEST_CLIENT_ID)
                .then()
                .statusCode(200)
                .extract()
                .as(ClientRepresentation.class);

        client.setPublicClient(false);
        client.setServiceAccountsEnabled(true);

        given()
                .contentType(ContentType.JSON)
                .body(client)
                .when()
                .put("/keycloak/client/{realmName}/{clientId}", TEST_REALM_NAME, TEST_CLIENT_ID)
                .then()
                .statusCode(200);

        CredentialRepresentation secretData = given()
                .when()
                .get("/keycloak/client-secret/{realmName}/{clientId}", TEST_REALM_NAME, TEST_CLIENT_ID)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract()
                .as(CredentialRepresentation.class);

        assertThat(secretData, notNullValue());
        assertThat(secretData.getValue(), notNullValue());
    }

    @Test
    @Order(31)
    public void testRegenerateClientSecret() {
        CredentialRepresentation oldSecretData = given()
                .when()
                .get("/keycloak/client-secret/{realmName}/{clientId}", TEST_REALM_NAME, TEST_CLIENT_ID)
                .then()
                .statusCode(200)
                .extract()
                .as(CredentialRepresentation.class);

        String oldSecret = oldSecretData.getValue();

        CredentialRepresentation newSecretData = given()
                .when()
                .post("/keycloak/client-secret/{realmName}/{clientId}/regenerate", TEST_REALM_NAME, TEST_CLIENT_ID)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract()
                .as(CredentialRepresentation.class);

        assertThat(newSecretData, notNullValue());
        assertThat(newSecretData.getValue(), notNullValue());
        assertThat(newSecretData.getValue().equals(oldSecret), is(false));
    }

    // ==================== Cleanup ====================

    @Test
    @Order(100)
    public void testCleanupClientRoles() {
        String[] rolesToDelete = { TEST_CLIENT_ROLE_NAME, TEST_CLIENT_ROLE_NAME + "-pojo" };

        for (String roleName : rolesToDelete) {
            given()
                    .when()
                    .delete("/keycloak/client-role/{realmName}/{clientId}/{roleName}",
                            TEST_REALM_NAME, TEST_CLIENT_ID, roleName)
                    .then()
                    .statusCode(200)
                    .body(is("Client role deleted successfully"));
        }
    }

    @Test
    @Order(101)
    public void testCleanupClientScopes() {
        given()
                .when()
                .delete("/keycloak/client-scope/{realmName}/{scopeName}", TEST_REALM_NAME, TEST_CLIENT_SCOPE_NAME)
                .then()
                .statusCode(200)
                .body(is("Client scope deleted successfully"));
    }

    @Test
    @Order(102)
    public void testCleanupClients() {
        String[] clientsToDelete = { TEST_CLIENT_ID, TEST_CLIENT_ID + "-pojo" };

        for (String clientId : clientsToDelete) {
            given()
                    .when()
                    .delete("/keycloak/client/{realmName}/{clientId}", TEST_REALM_NAME, clientId)
                    .then()
                    .statusCode(200)
                    .body(is("Client deleted successfully"));
        }
    }

    @Test
    @Order(103)
    public void testCleanup_DeleteRealm() {
        KeycloakRealmLifecycle.deleteRealm(TEST_REALM_NAME);
    }
}
