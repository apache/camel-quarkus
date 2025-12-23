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
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;
import org.keycloak.representations.idm.RealmRepresentation;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

@QuarkusTest
@QuarkusTestResource(KeycloakTestResource.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class KeycloakRealmTest extends KeycloakTestBase {

    @Test
    @Order(1)
    public void loadComponentKeycloak() {
        RestAssured.get("/keycloak/load/component/keycloak")
                .then()
                .statusCode(200);
    }

    @Test
    @Order(2)
    public void testSetup_CreateRealm() {
        KeycloakRealmLifecycle.createRealmWithSmtp(TEST_REALM_NAME);
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

    @Test
    @Order(5)
    public void testUpdateRealm() {
        // First get the realm
        RealmRepresentation realm = given()
                .when()
                .get("/keycloak/realm/{realmName}", TEST_REALM_NAME)
                .then()
                .statusCode(200)
                .extract()
                .as(RealmRepresentation.class);

        // Update the realm's display name
        realm.setDisplayName("Updated Test Realm Display Name");
        realm.setDisplayNameHtml("<h1>Updated Test Realm</h1>");

        given()
                .contentType(ContentType.JSON)
                .body(realm)
                .when()
                .put("/keycloak/realm/{realmName}", TEST_REALM_NAME)
                .then()
                .statusCode(200)
                .body(is("Realm updated successfully"));

        // Verify the update
        RealmRepresentation updatedRealm = given()
                .when()
                .get("/keycloak/realm/{realmName}", TEST_REALM_NAME)
                .then()
                .statusCode(200)
                .extract()
                .as(RealmRepresentation.class);

        assertThat(updatedRealm.getDisplayName(), is("Updated Test Realm Display Name"));
        assertThat(updatedRealm.getDisplayNameHtml(), is("<h1>Updated Test Realm</h1>"));
    }

    @Test
    @Order(99)
    public void testCleanup_DeleteRealm() {
        KeycloakRealmLifecycle.deleteRealm(TEST_REALM_NAME);
        KeycloakRealmLifecycle.verifyRealmDeleted(TEST_REALM_NAME);
    }
}
