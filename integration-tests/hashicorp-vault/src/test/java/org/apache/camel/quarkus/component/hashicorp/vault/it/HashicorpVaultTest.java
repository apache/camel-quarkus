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
package org.apache.camel.quarkus.component.hashicorp.vault.it;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;

import static org.apache.camel.quarkus.component.hashicorp.vault.it.HashicorpVaultRoutes.TEST_SECRET_NAME;
import static org.apache.camel.quarkus.component.hashicorp.vault.it.HashicorpVaultRoutes.TEST_SECRET_PATH;
import static org.apache.camel.quarkus.component.hashicorp.vault.it.HashicorpVaultRoutes.TEST_VERSIONED_SECRET_PATH;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

@QuarkusTest
@QuarkusTestResource(HashicorpVaultTestResource.class)
class HashicorpVaultTest {
    public static final String SECRET_VALUE = "2s3cr3t";

    @Test
    void secretCRUD() {
        RestAssured.given()
                .queryParam("endpointUri", "direct:createSecret")
                .queryParam("key", TEST_SECRET_NAME)
                .queryParam("value", SECRET_VALUE)
                .post("/hashicorp-vault/secret")
                .then()
                .statusCode(201);

        RestAssured.given()
                .queryParam("secretPath", TEST_SECRET_PATH)
                .get("/hashicorp-vault/secret")
                .then()
                .statusCode(200)
                .body(TEST_SECRET_NAME, is(SECRET_VALUE));

        RestAssured.given()
                .queryParam("endpointUri", "direct:getSecretWithCustomVaultTemplate")
                .queryParam("secretPath", TEST_SECRET_PATH)
                .get("/hashicorp-vault/secret")
                .then()
                .statusCode(200)
                .body(TEST_SECRET_NAME, is(SECRET_VALUE));

        RestAssured.given()
                .queryParam("secretPath", TEST_SECRET_PATH)
                .get("/hashicorp-vault/secret/placeholder")
                .then()
                .statusCode(200)
                .body(is(SECRET_VALUE));

        RestAssured.given()
                .get("/hashicorp-vault/secret/list/all")
                .then()
                .statusCode(200)
                .body(containsString(TEST_SECRET_PATH));

        RestAssured.given()
                .delete("/hashicorp-vault/secret")
                .then()
                .statusCode(204);

        RestAssured.given()
                .queryParam("secretPath", TEST_SECRET_PATH)
                .queryParam("key", TEST_SECRET_NAME)
                .get("/hashicorp-vault/secret")
                .then()
                .statusCode(404);
    }

    @Test
    void secretPojo() {
        String secretA = "Test Secret A";
        String secretB = "Test Secret B";
        String secretC = "Test Secret C";

        RestAssured.given()
                .queryParam("endpointUri", "direct:createSecret")
                .queryParam("secretA", secretA)
                .queryParam("secretB", secretB)
                .queryParam("secretC", secretC)
                .post("/hashicorp-vault/secret/pojo")
                .then()
                .statusCode(201);

        RestAssured.given()
                .queryParam("secretPath", TEST_SECRET_PATH)
                .get("/hashicorp-vault/secret")
                .then()
                .statusCode(200)
                .body(
                        "secretA", is(secretA),
                        "secretB", is(secretB),
                        "secretC", is(secretC));
    }

    @Test
    void versionedSecret() {
        RestAssured.given()
                .queryParam("endpointUri", "direct:createVersionedSecret")
                .queryParam("key", TEST_SECRET_NAME)
                .queryParam("value", SECRET_VALUE)
                .post("/hashicorp-vault/secret")
                .then()
                .statusCode(201);

        String newSecretVersion = SECRET_VALUE + " version 2";
        RestAssured.given()
                .queryParam("endpointUri", "direct:createVersionedSecret")
                .queryParam("key", TEST_SECRET_NAME)
                .queryParam("value", newSecretVersion)
                .post("/hashicorp-vault/secret")
                .then()
                .statusCode(201);

        RestAssured.given()
                .queryParam("secretPath", TEST_VERSIONED_SECRET_PATH)
                .queryParam("version", "1")
                .get("/hashicorp-vault/secret")
                .then()
                .statusCode(200)
                .body(TEST_SECRET_NAME, is(SECRET_VALUE));

        RestAssured.given()
                .queryParam("secretPath", TEST_VERSIONED_SECRET_PATH)
                .queryParam("version", "2")
                .get("/hashicorp-vault/secret")
                .then()
                .statusCode(200)
                .body(TEST_SECRET_NAME, is(newSecretVersion));

        RestAssured.given()
                .queryParam("secretPath", TEST_VERSIONED_SECRET_PATH)
                .queryParam("version", "1")
                .get("/hashicorp-vault/secret/placeholder")
                .then()
                .statusCode(200)
                .body(is(SECRET_VALUE));

        RestAssured.given()
                .queryParam("secretPath", TEST_VERSIONED_SECRET_PATH)
                .queryParam("version", "2")
                .get("/hashicorp-vault/secret/placeholder")
                .then()
                .statusCode(200)
                .body(is(newSecretVersion));
    }
}
