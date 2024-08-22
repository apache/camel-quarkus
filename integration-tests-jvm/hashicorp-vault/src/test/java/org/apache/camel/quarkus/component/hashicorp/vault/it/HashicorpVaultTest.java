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

import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;

import static org.apache.camel.quarkus.component.hashicorp.vault.it.HashicorpVaultRoutes.TEST_SECRET_NAME;
import static org.apache.camel.quarkus.component.hashicorp.vault.it.HashicorpVaultRoutes.TEST_SECRET_PATH;
import static org.hamcrest.Matchers.is;

@QuarkusTest
@WithTestResource(HashicorpVaultTestResource.class)
class HashicorpVaultTest {
    @Test
    void secretCRUD() {
        String secretValue = "2s3cr3t";

        RestAssured.given()
                .queryParam("key", TEST_SECRET_NAME)
                .queryParam("value", secretValue)
                .post("/hashicorp-vault/secret")
                .then()
                .statusCode(201);

        RestAssured.given()
                .queryParam("key", TEST_SECRET_NAME)
                .get("/hashicorp-vault/secret")
                .then()
                .statusCode(200)
                .body(is(secretValue));

        RestAssured.given()
                .get("/hashicorp-vault/secret/placeholder")
                .then()
                .statusCode(200)
                .body(is(secretValue));

        RestAssured.given()
                .get("/hashicorp-vault/secret/list/all")
                .then()
                .statusCode(200)
                .body(is(TEST_SECRET_PATH));

        RestAssured.given()
                .delete("/hashicorp-vault/secret")
                .then()
                .statusCode(204);

        RestAssured.given()
                .queryParam("key", TEST_SECRET_NAME)
                .get("/hashicorp-vault/secret")
                .then()
                .statusCode(404);
    }
}
