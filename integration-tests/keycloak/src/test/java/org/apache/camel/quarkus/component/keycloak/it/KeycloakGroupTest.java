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
import org.keycloak.representations.idm.GroupRepresentation;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

@QuarkusTest
@QuarkusTestResource(KeycloakTestResource.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class KeycloakGroupTest extends KeycloakTestBase {

    @Test
    @Order(1)
    public void testSetup_CreateRealm() {
        KeycloakRealmLifecycle.createRealmWithSmtp(TEST_REALM_NAME);

        // Create a test user for group-user operations
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
    public void testCreateGroupWithHeaders() {
        given()
                .when()
                .post("/keycloak/group/{realmName}/{groupName}", TEST_REALM_NAME, TEST_GROUP_NAME)
                .then()
                .statusCode(201)
                .body(is("Group created successfully"));
    }

    @Test
    @Order(2)
    public void testCreateGroupWithPojo() {
        String pojoGroupName = TEST_GROUP_NAME + "-pojo";

        GroupRepresentation group = new GroupRepresentation();
        group.setName(pojoGroupName);

        given()
                .contentType(ContentType.JSON)
                .body(group)
                .when()
                .post("/keycloak/group/{realmName}/pojo", TEST_REALM_NAME)
                .then()
                .statusCode(201)
                .body(is("Group created successfully"));
    }

    @Test
    @Order(3)
    public void testListGroups() {
        List<GroupRepresentation> groups = given()
                .when()
                .get("/keycloak/group/{realmName}", TEST_REALM_NAME)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract()
                .body()
                .jsonPath()
                .getList(".", GroupRepresentation.class);

        assertThat(groups, notNullValue());
        assertThat(groups.size(), greaterThanOrEqualTo(2)); // At least our two test groups
    }

    @Test
    @Order(4)
    public void testGetGroup() {
        GroupRepresentation group = given()
                .when()
                .get("/keycloak/group/{realmName}/{groupName}", TEST_REALM_NAME, TEST_GROUP_NAME)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract()
                .as(GroupRepresentation.class);

        assertThat(group, notNullValue());
        assertThat(group.getName(), is(TEST_GROUP_NAME));
    }

    @Test
    @Order(5)
    public void testUpdateGroup() {
        GroupRepresentation group = given()
                .when()
                .get("/keycloak/group/{realmName}/{groupName}", TEST_REALM_NAME, TEST_GROUP_NAME)
                .then()
                .statusCode(200)
                .extract()
                .as(GroupRepresentation.class);

        group.getAttributes().put("description", List.of("Updated group description"));

        given()
                .contentType(ContentType.JSON)
                .body(group)
                .when()
                .put("/keycloak/group/{realmName}/{groupName}", TEST_REALM_NAME, TEST_GROUP_NAME)
                .then()
                .statusCode(200)
                .body(is("Group updated successfully"));
    }

    @Test
    @Order(6)
    public void testAddUserToGroup() {
        given()
                .when()
                .post("/keycloak/group-user/{realmName}/{username}/{groupName}",
                        TEST_REALM_NAME, TEST_USER_NAME, TEST_GROUP_NAME)
                .then()
                .statusCode(200)
                .body(is("User added to group successfully"));
    }

    @Test
    @Order(7)
    public void testListUserGroups() {
        List<GroupRepresentation> groups = given()
                .when()
                .get("/keycloak/group-user/{realmName}/{username}", TEST_REALM_NAME, TEST_USER_NAME)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract()
                .body()
                .jsonPath()
                .getList(".", GroupRepresentation.class);

        assertThat(groups, notNullValue());
        assertThat(groups.size(), greaterThanOrEqualTo(1));
    }

    @Test
    @Order(8)
    public void testRemoveUserFromGroup() {
        given()
                .when()
                .delete("/keycloak/group-user/{realmName}/{username}/{groupName}",
                        TEST_REALM_NAME, TEST_USER_NAME, TEST_GROUP_NAME)
                .then()
                .statusCode(200)
                .body(is("User removed from group successfully"));
    }

    @Test
    @Order(100)
    public void testCleanupGroups() {
        String[] groupsToDelete = { TEST_GROUP_NAME, TEST_GROUP_NAME + "-pojo" };

        for (String groupName : groupsToDelete) {
            given()
                    .when()
                    .delete("/keycloak/group/{realmName}/{groupName}", TEST_REALM_NAME, groupName)
                    .then()
                    .statusCode(200)
                    .body(is("Group deleted successfully"));
        }
    }

    @Test
    @Order(101)
    public void testCleanup_DeleteRealm() {
        KeycloakRealmLifecycle.deleteRealm(TEST_REALM_NAME);
    }
}
