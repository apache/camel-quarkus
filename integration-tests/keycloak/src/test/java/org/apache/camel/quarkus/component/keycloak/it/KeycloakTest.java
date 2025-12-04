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
import java.util.UUID;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.config.ObjectMapperConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

@QuarkusTest
@QuarkusTestResource(KeycloakTestResource.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class KeycloakTest {

    // Test data - use unique names to avoid conflicts
    private static final String TEST_REALM_NAME = "test-realm-" + UUID.randomUUID().toString().substring(0, 8);
    private static final String TEST_USER_NAME = "test-user-" + UUID.randomUUID().toString().substring(0, 8);
    private static final String TEST_ROLE_NAME = "test-role-" + UUID.randomUUID().toString().substring(0, 8);

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

    @Test
    @Order(1)
    public void loadComponentKeycloak() {
        RestAssured.get("/keycloak/load/component/keycloak")
                .then()
                .statusCode(200);
    }

    // ==================== Realm Operations Tests ====================

    @Test
    @Order(2)
    public void testCreateRealmWithHeaders() {
        given()
                .when()
                .post("/keycloak/realm/{realmName}", TEST_REALM_NAME)
                .then()
                .statusCode(200)
                .body(is("Realm created successfully"));
    }

    @Test
    @Order(3)
    public void testCreateRealmWithPojo() {
        String pojoRealmName = TEST_REALM_NAME + "-pojo";

        RealmRepresentation realm = new RealmRepresentation();
        realm.setRealm(pojoRealmName);
        realm.setEnabled(true);
        realm.setDisplayName("Test Realm POJO");

        given()
                .contentType(ContentType.JSON)
                .body(realm)
                .when()
                .post("/keycloak/realm/pojo")
                .then()
                .statusCode(200)
                .body(is("Realm created successfully"));

        // Cleanup the POJO realm
        given()
                .when()
                .delete("/keycloak/realm/{realmName}", pojoRealmName)
                .then()
                .statusCode(200);
    }

    @Test
    @Order(4)
    public void testGetRealm() {
        RealmRepresentation realm = given()
                .when()
                .get("/keycloak/realm/{realmName}", TEST_REALM_NAME)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract()
                .as(RealmRepresentation.class);

        assertThat(realm, notNullValue());
        assertThat(realm.getRealm(), is(TEST_REALM_NAME));
        assertThat(realm.isEnabled(), is(true));
    }

    // ==================== User Operations Tests ====================

    @Test
    @Order(5)
    public void testCreateUserWithHeaders() {
        given()
                .queryParam("email", TEST_USER_NAME + "@test.com")
                .queryParam("firstName", "Test")
                .queryParam("lastName", "User")
                .when()
                .post("/keycloak/user/{realmName}/{username}", TEST_REALM_NAME, TEST_USER_NAME)
                .then()
                .statusCode(201)
                .body(is("User created successfully"));
    }

    @Test
    @Order(6)
    public void testCreateUserWithPojo() {
        String pojoUserName = TEST_USER_NAME + "-pojo";

        UserRepresentation user = new UserRepresentation();
        user.setUsername(pojoUserName);
        user.setEmail(pojoUserName + "@test.com");
        user.setFirstName("Test");
        user.setLastName("User POJO");
        user.setEnabled(true);

        given()
                .contentType(ContentType.JSON)
                .body(user)
                .when()
                .post("/keycloak/user/{realmName}/pojo", TEST_REALM_NAME)
                .then()
                .statusCode(201)
                .body(is("User created successfully"));
    }

    @Test
    @Order(7)
    public void testGetUser() {
        UserRepresentation user = given()
                .when()
                .get("/keycloak/user/{realmName}/{username}", TEST_REALM_NAME, TEST_USER_NAME)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract()
                .as(UserRepresentation.class);

        assertThat(user, notNullValue());
        assertThat(user.getUsername(), is(TEST_USER_NAME));
        assertThat(user.getEmail(), is(TEST_USER_NAME + "@test.com"));
        assertThat(user.getFirstName(), is("Test"));
        assertThat(user.getLastName(), is("User"));
    }

    @Test
    @Order(8)
    public void testListUsers() {
        List<UserRepresentation> users = given()
                .when()
                .get("/keycloak/user/{realmName}", TEST_REALM_NAME)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract()
                .body()
                .jsonPath()
                .getList(".", UserRepresentation.class);

        assertThat(users, notNullValue());
        assertThat(users.size(), greaterThanOrEqualTo(2)); // At least our two test users
    }

    // ==================== Role Operations Tests ====================

    @Test
    @Order(9)
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
    @Order(10)
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
    @Order(11)
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
    @Order(12)
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

    // ==================== User-Role Operations Tests ====================

    @Test
    @Order(13)
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
    @Order(14)
    public void testRemoveRoleFromUser() {
        given()
                .when()
                .delete("/keycloak/user-role/{realmName}/{username}/{roleName}",
                        TEST_REALM_NAME, TEST_USER_NAME, TEST_ROLE_NAME)
                .then()
                .statusCode(200)
                .body(is("Role removed from user successfully"));
    }

    // ==================== Error Handling Tests ====================

    @Test
    @Order(15)
    public void testErrorHandling_NonExistentRealm() {
        // Test with non-existent realm should fail
        given()
                .queryParam("email", "test@test.com")
                .queryParam("firstName", "Test")
                .queryParam("lastName", "User")
                .when()
                .post("/keycloak/user/{realmName}/{username}", "non-existent-realm", "testuser")
                .then()
                .statusCode(404); // Should fail since realm doesn't exist
    }

    @Test
    @Order(16)
    public void testErrorHandling_NonExistentUser() {
        // Test getting a user that doesn't exist
        given()
                .when()
                .get("/keycloak/user/{realmName}/{username}", TEST_REALM_NAME, "non-existent-user")
                .then()
                .statusCode(500); // Should fail since user doesn't exist
    }

    @Test
    @Order(17)
    public void testErrorHandling_NonExistentRole() {
        // Test getting a role that doesn't exist
        given()
                .when()
                .get("/keycloak/role/{realmName}/{roleName}", TEST_REALM_NAME, "non-existent-role")
                .then()
                .statusCode(500); // Should fail since role doesn't exist
    }

    // ==================== Cleanup Tests ====================

    @Test
    @Order(98)
    public void testCleanupRoles() {
        // Delete test roles
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
    @Order(99)
    public void testCleanupUsers() {
        // Delete test users
        String[] usersToDelete = { TEST_USER_NAME, TEST_USER_NAME + "-pojo" };

        for (String username : usersToDelete) {
            given()
                    .when()
                    .delete("/keycloak/user/{realmName}/{username}", TEST_REALM_NAME, username)
                    .then()
                    .statusCode(200)
                    .body(is("User deleted successfully"));
        }
    }

    @Test
    @Order(100)
    public void testCleanupRealm() {
        // Delete the test realm (this will also delete all users and roles in it)
        given()
                .when()
                .delete("/keycloak/realm/{realmName}", TEST_REALM_NAME)
                .then()
                .statusCode(200)
                .body(is("Realm deleted successfully"));
    }

    @Test
    @Order(101)
    public void testVerifyRealmDeleted() {
        // Verify that the realm was actually deleted by expecting a failure
        given()
                .when()
                .get("/keycloak/realm/{realmName}", TEST_REALM_NAME)
                .then()
                .statusCode(500); // Should fail since realm no longer exists
    }
}
