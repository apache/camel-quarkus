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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.common.mapper.TypeRef;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItems;

@QuarkusTest
@QuarkusTestResource(KeycloakTestResource.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class KeycloakUserTest extends KeycloakTestBase {

    @Test
    @Order(1)
    public void testSetup_CreateRealm() {
        KeycloakRealmLifecycle.createRealmWithSmtp(TEST_REALM_NAME);
    }

    @Test
    @Order(2)
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
    @Order(3)
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
    @Order(4)
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
    @Order(5)
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

    @Test
    @Order(6)
    public void testUpdateUser() {
        // First get the user
        UserRepresentation user = given()
                .when()
                .get("/keycloak/user/{realmName}/{username}", TEST_REALM_NAME, TEST_USER_NAME)
                .then()
                .statusCode(200)
                .extract()
                .as(UserRepresentation.class);

        // Update the user's first name
        user.setFirstName("UpdatedFirstName");
        user.setLastName("UpdatedLastName");

        given()
                .contentType(ContentType.JSON)
                .body(user)
                .when()
                .put("/keycloak/user/{realmName}/{username}", TEST_REALM_NAME, TEST_USER_NAME)
                .then()
                .statusCode(200)
                .body(is("User updated successfully"));

        // Verify the update
        UserRepresentation updatedUser = given()
                .when()
                .get("/keycloak/user/{realmName}/{username}", TEST_REALM_NAME, TEST_USER_NAME)
                .then()
                .statusCode(200)
                .extract()
                .as(UserRepresentation.class);

        assertThat(updatedUser.getFirstName(), is("UpdatedFirstName"));
        assertThat(updatedUser.getLastName(), is("UpdatedLastName"));
    }

    @Test
    @Order(7)
    public void testSearchUsers() {
        // Search for users by username prefix
        List<UserRepresentation> users = given()
                .queryParam("query", TEST_USER_NAME)
                .when()
                .get("/keycloak/user/{realmName}/search", TEST_REALM_NAME)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract()
                .body()
                .jsonPath()
                .getList(".", UserRepresentation.class);

        assertThat(users, notNullValue());
        boolean foundTestUser = users.stream()
                .anyMatch(u -> TEST_USER_NAME.equals(u.getUsername()));
        assertThat(foundTestUser, is(true));
    }

    @Test
    @Order(8)
    public void testListUserSessions() {
        List<?> sessions = given()
                .when()
                .get("/keycloak/user/{realmName}/{username}/sessions", TEST_REALM_NAME, TEST_USER_NAME)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract()
                .body()
                .jsonPath()
                .getList(".");

        // Just verify we can call the endpoint successfully (can be empty list)
        assertThat(sessions, notNullValue());
    }

    @Test
    @Order(9)
    public void testResetUserPassword() {
        given()
                .queryParam("password", "newTestPassword123!")
                .queryParam("temporary", false)
                .when()
                .post("/keycloak/user/{realmName}/{username}/reset-password", TEST_REALM_NAME, TEST_USER_NAME)
                .then()
                .statusCode(200)
                .body(is("Password reset successfully"));
    }

    @Test
    @Order(10)
    public void testSendVerifyEmail() {
        given()
                .when()
                .post("/keycloak/user/{realmName}/{username}/send-verify-email", TEST_REALM_NAME, TEST_USER_NAME)
                .then()
                .statusCode(200)
                .body(is("Verify email sent successfully"));
    }

    @Test
    @Order(11)
    public void testSendPasswordResetEmail() {
        given()
                .when()
                .post("/keycloak/user/{realmName}/{username}/send-password-reset-email", TEST_REALM_NAME,
                        TEST_USER_NAME)
                .then()
                .statusCode(200)
                .body(is("Password reset email sent successfully"));
    }

    @Test
    @Order(12)
    public void testLogoutUser() {
        given()
                .when()
                .post("/keycloak/user/{realmName}/{username}/logout", TEST_REALM_NAME, TEST_USER_NAME)
                .then()
                .statusCode(200)
                .body(is("User logged out successfully"));
    }

    @Test
    @Order(13)
    public void testSetUserAttribute() {
        given()
                .queryParam("attributeValue", "test-department")
                .when()
                .post("/keycloak/user-attribute/{realmName}/{username}/{attributeName}",
                        TEST_REALM_NAME, TEST_USER_NAME, "department")
                .then()
                .statusCode(200)
                .body(is("User attribute set successfully"));
    }

    @Test
    @Order(14)
    public void testGetUserAttributes() {
        Map<String, List<String>> attributes = given()
                .when()
                .get("/keycloak/user-attribute/{realmName}/{username}", TEST_REALM_NAME, TEST_USER_NAME)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract()
                .body()
                .as(new TypeRef<>() {
                });

        assertThat(attributes, notNullValue());
        if (attributes.containsKey("department")) {
            assertThat(attributes.get("department").get(0), is("test-department"));
        }
    }

    @Test
    @Order(15)
    public void testDeleteUserAttribute() {
        Map<String, List<String>> attributesBefore = given()
                .when()
                .get("/keycloak/user-attribute/{realmName}/{username}", TEST_REALM_NAME, TEST_USER_NAME)
                .then()
                .statusCode(200)
                .extract()
                .body()
                .as(new TypeRef<>() {
                });

        if (attributesBefore.containsKey("department")) {
            given()
                    .when()
                    .delete("/keycloak/user-attribute/{realmName}/{username}/{attributeName}",
                            TEST_REALM_NAME, TEST_USER_NAME, "department")
                    .then()
                    .statusCode(200)
                    .body(is("User attribute deleted successfully"));

            Map<String, List<String>> attributes = given()
                    .when()
                    .get("/keycloak/user-attribute/{realmName}/{username}", TEST_REALM_NAME, TEST_USER_NAME)
                    .then()
                    .statusCode(200)
                    .extract()
                    .body()
                    .as(new TypeRef<>() {
                    });

            assertThat(attributes.containsKey("department"), is(false));
        }
    }

    @Test
    @Order(16)
    public void testGetUserCredentials() {
        List<CredentialRepresentation> credentials = given()
                .when()
                .get("/keycloak/user-credential/{realmName}/{username}", TEST_REALM_NAME, TEST_USER_NAME)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract()
                .body()
                .jsonPath()
                .getList(".", CredentialRepresentation.class);

        assertThat(credentials, notNullValue());
    }

    @Test
    @Order(17)
    public void testDeleteUserCredential() {
        List<CredentialRepresentation> credentials = given()
                .when()
                .get("/keycloak/user-credential/{realmName}/{username}", TEST_REALM_NAME, TEST_USER_NAME)
                .then()
                .statusCode(200)
                .extract()
                .body()
                .jsonPath()
                .getList(".", CredentialRepresentation.class);

        if (credentials != null && !credentials.isEmpty()) {
            String credentialId = credentials.get(0).getId();

            given()
                    .when()
                    .delete("/keycloak/user-credential/{realmName}/{username}/{credentialId}",
                            TEST_REALM_NAME, TEST_USER_NAME, credentialId)
                    .then()
                    .statusCode(200)
                    .body(is("User credential deleted successfully"));
        }
    }

    @Test
    @Order(18)
    public void testAddRequiredAction() {
        given()
                .queryParam("action", "UPDATE_PASSWORD")
                .when()
                .post("/keycloak/user-action/{realmName}/{username}/add", TEST_REALM_NAME, TEST_USER_NAME)
                .then()
                .statusCode(200)
                .body(is("Required action added successfully"));
    }

    @Test
    @Order(19)
    public void testRemoveRequiredAction() {
        given()
                .queryParam("action", "UPDATE_PASSWORD")
                .when()
                .post("/keycloak/user-action/{realmName}/{username}/remove", TEST_REALM_NAME, TEST_USER_NAME)
                .then()
                .statusCode(200)
                .body(is("Required action removed successfully"));
    }

    @Test
    @Order(20)
    public void testExecuteActionsEmail() {
        given()
                .queryParam("actions", "VERIFY_EMAIL,UPDATE_PASSWORD")
                .queryParam("lifespan", 3600)
                .when()
                .post("/keycloak/user-action/{realmName}/{username}/execute", TEST_REALM_NAME, TEST_USER_NAME)
                .then()
                .statusCode(200)
                .body(is("Actions email sent successfully"));
    }

    @Test
    @Order(21)
    public void testBulkCreateUsers() {
        String bulkUserNameOne = TEST_USER_NAME + "-bulkOne";

        UserRepresentation userOne = new UserRepresentation();
        userOne.setUsername(bulkUserNameOne);
        userOne.setEmail(bulkUserNameOne + "@test.com");
        userOne.setFirstName("Test One");
        userOne.setLastName("User Bulk One");
        userOne.setEnabled(true);

        String bulkUserNameTwo = TEST_USER_NAME + "-bulkTwo";

        UserRepresentation userTwo = new UserRepresentation();
        userTwo.setUsername(bulkUserNameTwo);
        userTwo.setEmail(bulkUserNameTwo + "@test.com");
        userTwo.setFirstName("Test Two");
        userTwo.setLastName("User Bulk Two");
        userTwo.setEnabled(true);

        List<UserRepresentation> users = new ArrayList<>();
        users.add(userOne);
        users.add(userTwo);

        given()
                .contentType(ContentType.JSON)
                .body(users)
                .when()
                .post("/keycloak/user/{realmName}", TEST_REALM_NAME)
                .then()
                .statusCode(200)
                .body("total", is(2))
                .body("success", is(2))
                .body("results.username", hasItems(bulkUserNameOne, bulkUserNameTwo))
                .body("results.status", hasItem("success"));
    }

    @Test
    @Order(22)
    public void testBulkUpdateUsers() {
        //First get list of users
        List<UserRepresentation> batchCreatedUsers = given()
                .when()
                .get("/keycloak/user/{realmName}", TEST_REALM_NAME)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract()
                .body()
                .jsonPath()
                .getList(".", UserRepresentation.class).stream().filter(user -> user.getUsername().contains("bulk")).toList();

        //update firstname and lastname of each user
        batchCreatedUsers.forEach(user -> {
            user.setFirstName("updatedFirstNameForBulkCreatedUsers");
            user.setLastName("updatedLastNameForBulkCreatedUsers");
        });

        //bulk update users
        given()
                .contentType(ContentType.JSON)
                .body(batchCreatedUsers)
                .when()
                .put("/keycloak/user/{realmName}", TEST_REALM_NAME)
                .then()
                .statusCode(200)
                .body("total", is(batchCreatedUsers.size()))
                .body("success", is(batchCreatedUsers.size()));

        //verify updated result
        List<UserRepresentation> updatedUsers = given()
                .when()
                .get("/keycloak/user/{realmName}", TEST_REALM_NAME)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract()
                .body()
                .jsonPath()
                .getList(".", UserRepresentation.class).stream().filter(user -> user.getUsername().contains("bulk")).toList();

        Map<String, UserRepresentation> updatedUserMap = updatedUsers.stream()
                .collect(Collectors.toMap(UserRepresentation::getUsername, u -> u));

        for (UserRepresentation user : batchCreatedUsers) {
            UserRepresentation updatedUser = updatedUserMap.get(user.getUsername());
            assertThat(updatedUser, notNullValue());
            assertThat(updatedUser.getFirstName(), is("updatedFirstNameForBulkCreatedUsers"));
            assertThat(updatedUser.getLastName(), is("updatedLastNameForBulkCreatedUsers"));
        }
    }

    @Test
    @Order(23)
    public void testBulkUpdateUsersWithContinueOnError() {
        //First get list of users
        List<UserRepresentation> batchCreatedUsers = new ArrayList<>(given()
                .when()
                .get("/keycloak/user/{realmName}", TEST_REALM_NAME)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract()
                .body()
                .jsonPath()
                .getList(".", UserRepresentation.class).stream().filter(user -> user.getUsername().contains("bulk")).toList());

        //add a wrong user at the beginning, even failed to handle but still keep processing
        batchCreatedUsers.add(0, new UserRepresentation());

        //bulk update users
        given()
                .contentType(ContentType.JSON)
                .body(batchCreatedUsers)
                .header("continueOnError", true)
                .when()
                .put("/keycloak/user/{realmName}", TEST_REALM_NAME)
                .then()
                .statusCode(200)
                .body("total", is(batchCreatedUsers.size()))
                .body("success", is(2))
                .body("failed", is(1));
    }

    @Test
    @Order(24)
    public void testBulkDeleteUsers() {
        // First get list of users
        List<String> batchCreatedUsers = given()
                .when()
                .get("/keycloak/user/{realmName}", TEST_REALM_NAME)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract()
                .body()
                .jsonPath()
                .getList(".", UserRepresentation.class).stream().map(UserRepresentation::getUsername)
                .filter(username -> username.contains("bulk")).toList();

        // Bulk delete users
        given()
                .when()
                .contentType(ContentType.JSON)
                .body(batchCreatedUsers)
                .delete("/keycloak/user/{realmName}", TEST_REALM_NAME)
                .then()
                .statusCode(200)
                .body("total", is(batchCreatedUsers.size()))
                .body("success", is(batchCreatedUsers.size()))
                .body("results.size()", is(batchCreatedUsers.size()));

    }

    @Test
    @Order(100)
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
    @Order(101)
    public void testCleanup_DeleteRealm() {
        KeycloakRealmLifecycle.deleteRealm(TEST_REALM_NAME);
    }
}
