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

import java.util.HashMap;
import java.util.Map;

import io.restassured.http.ContentType;
import org.keycloak.representations.idm.RealmRepresentation;

import static io.restassured.RestAssured.given;

/**
 * Helper class to manage Keycloak realm lifecycle for tests.
 * Provides methods to create and delete test realms with SMTP configuration.
 */
public class KeycloakRealmLifecycle {

    /**
     * Creates a test realm with the given name and configures SMTP settings.
     *
     * @param realmName the name of the realm to create
     */
    public static void createRealmWithSmtp(String realmName) {
        // Create the realm
        given()
                .when()
                .post("/keycloak/realm/{realmName}", realmName)
                .then()
                .statusCode(200);

        // Get the realm
        RealmRepresentation realm = given()
                .when()
                .get("/keycloak/realm/{realmName}", realmName)
                .then()
                .statusCode(200)
                .extract()
                .as(RealmRepresentation.class);

        // Configure SMTP settings to use GreenMail
        Map<String, String> smtpServer = new HashMap<>();
        smtpServer.put("host", "greenmail");
        smtpServer.put("port", "3025");
        smtpServer.put("from", "keycloak@test.local");
        smtpServer.put("fromDisplayName", "Keycloak Test");
        smtpServer.put("replyTo", "noreply@test.local");
        smtpServer.put("ssl", "false");
        smtpServer.put("starttls", "false");
        smtpServer.put("auth", "false");

        realm.setSmtpServer(smtpServer);

        // Update the realm with SMTP configuration
        given()
                .contentType(ContentType.JSON)
                .body(realm)
                .when()
                .put("/keycloak/realm/{realmName}", realmName)
                .then()
                .statusCode(200);
    }

    /**
     * Deletes a test realm.
     *
     * @param realmName the name of the realm to delete
     */
    public static void deleteRealm(String realmName) {
        given()
                .when()
                .delete("/keycloak/realm/{realmName}", realmName)
                .then()
                .statusCode(200);
    }

    /**
     * Verifies that a realm has been deleted.
     *
     * @param realmName the name of the realm to verify
     */
    public static void verifyRealmDeleted(String realmName) {
        // Verify that the realm was actually deleted by expecting a failure
        given()
                .when()
                .get("/keycloak/realm/{realmName}", realmName)
                .then()
                .statusCode(500); // Should fail since realm no longer exists
    }
}
