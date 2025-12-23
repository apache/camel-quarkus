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
import org.keycloak.representations.idm.RoleRepresentation;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

@QuarkusTest
@QuarkusTestResource(KeycloakTestResource.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class KeycloakRoleTest extends KeycloakTestBase {

    @Test
    @Order(1)
    public void testSetup_CreateRealm() {
        KeycloakRealmLifecycle.createRealmWithSmtp(TEST_REALM_NAME);

        // Create a test user for user-role operations
        given()
                .queryParam("email", TEST_USER_NAME + "@test.com")
                .queryParam("firstName", "Test")
                .queryParam("lastName", "User")
                .when()
                .post("/keycloak/user/{realmName}/{username}", TEST_REALM_NAME, TEST_USER_NAME)
                .then()
                .statusCode(201);
    }

    @Test
    @Order(2)
    public void testCreateRoleWithHeaders() {
        given()
                .queryParam("description", "Test role for integration testing")
                .when()
                .post("/keycloak/role/{realmName}/{roleName}", TEST_REALM_NAME, TEST_ROLE_NAME)
                .then()
                .statusCode(200)
                .body(is("Role created successfully"));
    }

    @Test
    @Order(2)
    public void testCreateRoleWithPojo() {
        String pojoRoleName = TEST_ROLE_NAME + "-pojo";

        RoleRepresentation role = new RoleRepresentation();
        role.setName(pojoRoleName);
        role.setDescription("Test role created via POJO");

        given()
                .contentType(ContentType.JSON)
                .body(role)
                .when()
                .post("/keycloak/role/{realmName}/pojo", TEST_REALM_NAME)
                .then()
                .statusCode(200)
                .body(is("Role created successfully"));
    }

    @Test
    @Order(3)
    public void testGetRole() {
        RoleRepresentation role = given()
                .when()
                .get("/keycloak/role/{realmName}/{roleName}", TEST_REALM_NAME, TEST_ROLE_NAME)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract()
                .as(RoleRepresentation.class);

        assertThat(role, notNullValue());
        assertThat(role.getName(), is(TEST_ROLE_NAME));
        assertThat(role.getDescription(), is("Test role for integration testing"));
    }

    @Test
    @Order(4)
    public void testListRoles() {
        List<RoleRepresentation> roles = given()
                .when()
                .get("/keycloak/role/{realmName}", TEST_REALM_NAME)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract()
                .body()
                .jsonPath()
                .getList(".", RoleRepresentation.class);

        assertThat(roles, notNullValue());
        assertThat(roles.size(), greaterThanOrEqualTo(2)); // At least our test roles + default roles
    }

    @Test
    @Order(5)
    public void testUpdateRole() {
        RoleRepresentation role = given()
                .when()
                .get("/keycloak/role/{realmName}/{roleName}", TEST_REALM_NAME, TEST_ROLE_NAME)
                .then()
                .statusCode(200)
                .extract()
                .as(RoleRepresentation.class);

        role.setDescription("Updated role description");

        given()
                .contentType(ContentType.JSON)
                .body(role)
                .when()
                .put("/keycloak/role/{realmName}/{roleName}", TEST_REALM_NAME, TEST_ROLE_NAME)
                .then()
                .statusCode(200)
                .body(is("Role updated successfully"));

        RoleRepresentation updatedRole = given()
                .when()
                .get("/keycloak/role/{realmName}/{roleName}", TEST_REALM_NAME, TEST_ROLE_NAME)
                .then()
                .statusCode(200)
                .extract()
                .as(RoleRepresentation.class);

        assertThat(updatedRole.getDescription(), is("Updated role description"));
    }

    @Test
    @Order(6)
    public void testAssignRoleToUser() {
        given()
                .when()
                .post("/keycloak/user-role/{realmName}/{username}/{roleName}",
                        TEST_REALM_NAME, TEST_USER_NAME, TEST_ROLE_NAME)
                .then()
                .statusCode(200)
                .body(is("Role assigned to user successfully"));
    }

    @Test
    @Order(7)
    public void testGetUserRoles() {
        List<RoleRepresentation> roles = given()
                .when()
                .get("/keycloak/user-role/{realmName}/{username}", TEST_REALM_NAME, TEST_USER_NAME)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract()
                .body()
                .jsonPath()
                .getList(".", RoleRepresentation.class);

        assertThat(roles, notNullValue());
        assertThat(roles.size(), greaterThanOrEqualTo(1));

        boolean foundTestRole = roles.stream()
                .anyMatch(r -> TEST_ROLE_NAME.equals(r.getName()));
        assertThat(foundTestRole, is(true));
    }

    @Test
    @Order(8)
    public void testRemoveRoleFromUser() {
        given()
                .when()
                .delete("/keycloak/user-role/{realmName}/{username}/{roleName}",
                        TEST_REALM_NAME, TEST_USER_NAME, TEST_ROLE_NAME)
                .then()
                .statusCode(200)
                .body(is("Role removed from user successfully"));
    }

    @Test
    @Order(100)
    public void testCleanupRoles() {
        String[] rolesToDelete = { TEST_ROLE_NAME, TEST_ROLE_NAME + "-pojo" };

        for (String roleName : rolesToDelete) {
            given()
                    .when()
                    .delete("/keycloak/role/{realmName}/{roleName}", TEST_REALM_NAME, roleName)
                    .then()
                    .statusCode(200)
                    .body(is("Role deleted successfully"));
        }
    }

    @Test
    @Order(101)
    public void testCleanup_DeleteRealm() {
        KeycloakRealmLifecycle.deleteRealm(TEST_REALM_NAME);
    }
}
