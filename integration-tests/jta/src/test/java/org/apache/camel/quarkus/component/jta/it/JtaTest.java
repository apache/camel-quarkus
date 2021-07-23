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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.ResourceArg;
import io.quarkus.test.h2.H2DatabaseTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.apache.camel.quarkus.test.support.activemq.ActiveMQTestResource;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hamcrest.Matchers.is;

@QuarkusTest
@QuarkusTestResource(H2DatabaseTestResource.class)
@QuarkusTestResource(initArgs = {
        @ResourceArg(name = "modules", value = "quarkus.artemis")
}, value = ActiveMQTestResource.class)
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

    @ParameterizedTest
    @ValueSource(strings = { "jdbc", "jdbcRollback", "sqltx", "sqltxRollback" })
    public void testTx(String endpoint) throws SQLException {
        final String msg = endpoint + ":" + UUID.randomUUID().toString().replace("-", "");

        assertDBRows(endpoint);
        RestAssured.get("/jta/mock/" + endpoint + "/0/1000")
                .then()
                .statusCode(200)
                .body(Matchers.is(""));

        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(msg)
                .post("/jta/route/" + endpoint)
                .then()
                .statusCode(201)
                .body(is(msg + " added"));

        // One row inserted
        assertDBRows(endpoint, msg);
        RestAssured.get("/jta/mock/" + endpoint + "/1/15000")
                .then()
                .statusCode(200)
                .body(Matchers.is(msg));

        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body("rollback")
                .post("/jta/route/" + endpoint)
                .then()
                .statusCode(500);

        // Should still have the original row as the other insert attempt was rolled back
        assertDBRows(endpoint, msg);
        RestAssured.get("/jta/mock/" + endpoint + "/1/15000")
                .then()
                .statusCode(200)
                .body(Matchers.is(msg));
    }

    private void assertDBRows(String source, String... expectedMessages) throws SQLException {
        try (Connection connection = DriverManager.getConnection("jdbc:h2:tcp://localhost/mem:test")) {
            try (Statement statement = connection.createStatement()) {
                try (ResultSet resultSet = statement
                        .executeQuery("SELECT message FROM example WHERE origin = '" + source + "' ORDER BY id")) {
                    int i = 0;
                    for (; i < expectedMessages.length; i++) {
                        String expectedMessage = expectedMessages[i];
                        if (resultSet.next()) {
                            Assertions.assertEquals(expectedMessage, resultSet.getString(1));
                        } else {
                            Assertions
                                    .fail("Expected message '" + expectedMessage + "' for origin '" + source + "' at index " + i
                                            + "; found: end of list");
                        }
                    }
                    if (resultSet.next()) {
                        Assertions.fail("Expected end of list '" + source + "' at index " + i + "; found message: "
                                + resultSet.getString(1));
                    }
                }
            }
        }
    }
}
