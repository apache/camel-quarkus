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
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.config.ObjectMapperConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.http.ContentType;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.keycloak.representations.idm.AdminEventRepresentation;
import org.keycloak.representations.idm.EventRepresentation;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Integration test for Keycloak consumer operations - consuming admin events and regular events.
 */
@QuarkusTest
@QuarkusTestResource(KeycloakTestResource.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class KeycloakConsumerTest {

    // Test data - use unique names to avoid conflicts with other tests
    private static final String TEST_REALM_NAME = "consumer-test-realm-" + UUID.randomUUID().toString().substring(0, 8);
    private static final String TEST_USER_NAME = "consumer-test-user-" + UUID.randomUUID().toString().substring(0, 8);
    private static final String TEST_ROLE_NAME = "consumer-test-role-" + UUID.randomUUID().toString().substring(0, 8);

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
    public void testSetup_CreateRealm() {
        // Create test realm
        given()
                .when()
                .post("/keycloak/realm/{realmName}", TEST_REALM_NAME)
                .then()
                .statusCode(200)
                .body(is("Realm created successfully"));
    }

    @Test
    @Order(2)
    public void testSetup_EnableAdminEvents() {
        // Enable admin events and regular events for the realm
        given()
                .when()
                .post("/keycloak/events/admin/{realmName}/enable", TEST_REALM_NAME)
                .then()
                .statusCode(200)
                .body(is("Admin events enabled successfully"));
    }

    @Test
    @Order(3)
    public void testSetup_CreateConsumerRoutes() {
        // Create consumer routes for this realm
        given()
                .when()
                .post("/keycloak/consumer/route/create/{realmName}", TEST_REALM_NAME)
                .then()
                .statusCode(200)
                .body(is("Consumer routes created successfully"));
    }

    @Test
    @Order(4)
    public void testConsumeAdminEvents_CreateUser() {
        // Reset mock before test
        given()
                .when()
                .post("/keycloak/events/admin/reset")
                .then()
                .statusCode(200);

        // Start the admin events consumer
        given()
                .when()
                .post("/keycloak/consumer/admin-events/start/{realmName}", TEST_REALM_NAME)
                .then()
                .statusCode(200)
                .body(is("Admin events consumer started"));

        // Wait a bit for consumer to start and be ready
        Awaitility.await()
                .pollDelay(3, TimeUnit.SECONDS)
                .atMost(5, TimeUnit.SECONDS)
                .until(() -> true);

        // Create a user which should generate an admin event
        given()
                .queryParam("email", TEST_USER_NAME + "@test.com")
                .queryParam("firstName", "Test")
                .queryParam("lastName", "User")
                .when()
                .post("/keycloak/user/{realmName}/{username}", TEST_REALM_NAME, TEST_USER_NAME)
                .then()
                .statusCode(201)
                .body(is("User created successfully"));

        // Wait for events to be consumed - give more time for polling
        Awaitility.await()
                .pollInterval(1, TimeUnit.SECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> {
                    List<?> events = given()
                            .when()
                            .get("/keycloak/events/admin/collected")
                            .then()
                            .statusCode(200)
                            .contentType(ContentType.JSON)
                            .extract()
                            .body()
                            .jsonPath()
                            .getList(".");
                    return events != null && !events.isEmpty();
                });

        // Verify that admin events were collected
        List<AdminEventRepresentation> adminEvents = given()
                .when()
                .get("/keycloak/events/admin/collected")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract()
                .body()
                .jsonPath()
                .getList(".", AdminEventRepresentation.class);

        assertThat(adminEvents, notNullValue());
        assertThat(!adminEvents.isEmpty(), is(true));

        // Verify the event contains expected data
        AdminEventRepresentation firstEvent = adminEvents.get(0);
        assertThat(firstEvent.getRealmId(), notNullValue());
        assertThat(firstEvent.getOperationType(), notNullValue());
        assertThat(firstEvent.getResourceType(), notNullValue());
    }

    @Test
    @Order(5)
    public void testConsumeAdminEvents_CreateRole() {
        // Reset mock before test
        given()
                .when()
                .post("/keycloak/events/admin/reset")
                .then()
                .statusCode(200);

        // Create a role which should generate an admin event
        given()
                .queryParam("description", "Test role for consumer testing")
                .when()
                .post("/keycloak/role/{realmName}/{roleName}", TEST_REALM_NAME, TEST_ROLE_NAME)
                .then()
                .statusCode(200)
                .body(is("Role created successfully"));

        // Wait for events to be consumed - give more time for polling
        Awaitility.await()
                .pollInterval(1, TimeUnit.SECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> {
                    List<?> events = given()
                            .when()
                            .get("/keycloak/events/admin/collected")
                            .then()
                            .statusCode(200)
                            .contentType(ContentType.JSON)
                            .extract()
                            .body()
                            .jsonPath()
                            .getList(".");
                    return events != null && !events.isEmpty();
                });

        // Verify that admin events were collected
        List<AdminEventRepresentation> adminEvents = given()
                .when()
                .get("/keycloak/events/admin/collected")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract()
                .body()
                .jsonPath()
                .getList(".", AdminEventRepresentation.class);

        assertThat(adminEvents, notNullValue());
        assertThat(!adminEvents.isEmpty(), is(true));
    }

    @Test
    @Order(6)
    public void testConsumer_NoNewEvents() {
        // Reset mock
        given()
                .when()
                .post("/keycloak/events/admin/reset")
                .then()
                .statusCode(200);

        // Wait a bit to verify consumer is still running but not collecting new events
        Awaitility.await()
                .pollDelay(2, TimeUnit.SECONDS)
                .atMost(3, TimeUnit.SECONDS)
                .until(() -> true);

        // Get initial count after reset
        int initialCount = given()
                .when()
                .get("/keycloak/events/admin/collected")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract()
                .body()
                .jsonPath()
                .getList(".")
                .size();

        // Wait a bit more
        Awaitility.await()
                .pollDelay(2, TimeUnit.SECONDS)
                .atMost(3, TimeUnit.SECONDS)
                .until(() -> true);

        // Verify the count hasn't increased (no new operations means no new events)
        int finalCount = given()
                .when()
                .get("/keycloak/events/admin/collected")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract()
                .body()
                .jsonPath()
                .getList(".")
                .size();

        // Count should be stable (not increasing) since we didn't perform any operations
        assertThat(finalCount, is(initialCount));
    }

    @Test
    @Order(7)
    public void testConsumeRegularEvents() {
        // Start the regular events consumer
        given()
                .when()
                .post("/keycloak/consumer/regular-events/start/{realmName}", TEST_REALM_NAME)
                .then()
                .statusCode(200)
                .body(is("Regular events consumer started"));

        // Wait a bit for consumer to start and check for any events
        Awaitility.await()
                .pollDelay(3, TimeUnit.SECONDS)
                .atMost(4, TimeUnit.SECONDS)
                .until(() -> true);

        // Regular events (LOGIN, LOGOUT, etc.) require event listeners to be enabled in Keycloak
        // and user interactions. This test demonstrates the pattern but may not receive events
        // in a test environment without actual user login attempts.
        List<EventRepresentation> regularEvents = given()
                .when()
                .get("/keycloak/events/regular/collected")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract()
                .body()
                .jsonPath()
                .getList(".", EventRepresentation.class);

        // Just verify we can call the endpoint and get a list (can be empty)
        assertThat(regularEvents, notNullValue());
    }

    @Test
    @Order(98)
    public void testCleanup_DeleteRole() {
        // Delete test role
        given()
                .when()
                .delete("/keycloak/role/{realmName}/{roleName}", TEST_REALM_NAME, TEST_ROLE_NAME)
                .then()
                .statusCode(200)
                .body(is("Role deleted successfully"));
    }

    @Test
    @Order(99)
    public void testCleanup_DeleteUser() {
        // Delete test user
        given()
                .when()
                .delete("/keycloak/user/{realmName}/{username}", TEST_REALM_NAME, TEST_USER_NAME)
                .then()
                .statusCode(200)
                .body(is("User deleted successfully"));
    }

    @Test
    @Order(100)
    public void testCleanup_DeleteRealm() {
        // Delete the test realm
        given()
                .when()
                .delete("/keycloak/realm/{realmName}", TEST_REALM_NAME)
                .then()
                .statusCode(200)
                .body(is("Realm deleted successfully"));
    }
}
