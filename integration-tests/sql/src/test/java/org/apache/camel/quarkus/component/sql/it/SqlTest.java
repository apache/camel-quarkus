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
package org.apache.camel.quarkus.component.sql.it;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import org.apache.camel.component.sql.SqlConstants;
import org.apache.camel.util.CollectionHelper;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class SqlTest {

    //@Test
    public void testSqlComponent() {
        // Create Camel species
        RestAssured.given()
                .contentType(ContentType.JSON)
                .queryParam("table", "camel")
                .body(CollectionHelper.mapOf("species", "Dromedarius"))
                .post("/sql/insert")
                .then()
                .statusCode(201);

        // Retrieve camel species as map
        RestAssured.get("/sql/get/Dromedarius")
                .then()
                .statusCode(200)
                .body(is("[{ID=1, SPECIES=Dromedarius}]"));

        // Retrieve camel species as list
        RestAssured.get("/sql/get/Dromedarius/list")
                .then()
                .statusCode(200)
                .body(is("Dromedarius 1"));

        // Retrieve camel species as type
        RestAssured.get("/sql/get/Dromedarius/list/type")
                .then()
                .statusCode(200)
                .body(is("Dromedarius 1"));
    }

    //@Test
    public void testSqlStoredComponent() {
        // Invoke ADD_NUMS stored procedure
        RestAssured.given()
                .queryParam("numA", 10)
                .queryParam("numB", 5)
                .get("/sql/storedproc")
                .then()
                .statusCode(200)
                .body(is("15"));
    }

    //@Test
    public void testConsumer() throws InterruptedException {
        testConsumer(1, "consumerRoute");
    }

    //@Test
    public void testClasspathConsumer() throws InterruptedException {
        testConsumer(2, "consumerClasspathRoute");
    }

    //@Test
    public void testFileConsumer() throws InterruptedException {
        testConsumer(3, "consumerFileRoute");
    }

    private void testConsumer(int id, String routeId) throws InterruptedException {
        route(routeId, "start", "Started");

        Map project = CollectionHelper.mapOf("ID", id, "PROJECT", routeId, "LICENSE", "222", "PROCESSED", false);
        Map updatedProject = CollectionHelper.mapOf("ID", id, "PROJECT", routeId, "LICENSE", "XXX", "PROCESSED", false);

        postMapWithParam("/sql/insert",
                "table", "projects",
                project)
                        .statusCode(201);

        //wait for the record to be caught
        await().atMost(5, TimeUnit.SECONDS).until(() -> (Iterable<Object>) RestAssured
                .get("/sql/get/results/" + routeId).then().extract().as(List.class),
                both(iterableWithSize(1)).and(contains(project)));

        //update
        postMapWithParam("/sql/update",
                "table", "projects",
                updatedProject)
                        .statusCode(201);

        //wait for the record to be caught
        await().atMost(5, TimeUnit.SECONDS).until(() -> (Iterable<Object>) RestAssured
                .get("/sql/get/results/" + routeId).then().extract().as(List.class),
                both(iterableWithSize(1)).and(contains(updatedProject)));

        route(routeId, "stop", "Stopped");
    }

    //@Test
    public void testTransacted() throws InterruptedException {

        postMap("/sql/toDirect/transacted", CollectionHelper.mapOf(SqlConstants.SQL_QUERY,
                "insert into projects values (5, 'Transacted', 'ASF', false)",
                "rollback", false))
                        .statusCode(204);

        postMap("/sql/toDirect/transacted", CollectionHelper.mapOf(SqlConstants.SQL_QUERY,
                "select * from projects where project = 'Transacted'"))
                        .statusCode(200)
                        .body("size()", is(1));

        postMap("/sql/toDirect/transacted", CollectionHelper.mapOf(SqlConstants.SQL_QUERY,
                "insert into projects values (6, 'Transacted', 'ASF', false)",
                "rollback", true))
                        .statusCode(200)
                        .body(is("java.lang.Exception:forced Exception"));

        postMap("/sql/toDirect/transacted",
                CollectionHelper.mapOf(SqlConstants.SQL_QUERY, "select * from projects where project = 'Transacted'"))
                        .statusCode(200)
                        .body("size()", is(1));
    }

    //@Test
    public void testDefaultErrorCode() throws InterruptedException {
        postMap("/sql/toDirect/transacted", CollectionHelper.mapOf(SqlConstants.SQL_QUERY, "select * from NOT_EXIST order id"))
                .statusCode(200)
                .body(startsWith("org.springframework.jdbc.BadSqlGrammarException"));
    }

    //@Test
    public void testIdempotentRepository() {
        // add value with key 1
        postMapWithParam("/sql/toDirect/idempotent",
                "body", "one",
                CollectionHelper.mapOf("messageId", "1"))
                        .statusCode(200);

        // add value with key 2
        postMapWithParam("/sql/toDirect/idempotent",
                "body", "two",
                CollectionHelper.mapOf("messageId", "2"))
                        .statusCode(200);

        // add same value with key 3
        postMapWithParam("/sql/toDirect/idempotent",
                "body", "three",
                CollectionHelper.mapOf("messageId", "3"))
                        .statusCode(200);

        // add another value with key 1 -- this one is supposed to be skipped
        postMapWithParam("/sql/toDirect/idempotent",
                "body", "four",
                CollectionHelper.mapOf("messageId", "1"))
                        .statusCode(200);

        // get all values from the result map
        await().atMost(5, TimeUnit.SECONDS).until(() -> (Iterable<? extends String>) RestAssured
                .get("/sql/get/results/idempotentRoute").then().extract().as(List.class),
                containsInAnyOrder("one", "two", "three"));
    }

    //@Test
    public void testAggregationRepository() {
        postMapWithParam("/sql/toDirect/aggregation", "body", "A", CollectionHelper.mapOf("messageId", "123"))
                .statusCode(200);

        postMapWithParam("/sql/toDirect/aggregation", "body", "B", CollectionHelper.mapOf("messageId", "123"))
                .statusCode(200);

        postMapWithParam("/sql/toDirect/aggregation", "body", "C", CollectionHelper.mapOf("messageId", "123"))
                .statusCode(200);

        postMapWithParam("/sql/toDirect/aggregation", "body", "D", CollectionHelper.mapOf("messageId", "123"))
                .statusCode(200);

        // get all values from the result map
        await().atMost(5, TimeUnit.SECONDS).until(() -> (Iterable<? extends String>) RestAssured
                .get("/sql/get/results/aggregationRoute").then().extract().as(List.class),
                containsInAnyOrder("ABCD"));
    }

    private ValidatableResponse postMap(String toUrl, Map<String, String> body) {
        return postMapWithParam(toUrl, null, null, body);
    }

    private ValidatableResponse postMapWithParam(String toUrl, String param, String paramValue, Map<String, String> body) {
        RequestSpecification rs = RestAssured.given()
                .contentType(ContentType.JSON);

        if (param != null) {
            rs = rs.queryParam(param, paramValue);
        }

        return rs.body(body)
                .post(toUrl)
                .then();
    }

    private void route(String routeId, String operation, String expectedOutput) {
        RestAssured.given()
                .get("/sql/route/" + routeId + "/" + operation)
                .then().statusCode(204);

        if (expectedOutput != null) {
            await().atMost(5, TimeUnit.SECONDS).until(() -> RestAssured
                    .get("/sql/route/" + routeId + "/status")
                    .then()
                    .extract().asString(), equalTo(expectedOutput));
        }
    }
}
