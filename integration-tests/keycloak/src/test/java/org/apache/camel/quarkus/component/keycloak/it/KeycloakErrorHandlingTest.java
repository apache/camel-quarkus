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

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.*;

import static io.restassured.RestAssured.given;

/**
 * Tests for error handling scenarios in the Keycloak component.
 */
@QuarkusTest
@QuarkusTestResource(KeycloakTestResource.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class KeycloakErrorHandlingTest extends KeycloakTestBase {

    @Test
    @Order(1)
    public void testSetup_CreateRealm() {
        KeycloakRealmLifecycle.createRealmWithSmtp(TEST_REALM_NAME);
    }

    @Test
    @Order(2)
    public void testErrorHandling_NonExistentRealm() {
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
    @Order(3)
    public void testErrorHandling_NonExistentUser() {
        given()
                .when()
                .get("/keycloak/user/{realmName}/{username}", TEST_REALM_NAME, "non-existent-user")
                .then()
                .statusCode(500); // Should fail since user doesn't exist
    }

    @Test
    @Order(4)
    public void testErrorHandling_NonExistentRole() {
        given()
                .when()
                .get("/keycloak/role/{realmName}/{roleName}", TEST_REALM_NAME, "non-existent-role")
                .then()
                .statusCode(500); // Should fail since role doesn't exist
    }

    @Test
    @Order(99)
    public void testCleanup_DeleteRealm() {
        KeycloakRealmLifecycle.deleteRealm(TEST_REALM_NAME);
    }
}
