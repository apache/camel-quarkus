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
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
@QuarkusTestResource(KeycloakTestResource.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class KeycloakSecurityPolicyTest extends KeycloakSecurityPolicyTestBase {

    private static String adminToken;
    private static String normalUserToken;
    private static String attackerToken;

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

        adminToken = getAccessToken(ADMIN_USER, ADMIN_PASSWORD, config("test.client.id"), TEST_CLIENT_SECRET);
        normalUserToken = getAccessToken(NORMAL_USER, NORMAL_PASSWORD, config("test.client.id"), TEST_CLIENT_SECRET);
        attackerToken = getAccessToken(ATTACKER_USER, ATTACKER_PASSWORD, config("test.client.id"), TEST_CLIENT_SECRET);
    }

    @Test
    @Order(2)
    public void testPropertyTokenWorks() {
        given()
                .when()
                .queryParam("propertyToken", normalUserToken)
                .get("/keycloak/secure-policy/user-with-token-in-property")
                .then()
                .statusCode(200)
                .body(is("Access granted - secure default"));
    }

    @Test
    @Order(3)
    public void testHeaderTokenWorks() {
        given()
                .when()
                .queryParam("headerToken", normalUserToken)
                .get("/keycloak/secure-policy/user-with-token-in-header")
                .then()
                .statusCode(200)
                .body(is("Access granted - secure default"));
    }

    @Test
    @Order(4)
    public void testPropertyPreferredOverHeaderTokenWorks() {
        given()
                .when()
                .queryParam("propertyToken", normalUserToken)
                .queryParam("headerToken", attackerToken)
                .get("/keycloak/secure-policy/user-with-token-in-property-and-header")
                .then()
                .statusCode(200)
                .body(is("Access granted - secure default"));
    }

    @Test
    @Order(5)
    public void testInvalidHeaderIgnoredWhenPropertyValid() {
        given()
                .when()
                .queryParam("propertyToken", normalUserToken)
                .queryParam("headerToken", "invalid.token")
                .get("/keycloak/secure-policy/user-with-token-in-property-and-header")
                .then()
                .statusCode(200)
                .body(is("Access granted - secure default"));
    }

    @Test
    @Order(6)
    public void testHeaderRejectedWhenHeadersDisabled() {
        given()
                .when()
                .queryParam("headerToken", normalUserToken)
                .get("/keycloak/secure-policy/max-security")
                .then()
                .statusCode(500)
                .body(containsString("Access token not found in exchange"));
    }

    @Test
    @Order(7)
    public void testPropertyWorksWhenHeadersDisabled() {
        given()
                .when()
                .queryParam("propertyToken", normalUserToken)
                .get("/keycloak/secure-policy/max-security")
                .then()
                .statusCode(200)
                .body(is("Access granted - max security"));
    }

    @Test
    @Order(8)
    public void testPropertyTokenUsedNotHeader() {
        given()
                .when()
                .queryParam("propertyToken", normalUserToken)
                .queryParam("headerToken", adminToken)
                .get("/keycloak/secure-policy/user-with-token-in-property-and-header")
                .then()
                .statusCode(200)
                .body(is("Access granted - secure default"));
    }

    @Test
    @Order(9)
    public void testAttackScenario_SessionHijacking() {
        given()
                .when()
                .queryParam("propertyToken", normalUserToken)
                .queryParam("headerToken", attackerToken)
                .get("/keycloak/secure-policy/user-with-token-in-property-and-header")
                .then()
                .statusCode(200)
                .body(is("Access granted - secure default"));
    }

    @Test
    @Order(10)
    public void testAttackScenario_LegacyUnsafe() {
        given()
                .when()
                .queryParam("propertyToken", normalUserToken)
                .queryParam("headerToken", attackerToken)
                .get("/keycloak/secure-policy/legacy-unsafe")
                .then()
                .statusCode(500)
                .body(containsString("Token mismatch detected"));
    }

    @Test
    @Order(11)
    public void testAuthorizationHeaderFormat() {
        given()
                .when()
                .queryParam("headerToken", normalUserToken)
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
        given()
                .when()
                .queryParam("propertyToken", adminToken)
                .queryParam("headerToken", normalUserToken)
                .get("/keycloak/secure-policy/admin-only")
                .then()
                .statusCode(200)
                .body(is("Admin access granted"));
    }

    @Test
    @Order(14)
    public void testIntrospectionEnabledWithDefaultCacheConcurrentMap() {
        given()
                .when()
                .queryParam("propertyToken", adminToken)
                .queryParam("clientId", config("test.client.id"))
                .queryParam("clientSecret", TEST_CLIENT_SECRET)
                .get("/keycloak/secure-policy/introspection-cache-concurrent-map")
                .then()
                .statusCode(200)
                .body(is("Access granted - concurrent map cache"));
    }

    @Test
    @Order(15)
    public void testIntrospectionEnabledWithNoCache() {
        given()
                .when()
                .queryParam("propertyToken", adminToken)
                .queryParam("clientId", config("test.client.id"))
                .queryParam("clientSecret", TEST_CLIENT_SECRET)
                .get("/keycloak/secure-policy/introspection-no-cache")
                .then()
                .statusCode(200)
                .body(is("Access granted - no cache"));
    }

    @Test
    @Order(16)
    public void testIntrospector_concurrentMapCache_tokenIsActive() {
        given()
                .queryParam("accessToken", normalUserToken)
                .queryParam("clientId", config("test.client.id"))
                .queryParam("clientSecret", TEST_CLIENT_SECRET)
                .get("/keycloak/introspection-cache/introspector/concurrent-map")
                .then()
                .statusCode(200)
                .body("active", is(true))
                .body("subject", notNullValue())
                .body("cacheSize", greaterThan(0));
    }

    @Test
    @Order(17)
    public void testIntrospector_concurrentMapCache_invalidToken_returnsInactive() {
        given()
                .queryParam("accessToken", "invalid.token")
                .queryParam("clientId", config("test.client.id"))
                .queryParam("clientSecret", TEST_CLIENT_SECRET)
                .get("/keycloak/introspection-cache/introspector/concurrent-map")
                .then()
                .statusCode(200)
                .body("active", is(false));
    }

    @Test
    @Order(18)
    public void testIntrospector_caffeineCache_tokenIsActive() {
        given()
                .queryParam("accessToken", normalUserToken)
                .queryParam("clientId", config("test.client.id"))
                .queryParam("clientSecret", TEST_CLIENT_SECRET)
                .get("/keycloak/introspection-cache/introspector/caffeine")
                .then()
                .statusCode(200)
                .body("active", is(true))
                .body("subject", notNullValue())
                .body("cacheSize", greaterThan(0));
    }

    @Test
    @Order(19)
    public void testIntrospector_caffeineCache_invalidToken_returnsInactive() {
        given()
                .queryParam("accessToken", "invalid.token")
                .queryParam("clientId", config("test.client.id"))
                .queryParam("clientSecret", TEST_CLIENT_SECRET)
                .get("/keycloak/introspection-cache/introspector/caffeine")
                .then()
                .statusCode(200)
                .body("active", is(false));
    }

    @Test
    @Order(20)
    public void testIntrospector_noCache_tokenIsActive() {
        given()
                .queryParam("accessToken", normalUserToken)
                .queryParam("clientId", config("test.client.id"))
                .queryParam("clientSecret", TEST_CLIENT_SECRET)
                .get("/keycloak/introspection-cache/introspector/none")
                .then()
                .statusCode(200)
                .body("active", is(true))
                .body("subject", notNullValue())
                .body("cacheSize", is(0));
    }

    @Test
    @Order(21)
    public void testIntrospector_noCache_invalidToken_returnsInactive() {
        given()
                .queryParam("accessToken", "invalid.token")
                .queryParam("clientId", config("test.client.id"))
                .queryParam("clientSecret", TEST_CLIENT_SECRET)
                .get("/keycloak/introspection-cache/introspector/none")
                .then()
                .statusCode(200)
                .body("active", is(false));
    }

    @Test
    @Order(22)
    public void testIntrospector_caffeineStats_secondCallHitsCache() {
        given()
                .queryParam("accessToken", normalUserToken)
                .queryParam("clientId", config("test.client.id"))
                .queryParam("clientSecret", TEST_CLIENT_SECRET)
                .get("/keycloak/introspection-cache/introspector/caffeine-stats")
                .then()
                .statusCode(200)
                .body("hitCount", is(1))
                .body("missCount", is(1))
                .body("hitRate", is(0.5f))
                .body("cacheSize", greaterThan(0));
    }

    @Test
    @Order(23)
    public void testIntrospector_noCache_invalidToken_returns500() {
        given()
                .queryParam("accessToken", "invalid.token")
                .queryParam("clientId", config("test.client.id"))
                .queryParam("clientSecret", "wrong.secret")
                .get("/keycloak/introspection-cache/introspector/none")
                .then()
                .statusCode(500);
    }

    @Test
    @Order(100)
    public void testCleanup_DeleteRealm() {
        KeycloakRealmLifecycle.deleteRealm(config("test.realm"));
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
