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
package org.apache.camel.quarkus.component.jta.it;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.h2.H2DatabaseTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.is;

@QuarkusTest
@QuarkusTestResource(H2DatabaseTestResource.class)
@QuarkusTestResource(ActiveMQXATestResource.class)
class JtaTest {

    @Test
    public void testNoTx() {
        final String msg = java.util.UUID.randomUUID().toString().replace("-", "");

        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(msg)
                .post("/jta/required")
                .then()
                .statusCode(201)
                .body(is("required"));

        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(msg)
                .post("/jta/requires_new")
                .then()
                .statusCode(201)
                .body(is("requires_new"));

        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(msg)
                .post("/jta/mandatory")
                .then()
                .statusCode(201)
                .body(is("Policy 'PROPAGATION_MANDATORY' is configured but no active transaction was found!"));

        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(msg)
                .post("/jta/never")
                .then()
                .statusCode(201)
                .body(is("never"));

        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(msg)
                .post("/jta/supports")
                .then()
                .statusCode(201)
                .body(is("supports"));

        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(msg)
                .post("/jta/not_supported")
                .then()
                .statusCode(201)
                .body(is("not_supported"));
    }

    @Test
    public void testInTx() {
        final String msg = java.util.UUID.randomUUID().toString().replace("-", "");

        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(msg)
                .post("/jta/in_tx/required")
                .then()
                .statusCode(201)
                .body(is("required"));

        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(msg)
                .post("/jta/in_tx/requires_new")
                .then()
                .statusCode(201)
                .body(is("requires_new"));

        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(msg)
                .post("/jta/in_tx/mandatory")
                .then()
                .statusCode(201)
                .body(is("mandatory"));

        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(msg)
                .post("/jta/in_tx/never")
                .then()
                .statusCode(201)
                .body(is("Policy 'PROPAGATION_NEVER' is configured but an active transaction was found!"));

        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(msg)
                .post("/jta/in_tx/supports")
                .then()
                .statusCode(201)
                .body(is("supports"));

        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(msg)
                .post("/jta/in_tx/not_supported")
                .then()
                .statusCode(201)
                .body(is("not_supported"));
    }

    @Test
    public void testJdbcInTx() {
        testTx("/jta/jdbc");
    }

    @Test
    public void testSqlInTx() {
        testTx("/jta/sqltx");
    }

    private void testTx(String url) {
        final String msg = java.util.UUID.randomUUID().toString().replace("-", "");

        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(msg)
                .post(url)
                .then()
                .statusCode(201)
                .body(is(msg + " added"));

        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body("fail")
                .post(url)
                .then()
                .statusCode(500);

        RestAssured.given()
                .contentType(ContentType.TEXT)
                .get("/jta/mock")
                .then()
                .statusCode(200)
                .body(is("empty"))
                .log();
    }
}
