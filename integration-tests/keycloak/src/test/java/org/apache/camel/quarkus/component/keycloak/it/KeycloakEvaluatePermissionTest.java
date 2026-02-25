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
import java.util.Set;
import java.util.UUID;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.authorization.Logic;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.keycloak.representations.idm.authorization.ResourcePermissionRepresentation;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.representations.idm.authorization.ScopeRepresentation;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@QuarkusTest
@QuarkusTestResource(KeycloakTestResource.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class KeycloakEvaluatePermissionTest extends KeycloakTestBase {

    private static String userToken;
    private static final String RESOURCE_DOCUMENTS = "documents";
    private static final String SCOPE_READ = "read";

    @Test
    @Order(1)
    public void testSetup() {
        KeycloakRealmLifecycle.createRealmWithSmtp(config("test.realm"));

        // 1. Confidential client with Authorization Services enabled
        ClientRepresentation authzClient = new ClientRepresentation();
        authzClient.setClientId(TEST_CLIENT_ID);
        authzClient.setSecret(TEST_CLIENT_SECRET);
        authzClient.setPublicClient(false);
        authzClient.setDirectAccessGrantsEnabled(true);
        authzClient.setServiceAccountsEnabled(true);
        authzClient.setAuthorizationServicesEnabled(true);
        authzClient.setStandardFlowEnabled(false);

        given()
                .contentType(ContentType.JSON)
                .body(authzClient)
                .post("/keycloak/client/{realmName}/pojo", config("test.realm"))
                .then()
                .statusCode(anyOf(is(200), is(201)));

        // 2. Protected resource with a read scope
        ScopeRepresentation readScope = new ScopeRepresentation();
        readScope.setName(SCOPE_READ);

        ResourceRepresentation documents = new ResourceRepresentation();
        documents.setName(RESOURCE_DOCUMENTS);
        documents.setUris(Set.of("/documents/*"));
        documents.setScopes(Set.of(readScope));

        given()
                .contentType(ContentType.JSON)
                .body(documents)
                .post("/keycloak/resource/{realmName}/{clientId}/pojo",
                        config("test.realm"), TEST_CLIENT_ID)
                .then()
                .statusCode(anyOf(is(200), is(201)));

        // 3. Test user
        UserRepresentation user = new UserRepresentation();
        user.setUsername(TEST_USER_NAME);
        user.setEmail(TEST_USER_NAME + "@test.com");
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEnabled(true);

        given()
                .contentType(ContentType.JSON)
                .body(List.of(user))
                .post("/keycloak/user/{realmName}", config("test.realm"))
                .then()
                .statusCode(200);

        given()
                .queryParam("password", TEST_USER_PASSWORD)
                .queryParam("temporary", false)
                .post("/keycloak/user/{realmName}/{username}/reset-password",
                        config("test.realm"), TEST_USER_NAME)
                .then()
                .statusCode(200);

        String userId = given()
                .get("/keycloak/user/{realmName}/{username}",
                        config("test.realm"), TEST_USER_NAME)
                .then()
                .statusCode(200)
                .extract().jsonPath().getString("id");

        assertNotNull(userId, "userId must not be null after creation");

        // 4. User policy
        PolicyRepresentation userPolicy = new PolicyRepresentation();
        userPolicy.setType("user");
        userPolicy.setName("user-policy-" + UUID.randomUUID().toString().substring(0, 6));
        userPolicy.setLogic(Logic.POSITIVE);
        userPolicy.setConfig(Map.of("users", "[\"" + userId + "\"]"));

        String policyLocation = given()
                .contentType(ContentType.JSON)
                .body(userPolicy)
                .post("/keycloak/resource-policy/{realmName}/{clientId}/pojo",
                        config("test.realm"), TEST_CLIENT_ID)
                .then()
                .statusCode(anyOf(is(200), is(201)))
                .extract().header("Location");

        String policyId = policyLocation != null
                ? policyLocation.substring(policyLocation.lastIndexOf('/') + 1)
                : fetchPolicyId(TEST_CLIENT_ID, userPolicy.getName());

        // 5. Resource permission
        ResourcePermissionRepresentation permission = new ResourcePermissionRepresentation();
        permission.setName("docs-perm-" + UUID.randomUUID().toString().substring(0, 6));
        permission.setResources(Set.of(RESOURCE_DOCUMENTS));
        permission.setPolicies(Set.of(policyId));

        given()
                .contentType(ContentType.JSON)
                .body(permission)
                .post("/keycloak/resource-permission/{realmName}/{clientId}/pojo",
                        config("test.realm"), TEST_CLIENT_ID)
                .then()
                .statusCode(anyOf(is(200), is(201)));

        // 6. Obtain access token
        userToken = getAccessToken(TEST_USER_NAME, TEST_USER_PASSWORD, TEST_CLIENT_ID, TEST_CLIENT_SECRET);
        assertNotNull(userToken, "userToken must be non-null after setup");
    }

    @Test
    @Order(2)
    public void testPermissionsOnly_responseContainsRequiredFields() {
        given()
                .queryParam("clientId", TEST_CLIENT_ID)
                .queryParam("clientSecret", TEST_CLIENT_SECRET)
                .queryParam("accessToken", userToken)
                .get("/keycloak/evaluate-permission/permissions-only")
                .then()
                .statusCode(200)
                .body("permissions", notNullValue())
                .body("permissionCount", notNullValue())
                .body("granted", notNullValue());
    }

    @Test
    @Order(3)
    public void testPermissionsOnly_userGrantedAccessToDocumentsRead() {
        given()
                .queryParam("clientId", TEST_CLIENT_ID)
                .queryParam("clientSecret", TEST_CLIENT_SECRET)
                .queryParam("accessToken", userToken)
                .queryParam("resourceNames", RESOURCE_DOCUMENTS)
                .queryParam("scopes", SCOPE_READ)
                .get("/keycloak/evaluate-permission/permissions-only")
                .then()
                .statusCode(200)
                .body("granted", is(true))
                .body("permissionCount", greaterThan(0));
    }

    @Test
    @Order(4)
    public void testRptMode_responseContainsAllExpectedFields() {
        given()
                .queryParam("clientId", TEST_CLIENT_ID)
                .queryParam("clientSecret", TEST_CLIENT_SECRET)
                .queryParam("accessToken", userToken)
                .queryParam("resourceNames", RESOURCE_DOCUMENTS)
                .get("/keycloak/evaluate-permission/rpt")
                .then()
                .statusCode(200)
                .body("token", notNullValue())
                .body("tokenType", is("Bearer"))
                .body("expiresIn", notNullValue())
                .body("refreshToken", notNullValue())
                .body("refreshExpiresIn", notNullValue())
                .body("upgraded", notNullValue());
    }

    @Test
    @Order(5)
    public void testPermissionsOnly_resourceNamesWithWhitespace() {
        given()
                .queryParam("clientId", TEST_CLIENT_ID)
                .queryParam("clientSecret", TEST_CLIENT_SECRET)
                .queryParam("accessToken", userToken)
                .queryParam("resourceNames", " " + RESOURCE_DOCUMENTS + " , unknown-resource ")
                .get("/keycloak/evaluate-permission/permissions-only")
                .then()
                .statusCode(200)
                .body("granted", is(true));
    }

    @Test
    @Order(6)
    public void testPermissionsOnly_scopeOnlyNoResource() {
        given()
                .queryParam("clientId", TEST_CLIENT_ID)
                .queryParam("clientSecret", TEST_CLIENT_SECRET)
                .queryParam("accessToken", userToken)
                .queryParam("scopes", SCOPE_READ)
                .get("/keycloak/evaluate-permission/permissions-only")
                .then()
                .statusCode(200)
                .body("granted", is(true));
    }

    @Test
    @Order(7)
    public void testPermissionsOnly_withAudienceHeader() {
        given()
                .queryParam("clientId", TEST_CLIENT_ID)
                .queryParam("clientSecret", TEST_CLIENT_SECRET)
                .queryParam("accessToken", userToken)
                .queryParam("resourceNames", RESOURCE_DOCUMENTS)
                .queryParam("audience", TEST_CLIENT_ID)
                .get("/keycloak/evaluate-permission/audience")
                .then()
                .statusCode(200)
                .body("granted", is(true));
    }

    @Test
    @Order(8)
    public void testPermissionsOnly_withSubjectToken() {
        given()
                .queryParam("clientId", TEST_CLIENT_ID)
                .queryParam("clientSecret", TEST_CLIENT_SECRET)
                .queryParam("subjectToken", userToken)
                .queryParam("resourceNames", RESOURCE_DOCUMENTS)
                .get("/keycloak/evaluate-permission/subject-token")
                .then()
                .statusCode(200)
                .body("granted", is(true));
    }

    @Test
    @Order(9)
    public void testEvaluatePermission_usernamePasswordAuth_granted() {
        given()
                .queryParam("clientId", TEST_CLIENT_ID)
                .queryParam("clientSecret", TEST_CLIENT_SECRET)
                .queryParam("username", TEST_USER_NAME)
                .queryParam("password", TEST_USER_PASSWORD)
                .queryParam("resourceNames", RESOURCE_DOCUMENTS)
                .get("/keycloak/evaluate-permission/username-password")
                .then()
                .statusCode(200)
                .body("granted", is(true));
    }

    @Test
    @Order(10)
    public void testEvaluatePermission_missingClientSecret_returns500WithValidationMessage() {
        given()
                .queryParam("clientId", TEST_CLIENT_ID)
                .queryParam("clientSecret", "")
                .queryParam("accessToken", userToken)
                .get("/keycloak/evaluate-permission/permissions-only")
                .then()
                .statusCode(500)
                .body(containsString("Client secret must be specified"));
    }

    @Test
    @Order(11)
    public void testEvaluatePermission_invalidToken_returns500WithAuthError() {
        given()
                .queryParam("clientId", TEST_CLIENT_ID)
                .queryParam("clientSecret", TEST_CLIENT_SECRET)
                .queryParam("accessToken", "invalid.token")
                .get("/keycloak/evaluate-permission/permissions-only")
                .then()
                .statusCode(500)
                .body(containsString("401"));
    }

    @Test
    @Order(100)
    public void testCleanup_DeleteRealm() {
        KeycloakRealmLifecycle.deleteRealm(config("test.realm"));
    }

    private String fetchPolicyId(String clientId, String policyName) {
        return given()
                .get("/keycloak/resource-policy/{realmName}/{clientId}",
                        config("test.realm"), clientId)
                .then()
                .statusCode(200)
                .extract().jsonPath()
                .param("name", policyName)
                .getString("find { it.name == name }.id");
    }
}
