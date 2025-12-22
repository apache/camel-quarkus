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
import org.keycloak.representations.idm.IdentityProviderRepresentation;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

@QuarkusTest
@QuarkusTestResource(KeycloakTestResource.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class KeycloakIdentityProviderTest extends KeycloakTestBase {

    @Test
    @Order(1)
    public void testSetup_CreateRealm() {
        KeycloakRealmLifecycle.createRealmWithSmtp(TEST_REALM_NAME);
    }

    @Test
    @Order(2)
    public void testCreateIdentityProvider() {
        IdentityProviderRepresentation idp = new IdentityProviderRepresentation();
        idp.setAlias(TEST_IDP_ALIAS);
        idp.setProviderId("oidc");
        idp.setEnabled(true);
        idp.setDisplayName("Test Identity Provider");

        given()
                .contentType(ContentType.JSON)
                .body(idp)
                .when()
                .post("/keycloak/identity-provider/{realmName}/pojo", TEST_REALM_NAME)
                .then()
                .statusCode(201)
                .body(is("Identity provider created successfully"));
    }

    @Test
    @Order(3)
    public void testListIdentityProviders() {
        List<IdentityProviderRepresentation> idps = given()
                .when()
                .get("/keycloak/identity-provider/{realmName}", TEST_REALM_NAME)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract()
                .body()
                .jsonPath()
                .getList(".", IdentityProviderRepresentation.class);

        assertThat(idps, notNullValue());
        assertThat(idps.size(), greaterThanOrEqualTo(1));

        boolean foundTestIdp = idps.stream()
                .anyMatch(i -> TEST_IDP_ALIAS.equals(i.getAlias()));
        assertThat(foundTestIdp, is(true));
    }

    @Test
    @Order(4)
    public void testGetIdentityProvider() {
        IdentityProviderRepresentation idp = given()
                .when()
                .get("/keycloak/identity-provider/{realmName}/{idpAlias}", TEST_REALM_NAME, TEST_IDP_ALIAS)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract()
                .as(IdentityProviderRepresentation.class);

        assertThat(idp, notNullValue());
        assertThat(idp.getAlias(), is(TEST_IDP_ALIAS));
        assertThat(idp.getProviderId(), is("oidc"));
        assertThat(idp.getDisplayName(), is("Test Identity Provider"));
    }

    @Test
    @Order(5)
    public void testUpdateIdentityProvider() {
        IdentityProviderRepresentation idp = given()
                .when()
                .get("/keycloak/identity-provider/{realmName}/{idpAlias}", TEST_REALM_NAME, TEST_IDP_ALIAS)
                .then()
                .statusCode(200)
                .extract()
                .as(IdentityProviderRepresentation.class);

        idp.setDisplayName("Updated Test Identity Provider");

        given()
                .contentType(ContentType.JSON)
                .body(idp)
                .when()
                .put("/keycloak/identity-provider/{realmName}/{idpAlias}", TEST_REALM_NAME, TEST_IDP_ALIAS)
                .then()
                .statusCode(200)
                .body(is("Identity provider updated successfully"));

        IdentityProviderRepresentation updatedIdp = given()
                .when()
                .get("/keycloak/identity-provider/{realmName}/{idpAlias}", TEST_REALM_NAME, TEST_IDP_ALIAS)
                .then()
                .statusCode(200)
                .extract()
                .as(IdentityProviderRepresentation.class);

        assertThat(updatedIdp.getDisplayName(), is("Updated Test Identity Provider"));
    }

    @Test
    @Order(101)
    public void testCleanupIdentityProvider() {
        given()
                .when()
                .delete("/keycloak/identity-provider/{realmName}/{idpAlias}", TEST_REALM_NAME, TEST_IDP_ALIAS)
                .then()
                .statusCode(200)
                .body(is("Identity provider deleted successfully"));
    }

    @Test
    @Order(102)
    public void testCleanup_DeleteRealm() {
        KeycloakRealmLifecycle.deleteRealm(TEST_REALM_NAME);
    }
}
