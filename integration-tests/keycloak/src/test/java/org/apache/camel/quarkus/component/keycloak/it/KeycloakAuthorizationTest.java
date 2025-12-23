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
import java.util.Set;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.keycloak.representations.idm.authorization.ResourcePermissionRepresentation;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

/**
 * Tests for Keycloak Authorization Services including:
 * - Resources
 * - Policies
 * - Permissions
 */
@QuarkusTest
@QuarkusTestResource(KeycloakTestResource.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class KeycloakAuthorizationTest extends KeycloakTestBase {

    @Test
    @Order(1)
    public void testSetup_CreateRealm() {
        KeycloakRealmLifecycle.createRealmWithSmtp(TEST_REALM_NAME);

        // Create a client with authorization enabled
        ClientRepresentation client = new ClientRepresentation();
        client.setClientId(TEST_AUTHZ_CLIENT_ID);
        client.setEnabled(true);
        client.setPublicClient(false);
        client.setServiceAccountsEnabled(true);
        client.setAuthorizationServicesEnabled(true);

        given()
                .contentType(ContentType.JSON)
                .body(client)
                .when()
                .post("/keycloak/client/{realmName}/pojo", TEST_REALM_NAME)
                .then()
                .statusCode(201);
    }

    // ==================== Resource Operations ====================

    @Test
    @Order(2)
    public void testCreateResource() {
        ResourceRepresentation resource = new ResourceRepresentation();
        resource.setName("test-resource");
        resource.setDisplayName("Test Resource");
        resource.setType("urn:test:resources:default");
        resource.setUris(Set.of("/test-resource/*"));

        Response response = given()
                .contentType(ContentType.JSON)
                .body(resource)
                .when()
                .post("/keycloak/resource/{realmName}/{clientId}/pojo", TEST_REALM_NAME, TEST_AUTHZ_CLIENT_ID)
                .then()
                .statusCode(201)
                .extract()
                .response();

        String location = response.header("Location");
        if (location != null) {
            TEST_RESOURCE_ID = location.substring(location.lastIndexOf('/') + 1);
        }
    }

    @Test
    @Order(3)
    public void testListResources() {
        List<ResourceRepresentation> resources = given()
                .when()
                .get("/keycloak/resource/{realmName}/{clientId}", TEST_REALM_NAME, TEST_AUTHZ_CLIENT_ID)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract()
                .body()
                .jsonPath()
                .getList(".", ResourceRepresentation.class);

        assertThat(resources, notNullValue());
        assertThat(resources.size(), greaterThanOrEqualTo(1));

        if (TEST_RESOURCE_ID == null) {
            TEST_RESOURCE_ID = resources.stream()
                    .filter(r -> "test-resource".equals(r.getName()))
                    .map(ResourceRepresentation::getId)
                    .findFirst()
                    .orElse(null);
        }
    }

    @Test
    @Order(4)
    public void testGetResource() {
        assertThat(TEST_RESOURCE_ID, notNullValue());

        ResourceRepresentation resource = given()
                .when()
                .get("/keycloak/resource/{realmName}/{clientId}/{resourceId}",
                        TEST_REALM_NAME, TEST_AUTHZ_CLIENT_ID, TEST_RESOURCE_ID)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract()
                .as(ResourceRepresentation.class);

        assertThat(resource, notNullValue());
        assertThat(resource.getName(), is("test-resource"));
        assertThat(resource.getType(), is("urn:test:resources:default"));
    }

    @Test
    @Order(5)
    public void testUpdateResource() {
        assertThat(TEST_RESOURCE_ID, notNullValue());

        ResourceRepresentation resource = given()
                .when()
                .get("/keycloak/resource/{realmName}/{clientId}/{resourceId}",
                        TEST_REALM_NAME, TEST_AUTHZ_CLIENT_ID, TEST_RESOURCE_ID)
                .then()
                .statusCode(200)
                .extract()
                .as(ResourceRepresentation.class);

        resource.setDisplayName("Updated Test Resource");

        given()
                .contentType(ContentType.JSON)
                .body(resource)
                .when()
                .put("/keycloak/resource/{realmName}/{clientId}/{resourceId}",
                        TEST_REALM_NAME, TEST_AUTHZ_CLIENT_ID, TEST_RESOURCE_ID)
                .then()
                .statusCode(200)
                .body(is("Resource updated successfully"));

        ResourceRepresentation updatedResource = given()
                .when()
                .get("/keycloak/resource/{realmName}/{clientId}/{resourceId}",
                        TEST_REALM_NAME, TEST_AUTHZ_CLIENT_ID, TEST_RESOURCE_ID)
                .then()
                .statusCode(200)
                .extract()
                .as(ResourceRepresentation.class);

        assertThat(updatedResource.getDisplayName(), is("Updated Test Resource"));
    }

    // ==================== Policy Operations ====================

    @Test
    @Order(11)
    public void testCreateResourcePolicy() {
        PolicyRepresentation policy = new PolicyRepresentation();
        policy.setName("test-policy");
        policy.setDescription("Test Policy");
        policy.setType("resource");
        policy.setDecisionStrategy(org.keycloak.representations.idm.authorization.DecisionStrategy.UNANIMOUS);

        Response response = given()
                .contentType(ContentType.JSON)
                .body(policy)
                .when()
                .post("/keycloak/resource-policy/{realmName}/{clientId}/pojo",
                        TEST_REALM_NAME, TEST_AUTHZ_CLIENT_ID)
                .then()
                .statusCode(201)
                .extract()
                .response();

        String location = response.header("Location");
        if (location != null) {
            TEST_POLICY_ID = location.substring(location.lastIndexOf('/') + 1);
        }
    }

    @Test
    @Order(12)
    public void testListResourcePolicies() {
        List<PolicyRepresentation> policies = given()
                .when()
                .get("/keycloak/resource-policy/{realmName}/{clientId}", TEST_REALM_NAME, TEST_AUTHZ_CLIENT_ID)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract()
                .body()
                .jsonPath()
                .getList(".", PolicyRepresentation.class);

        assertThat(policies, notNullValue());
        assertThat(policies.size(), greaterThanOrEqualTo(1));

        if (TEST_POLICY_ID == null) {
            TEST_POLICY_ID = policies.stream()
                    .filter(p -> "test-policy".equals(p.getName()))
                    .map(PolicyRepresentation::getId)
                    .findFirst()
                    .orElse(null);
        }
    }

    @Test
    @Order(13)
    public void testGetResourcePolicy() {
        assertThat(TEST_POLICY_ID, notNullValue());

        PolicyRepresentation policy = given()
                .when()
                .get("/keycloak/resource-policy/{realmName}/{clientId}/{policyId}",
                        TEST_REALM_NAME, TEST_AUTHZ_CLIENT_ID, TEST_POLICY_ID)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract()
                .as(PolicyRepresentation.class);

        assertThat(policy, notNullValue());
        assertThat(policy.getName(), is("test-policy"));
    }

    @Test
    @Order(14)
    public void testUpdateResourcePolicy() {
        assertThat(TEST_POLICY_ID, notNullValue());

        PolicyRepresentation policy = given()
                .when()
                .get("/keycloak/resource-policy/{realmName}/{clientId}/{policyId}",
                        TEST_REALM_NAME, TEST_AUTHZ_CLIENT_ID, TEST_POLICY_ID)
                .then()
                .statusCode(200)
                .extract()
                .as(PolicyRepresentation.class);

        policy.setDescription("Updated Test Policy");

        given()
                .contentType(ContentType.JSON)
                .body(policy)
                .when()
                .put("/keycloak/resource-policy/{realmName}/{clientId}/{policyId}",
                        TEST_REALM_NAME, TEST_AUTHZ_CLIENT_ID, TEST_POLICY_ID)
                .then()
                .statusCode(200)
                .body(is("Policy updated successfully"));

        PolicyRepresentation updatedPolicy = given()
                .when()
                .get("/keycloak/resource-policy/{realmName}/{clientId}/{policyId}",
                        TEST_REALM_NAME, TEST_AUTHZ_CLIENT_ID, TEST_POLICY_ID)
                .then()
                .statusCode(200)
                .extract()
                .as(PolicyRepresentation.class);

        assertThat(updatedPolicy.getDescription(), is("Updated Test Policy"));
    }

    // ==================== Permission Operations ====================

    @Test
    @Order(21)
    public void testCreateResourcePermission() {
        assertThat(TEST_RESOURCE_ID, notNullValue());
        assertThat(TEST_POLICY_ID, notNullValue());

        ResourcePermissionRepresentation permission = new ResourcePermissionRepresentation();
        permission.setName("test-permission");
        permission.setDescription("Test Permission");
        permission.setResources(java.util.Set.of(TEST_RESOURCE_ID));
        permission.setPolicies(java.util.Set.of(TEST_POLICY_ID));
        permission.setDecisionStrategy(org.keycloak.representations.idm.authorization.DecisionStrategy.UNANIMOUS);

        Response response = given()
                .contentType(ContentType.JSON)
                .body(permission)
                .when()
                .post("/keycloak/resource-permission/{realmName}/{clientId}/pojo",
                        TEST_REALM_NAME, TEST_AUTHZ_CLIENT_ID)
                .then()
                .statusCode(201)
                .extract()
                .response();

        String location = response.header("Location");
        if (location != null) {
            TEST_PERMISSION_ID = location.substring(location.lastIndexOf('/') + 1);
        }
    }

    @Test
    @Order(22)
    public void testListResourcePermissions() {
        List<ResourcePermissionRepresentation> permissions = given()
                .when()
                .get("/keycloak/resource-permission/{realmName}/{clientId}",
                        TEST_REALM_NAME, TEST_AUTHZ_CLIENT_ID)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract()
                .body()
                .jsonPath()
                .getList(".", ResourcePermissionRepresentation.class);

        assertThat(permissions, notNullValue());
        assertThat(permissions.size(), greaterThanOrEqualTo(1));

        if (TEST_PERMISSION_ID == null) {
            TEST_PERMISSION_ID = permissions.stream()
                    .filter(p -> "test-permission".equals(p.getName()))
                    .map(ResourcePermissionRepresentation::getId)
                    .findFirst()
                    .orElse(null);
        }
    }

    @Test
    @Order(23)
    public void testGetResourcePermission() {
        assertThat(TEST_PERMISSION_ID, notNullValue());

        PolicyRepresentation permission = given()
                .when()
                .get("/keycloak/resource-permission/{realmName}/{clientId}/{permissionId}",
                        TEST_REALM_NAME, TEST_AUTHZ_CLIENT_ID, TEST_PERMISSION_ID)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract()
                .as(PolicyRepresentation.class);

        assertThat(permission, notNullValue());
        assertThat(permission.getName(), is("test-permission"));
    }

    @Test
    @Order(24)
    public void testUpdateResourcePermission() {
        assertThat(TEST_PERMISSION_ID, notNullValue());

        PolicyRepresentation permission = given()
                .when()
                .get("/keycloak/resource-permission/{realmName}/{clientId}/{permissionId}",
                        TEST_REALM_NAME, TEST_AUTHZ_CLIENT_ID, TEST_PERMISSION_ID)
                .then()
                .statusCode(200)
                .extract()
                .as(PolicyRepresentation.class);

        permission.setDescription("Updated Test Permission");

        given()
                .contentType(ContentType.JSON)
                .body(permission)
                .when()
                .put("/keycloak/resource-permission/{realmName}/{clientId}/{permissionId}",
                        TEST_REALM_NAME, TEST_AUTHZ_CLIENT_ID, TEST_PERMISSION_ID)
                .then()
                .statusCode(200)
                .body(is("Permission updated successfully"));

        PolicyRepresentation updatedPermission = given()
                .when()
                .get("/keycloak/resource-permission/{realmName}/{clientId}/{permissionId}",
                        TEST_REALM_NAME, TEST_AUTHZ_CLIENT_ID, TEST_PERMISSION_ID)
                .then()
                .statusCode(200)
                .extract()
                .as(PolicyRepresentation.class);

        assertThat(updatedPermission.getDescription(), is("Updated Test Permission"));
    }

    // ==================== Cleanup ====================

    @Test
    @Order(101)
    public void testDeleteResourcePermission() {
        assertThat(TEST_PERMISSION_ID, notNullValue());

        given()
                .when()
                .delete("/keycloak/resource-permission/{realmName}/{clientId}/{permissionId}",
                        TEST_REALM_NAME, TEST_AUTHZ_CLIENT_ID, TEST_PERMISSION_ID)
                .then()
                .statusCode(200)
                .body(is("Permission deleted successfully"));
    }

    @Test
    @Order(102)
    public void testDeleteResourcePolicy() {
        assertThat(TEST_POLICY_ID, notNullValue());

        given()
                .when()
                .delete("/keycloak/resource-policy/{realmName}/{clientId}/{policyId}",
                        TEST_REALM_NAME, TEST_AUTHZ_CLIENT_ID, TEST_POLICY_ID)
                .then()
                .statusCode(200)
                .body(is("Policy deleted successfully"));
    }

    @Test
    @Order(103)
    public void testDeleteResource() {
        assertThat(TEST_RESOURCE_ID, notNullValue());

        given()
                .when()
                .delete("/keycloak/resource/{realmName}/{clientId}/{resourceId}",
                        TEST_REALM_NAME, TEST_AUTHZ_CLIENT_ID, TEST_RESOURCE_ID)
                .then()
                .statusCode(200)
                .body(is("Resource deleted successfully"));
    }

    @Test
    @Order(104)
    public void testCleanupAuthorizationClient() {
        given()
                .when()
                .delete("/keycloak/client/{realmName}/{clientId}", TEST_REALM_NAME, TEST_AUTHZ_CLIENT_ID)
                .then()
                .statusCode(200)
                .body(is("Client deleted successfully"));
    }

    @Test
    @Order(105)
    public void testCleanup_DeleteRealm() {
        KeycloakRealmLifecycle.deleteRealm(TEST_REALM_NAME);
    }
}
