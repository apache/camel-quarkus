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
package org.apache.camel.quarkus.component.aws2.ddb.it;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.apache.camel.component.aws2.ddb.Ddb2Constants;
import org.apache.camel.component.aws2.ddb.Ddb2Operations;
import org.apache.camel.quarkus.test.support.aws2.Aws2TestResource;
import org.awaitility.Awaitility;
import org.hamcrest.Matchers;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.dynamodb.model.TableStatus;

import static org.apache.camel.quarkus.component.aws2.ddb.it.Aws2DdbResource.Table;

@QuarkusTest
@QuarkusTestResource(Aws2TestResource.class)
class Aws2DdbTest {

    private static final Logger LOG = Logger.getLogger(Aws2DdbTest.class);

    @Test
    public void crud() {
        final String key = "key" + UUID.randomUUID().toString().replace("-", "");
        final String msg = "val" + UUID.randomUUID().toString().replace("-", "");

        /* Ensure initially empty */
        RestAssured.get("/aws2-ddb/item/" + key)
                .then()
                .statusCode(204);

        /* Put */
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(msg)
                .queryParam("table", Table.basic)
                .post("/aws2-ddb/item/" + key)
                .then()
                .statusCode(201);

        Awaitility.await().pollInterval(1, TimeUnit.SECONDS).atMost(120, TimeUnit.SECONDS).until(
                () -> {
                    ExtractableResponse<Response> result = RestAssured.get("/aws2-ddb/item/" + key)
                            .then()
                            .statusCode(Matchers.anyOf(Matchers.is(200), Matchers.is(204)))
                            .extract();
                    LOG.info("Expecting " + msg + " got " + result.statusCode() + ": " + result.body().asString());
                    return result.body().asString();
                },
                Matchers.is(msg));

        /* Update */
        final String newMsg = "newVal" + UUID.randomUUID().toString().replace("-", "");
        RestAssured.given()
                .queryParam("table", Table.basic)
                .body(newMsg)
                .put("/aws2-ddb/item/" + key)
                .then()
                .statusCode(204);
        Awaitility.await().pollInterval(1, TimeUnit.SECONDS).atMost(120, TimeUnit.SECONDS).until(
                () -> {
                    ExtractableResponse<Response> result = RestAssured.get("/aws2-ddb/item/" + key)
                            .then()
                            .statusCode(Matchers.anyOf(Matchers.is(200), Matchers.is(204)))
                            .extract();
                    LOG.info("Expecting " + newMsg + " got " + result.statusCode() + ": " + result.body().asString());
                    return result.body().asString();
                },
                Matchers.is(newMsg));

        /* Delete */
        RestAssured.given()
                .queryParam("table", Table.basic)
                .delete("/aws2-ddb/item/" + key)
                .then()
                .statusCode(204);

        Awaitility.await().pollInterval(1, TimeUnit.SECONDS).atMost(120, TimeUnit.SECONDS).until(
                () -> {
                    ExtractableResponse<Response> result = RestAssured.get("/aws2-ddb/item/" + key)
                            .then()
                            .extract();
                    LOG.info("Expecting " + msg + " got " + result.statusCode() + ": " + result.body().asString());
                    return result.statusCode();
                },
                Matchers.is(204));

    }

    @Test
    public void operations() {

        final String key1 = "key-1-" + UUID.randomUUID().toString().replace("-", "");
        final String msg1 = "val-1-" + UUID.randomUUID().toString().replace("-", "");
        final String key2 = "key-2-" + UUID.randomUUID().toString().replace("-", "");
        final String msg2 = "val-2-" + UUID.randomUUID().toString().replace("-", "");
        final String key3 = "key-3-" + UUID.randomUUID().toString().replace("-", "");
        final String msg3 = "val-3-" + UUID.randomUUID().toString().replace("-", "");

        RestAssured.given()
                .contentType(ContentType.TEXT)
                .queryParam("table", Table.operations)
                .body(msg1)
                .post("/aws2-ddb/item/" + key1)
                .then()
                .statusCode(201);

        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(msg2)
                .queryParam("table", Table.operations)
                .post("/aws2-ddb/item/" + key2)
                .then()
                .statusCode(201);

        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(msg3)
                .queryParam("table", Table.operations)
                .post("/aws2-ddb/item/" + key3)
                .then()
                .statusCode(201);

        /* Batch items */
        Awaitility.await().pollInterval(1, TimeUnit.SECONDS).atMost(120, TimeUnit.SECONDS).until(
                () -> {

                    ExtractableResponse<Response> result = RestAssured.given()
                            .contentType(ContentType.JSON)
                            .body(Stream.of(key1, key2).collect(Collectors.toList()))
                            .post("/aws2-ddb/batchItems")
                            .then()
                            .statusCode(200)
                            .extract();

                    LOG.info("Expecting 2 items, got " + result.statusCode() + ": " + result.body().asString());

                    return result.jsonPath().getMap("$");
                },
                /* Both inserted pairs have to be returned */
                map -> map.size() == 2
                        && msg1.equals(map.get(key1))
                        && msg2.equals(map.get(key2)));

        /* Query */
        Awaitility.await().pollInterval(1, TimeUnit.SECONDS).atMost(120, TimeUnit.SECONDS).until(
                () -> {
                    ExtractableResponse<Response> result = RestAssured.given()
                            .contentType(ContentType.JSON)
                            .body(key3)
                            .post("/aws2-ddb/query")
                            .then()
                            .statusCode(200)
                            .extract();

                    LOG.info("Expecting 1 item, got " + result.statusCode() + ": " + result.body().asString());

                    return result.jsonPath().getMap("$");
                },
                map -> map.size() == 1
                        && msg3.equals(map.get(key3)));

        /* Scan */
        Awaitility.await().pollInterval(1, TimeUnit.SECONDS).atMost(120, TimeUnit.SECONDS).until(
                () -> {
                    ExtractableResponse<Response> result = RestAssured.get("/aws2-ddb/scan")
                            .then()
                            .statusCode(200)
                            .extract();

                    LOG.info("Expecting 3 items, got " + result.statusCode() + ": " + result.body().asString());

                    return result.jsonPath().getMap("$");
                },
                map -> map.size() == 3
                        && msg1.equals(map.get(key1))
                        && msg2.equals(map.get(key2))
                        && msg3.equals(map.get(key3)));

        /* Describe table */
        Awaitility.await().pollInterval(1, TimeUnit.SECONDS).atMost(120, TimeUnit.SECONDS).until(
                () -> {
                    ExtractableResponse<Response> result = RestAssured.given()
                            .contentType(ContentType.JSON)
                            .body(Ddb2Operations.DescribeTable)
                            .post("/aws2-ddb/operation")
                            .then()
                            .statusCode(200)
                            .extract();

                    LOG.info("Expecting table description, got " + result.statusCode() + ": " + result.body().asString());

                    return result.jsonPath().getMap("$");
                },
                map -> map.size() == 8
                        && map.containsKey(Ddb2Constants.CREATION_DATE)
                        && map.containsKey(Ddb2Constants.READ_CAPACITY)
                        && TableStatus.ACTIVE.name().equals(map.get(Ddb2Constants.TABLE_STATUS))
                        && map.containsKey(Ddb2Constants.WRITE_CAPACITY)
                        && map.containsKey(Ddb2Constants.TABLE_SIZE)
                        && map.containsKey(Ddb2Constants.KEY_SCHEMA)
                        && map.containsKey(Ddb2Constants.ITEM_COUNT)
                        && Table.operations == Table.valueOf((String) map.get(Ddb2Constants.TABLE_NAME)));

        /* Update table */
        Awaitility.await().pollInterval(1, TimeUnit.SECONDS).atMost(120, TimeUnit.SECONDS).until(
                () -> {
                    ExtractableResponse<Response> result = RestAssured.given()
                            .contentType(ContentType.JSON)
                            .body(5)
                            .post("/aws2-ddb/updateTable")
                            .then()
                            .extract();

                    LOG.info("Expecting table update, got " + result.statusCode() + ": " + result.body().asString());

                    return result.statusCode();
                },
                Matchers.is(201));

        /* Delete table (also verify that update from previous step took effect) */
        Awaitility.await().pollInterval(1, TimeUnit.SECONDS).atMost(120, TimeUnit.SECONDS).until(
                () -> {
                    ExtractableResponse<Response> result = RestAssured.given()
                            .contentType(ContentType.JSON)
                            .body(Ddb2Operations.DeleteTable)
                            .post("/aws2-ddb/operation")
                            .then()
                            .statusCode(200)
                            .extract();

                    LOG.info("Expecting table deletion, got " + result.statusCode() + ": " + result.body().asString());

                    return result.jsonPath().getMap("$");
                },
                map -> map.size() == 7
                        && map.containsKey(Ddb2Constants.CREATION_DATE)
                        && map.containsKey(Ddb2Constants.TABLE_STATUS)
                        && map.containsKey(Ddb2Constants.TABLE_SIZE)
                        && map.containsKey(Ddb2Constants.KEY_SCHEMA)
                        && map.containsKey(Ddb2Constants.ITEM_COUNT)
                        && Table.operations == Table.valueOf((String) map.get(Ddb2Constants.TABLE_NAME))
                        //previous update changed throughput capacity from 10 to 5
                        && ((Map) map.get(Ddb2Constants.PROVISIONED_THROUGHPUT)).size() == 2
                        && ((Map) map.get(Ddb2Constants.PROVISIONED_THROUGHPUT)).get(Ddb2Constants.READ_CAPACITY).equals(5)
                        && ((Map) map.get(Ddb2Constants.PROVISIONED_THROUGHPUT)).get(Ddb2Constants.WRITE_CAPACITY).equals(5));

        /* Verify delete with describe table */
        Awaitility.await().pollInterval(1, TimeUnit.SECONDS).atMost(120, TimeUnit.SECONDS).until(
                () -> {
                    ExtractableResponse<Response> result = RestAssured.given()
                            .contentType(ContentType.JSON)
                            .body(Ddb2Operations.DescribeTable)
                            .post("/aws2-ddb/operation")
                            .then()
                            .statusCode(200)
                            .extract();

                    LOG.info("Expecting table description of non-existing table, got " + result.statusCode() + ": "
                            + result.body().asString());

                    return result.jsonPath().getMap("$");
                },
                map -> map.isEmpty());
    }

}
