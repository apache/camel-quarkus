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
package org.apache.camel.quarkus.component.kudu.it;

import java.util.Map;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.apache.camel.quarkus.test.DisabledIfFipsMode;
import org.apache.kudu.client.KuduClient;
import org.apache.kudu.client.KuduException;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.apache.camel.quarkus.component.kudu.it.KuduRoute.KUDU_AUTHORITY_CONFIG_KEY;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTestResource(KuduTestResource.class)
@QuarkusTest
@DisabledIfFipsMode // https://github.com/apache/camel-quarkus/issues/5700
class KuduTest {
    private static final Logger LOG = Logger.getLogger(KuduTest.class);
    static KuduClient client;

    @BeforeAll
    static void setup() {
        String authority = ConfigProvider.getConfig().getValue(KUDU_AUTHORITY_CONFIG_KEY, String.class);
        client = new KuduClient.KuduClientBuilder(authority).build();
    }

    @AfterAll
    static void afterAll() {
        if (client != null) {
            try {
                client.close();
            } catch (KuduException e) {
                LOG.warn("Failed to close kudu client", e);
            }
        }
    }

    @BeforeEach
    void beforeEach() throws KuduException {
        createTable();
    }

    @AfterEach
    void afterEach() {
        if (client != null) {
            try {
                client.deleteTable(KuduRoute.TABLE_NAME);
            } catch (KuduException e) {
                throw new RuntimeException(e);
            }
        }
    }

    void createTable() throws KuduException {
        assertEquals(0, client.getTablesList().getTablesList().size());
        RestAssured.put("/kudu/createTable")
                .then()
                .statusCode(200);
        assertEquals(1, client.getTablesList().getTablesList().size());
    }

    @Test
    void kuduCrud() throws KuduException {
        // Create
        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(Map.of("id", "key1", "name", "Samuel", "age", 50))
                .put("/kudu/insert")
                .then()
                .statusCode(200);

        // Read
        RestAssured.get("/kudu/scan")
                .then()
                .statusCode(200)
                .body(
                        "[0].id", is("key1"),
                        "[0].name", is("Samuel"),
                        "[0].age", is(50));

        // Update
        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(Map.of("id", "key1", "name", "John", "age", 40))
                .patch("/kudu/update")
                .then()
                .statusCode(200);

        RestAssured.get("/kudu/scan")
                .then()
                .statusCode(200)
                .body(
                        "[0].id", is("key1"),
                        "[0].name", is("John"),
                        "[0].age", is(40));

        // Delete
        RestAssured.delete("/kudu/delete/key1")
                .then()
                .statusCode(200);

        // Confirm deletion
        RestAssured.get("/kudu/scan")
                .then()
                .statusCode(200)
                .body("$.size()", is(0));
    }

    @Test
    void upsertUpdate() {
        // Create
        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(Map.of("id", "key1", "name", "Samuel", "age", 50))
                .put("/kudu/insert")
                .then()
                .statusCode(200);

        // Read
        RestAssured.get("/kudu/scan")
                .then()
                .statusCode(200)
                .body(
                        "[0].id", is("key1"),
                        "[0].name", is("Samuel"),
                        "[0].age", is(50));

        // Upsert update of name
        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(Map.of("id", "key1", "name", "John", "age", 50))
                .patch("/kudu/upsert")
                .then()
                .statusCode(200);

        // Read update
        RestAssured.get("/kudu/scan")
                .then()
                .statusCode(200)
                .body(
                        "[0].id", is("key1"),
                        "[0].name", is("John"),
                        "[0].age", is(50));
    }

    @Test
    void upsertInsert() {
        // Upsert
        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(Map.of("id", "key1", "name", "Samuel", "age", 50))
                .put("/kudu/insert")
                .then()
                .statusCode(200);

        // Read
        RestAssured.get("/kudu/scan")
                .then()
                .statusCode(200)
                .body(
                        "[0].id", is("key1"),
                        "[0].name", is("Samuel"),
                        "[0].age", is(50));
    }

    @Test
    void scanWithSpecificColumns() {
        // Create
        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(Map.of("id", "key1", "name", "Samuel", "age", 50))
                .put("/kudu/insert")
                .then()
                .statusCode(200);

        // Read
        RestAssured.given()
                .queryParam("columnNames", "name,age")
                .get("/kudu/scan")
                .then()
                .statusCode(200)
                .body(
                        "[0].id", nullValue(),
                        "[0].age", equalTo(50),
                        "[0].name", is("Samuel"));
    }

    @Test
    void scanWithPredicate() {
        // Create
        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(Map.of("id", "key1", "name", "Samuel", "age", 50))
                .put("/kudu/insert")
                .then()
                .statusCode(200);

        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(Map.of("id", "key2", "name", "Alice", "age", 12))
                .put("/kudu/insert")
                .then()
                .statusCode(200);

        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(Map.of("id", "key3", "name", "John", "age", 40))
                .put("/kudu/insert")
                .then()
                .statusCode(200);

        // Read (finds all records with age >= 18)
        RestAssured.given()
                .queryParam("minAge", 18)
                .get("/kudu/scan/predicate")
                .then()
                .statusCode(200)
                .body(
                        "[0].id", is("key1"),
                        "[0].name", is("Samuel"),
                        "[0].age", is(50),
                        "[1].id", is("key3"),
                        "[1].name", is("John"),
                        "[1].age", is(40));
    }

    @Test
    void scanWithLimit() {
        // Create
        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(Map.of("id", "key1", "name", "Samuel", "age", 50))
                .put("/kudu/insert")
                .then()
                .statusCode(200);

        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(Map.of("id", "key2", "name", "John", "age", 40))
                .put("/kudu/insert")
                .then()
                .statusCode(200);

        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(Map.of("id", "key3", "name", "Alice", "age", 12))
                .put("/kudu/insert")
                .then()
                .statusCode(200);

        // Read (limit of 2 records)
        RestAssured.given()
                .queryParam("limit", 2)
                .get("/kudu/scan/limit")
                .then()
                .statusCode(200)
                .body(
                        "[0].id", is("key1"),
                        "[0].name", is("Samuel"),
                        "[0].age", is(50),
                        "[1].id", is("key2"),
                        "[1].name", is("John"),
                        "[1].age", is(40));
    }
}
