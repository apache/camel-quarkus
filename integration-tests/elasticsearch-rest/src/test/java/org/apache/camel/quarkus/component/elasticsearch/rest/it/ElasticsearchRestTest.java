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
package org.apache.camel.quarkus.component.elasticsearch.rest.it;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
@QuarkusTestResource(ElasticSearchTestResource.class)
class ElasticsearchRestTest {

    @AfterEach
    public void afterEach() {
        // Clean up all indexed data
        RestAssured.given()
                .queryParam("component", "elasticsearch-rest")
                .queryParam("indexName", "_all")
                .delete("/elasticsearch-rest/delete/index")
                .then()
                .statusCode(200)
                .body(is("true"));
    }

    @ParameterizedTest
    @MethodSource("componentNames")
    public void testElasticsearchBasicOperations(String component) {
        String indexName = UUID.randomUUID().toString();
        String indexKey = "test-key";
        String indexValue = "Hello Camel Quarkus ElasticSearch REST";

        // Verify the ElasticSearch server is available
        RestAssured.given()
                .queryParam("component", component)
                .get("/elasticsearch-rest/ping")
                .then()
                .statusCode(200)
                .body(is("true"));

        // Index data
        String indexId = RestAssured.given()
                .queryParam("component", component)
                .queryParam("indexName", indexName)
                .queryParam("indexKey", indexKey)
                .contentType(ContentType.TEXT)
                .body(indexValue)
                .post("/elasticsearch-rest/index")
                .then()
                .statusCode(201)
                .extract()
                .body()
                .asString();

        // Verify index exists
        RestAssured.given()
                .queryParam("component", component)
                .queryParam("indexName", indexName)
                .get("/elasticsearch-rest/exists")
                .then()
                .statusCode(200)
                .body(is("true"));

        // Retrieve indexed data
        RestAssured.given()
                .queryParam("component", component)
                .queryParam("indexName", indexName)
                .queryParam("indexId", indexId)
                .queryParam("indexKey", indexKey)
                .get("/elasticsearch-rest/get")
                .then()
                .statusCode(200)
                .body(is(indexValue));

        // Update indexed data
        String updatedIndexValue = indexValue + " Updated";
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .queryParam("component", component)
                .queryParam("indexId", indexId)
                .queryParam("indexName", indexName)
                .queryParam("indexKey", indexKey)
                .body(updatedIndexValue)
                .patch("/elasticsearch-rest/update")
                .then()
                .statusCode(200);

        // Verify updated data
        RestAssured.given()
                .queryParam("component", component)
                .queryParam("indexName", indexName)
                .queryParam("indexId", indexId)
                .queryParam("indexKey", indexKey)
                .get("/elasticsearch-rest/get")
                .then()
                .statusCode(200)
                .body(is(updatedIndexValue));

        // Delete indexed data
        RestAssured.given()
                .queryParam("component", component)
                .queryParam("indexName", indexName)
                .queryParam("indexId", indexId)
                .delete("/elasticsearch-rest/delete")
                .then()
                .statusCode(204);

        // Verify data deleted
        RestAssured.given()
                .queryParam("component", component)
                .queryParam("indexName", indexName)
                .queryParam("indexId", indexId)
                .queryParam("indexKey", indexKey)
                .get("/elasticsearch-rest/get")
                .then()
                .statusCode(404);
    }

    @ParameterizedTest
    @MethodSource("componentNames")
    public void testElasticsearchBulk(String component) {
        String indexName = UUID.randomUUID().toString();

        String indexId = RestAssured.given()
                .queryParam("component", component)
                .queryParam("indexName", indexName)
                .post("/elasticsearch-rest/bulk")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .asString();

        RestAssured.given()
                .queryParam("component", component)
                .queryParam("indexName", indexName)
                .get("/elasticsearch-rest/exists")
                .then()
                .statusCode(200)
                .body(is("true"));

        RestAssured.given()
                .queryParam("component", component)
                .queryParam("indexName", indexName)
                .queryParam("indexId", indexId)
                .queryParam("indexKey", "camel")
                .get("/elasticsearch-rest/get")
                .then()
                .statusCode(200)
                .body(is("quarkus"));
    }

    @ParameterizedTest
    @MethodSource("componentNames")
    public void testElasticsearchBulkIndex(String component) {
        String indexName = UUID.randomUUID().toString();
        String indexId = RestAssured.given()
                .queryParam("component", component)
                .queryParam("indexName", indexName)
                .post("/elasticsearch-rest/bulk/index")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .asString();
        assertEquals(2, indexId.split(",").length);
    }

    @ParameterizedTest
    @MethodSource("componentNames")
    public void testElasticsearchDeleteIndex(String component) {
        String indexName = UUID.randomUUID().toString();
        String indexKey = "test-key";
        String indexValue = "Hello Camel Quarkus ElasticSearch REST";

        // Index data
        RestAssured.given()
                .queryParam("component", component)
                .queryParam("indexName", indexName)
                .queryParam("indexKey", indexKey)
                .contentType(ContentType.TEXT)
                .body(indexValue)
                .post("/elasticsearch-rest/index")
                .then()
                .statusCode(201)
                .extract()
                .body()
                .asString();

        // Verify index exists
        RestAssured.given()
                .queryParam("component", component)
                .queryParam("indexName", indexName)
                .get("/elasticsearch-rest/exists")
                .then()
                .statusCode(200)
                .body(is("true"));

        // Delete indexed data
        RestAssured.given()
                .queryParam("component", component)
                .queryParam("indexName", indexName)
                .delete("/elasticsearch-rest/delete/index")
                .then()
                .statusCode(200)
                .body(is("true"));
    }

    @ParameterizedTest
    @MethodSource("componentNames")
    public void testElasticsearchSearch(String component) {
        String indexName = UUID.randomUUID().toString();
        String indexKey = "camel-quarkus";
        String indexValue = "Sub Atomic, Super Fast Camel Quarkus";

        // Index data
        RestAssured.given()
                .queryParam("component", component)
                .queryParam("indexName", indexName)
                .queryParam("indexKey", indexKey)
                .contentType(ContentType.TEXT)
                .body(indexValue)
                .post("/elasticsearch-rest/index")
                .then()
                .statusCode(201)
                .extract()
                .body()
                .asString();

        // Verify index exists
        RestAssured.given()
                .queryParam("component", component)
                .queryParam("indexName", indexName)
                .get("/elasticsearch-rest/exists")
                .then()
                .statusCode(200)
                .body(is("true"));

        // Search data
        Awaitility.await().pollInterval(50, TimeUnit.MILLISECONDS).atMost(10, TimeUnit.SECONDS).until(() -> {
            String searchResult = RestAssured.given()
                    .queryParam("component", component)
                    .queryParam("indexKey", indexKey)
                    .body("Super Fast")
                    .get("/elasticsearch-rest/search")
                    .then()
                    .statusCode(200)
                    .extract()
                    .body()
                    .asString();
            return searchResult.equals(indexValue);
        });
    }

    @ParameterizedTest
    @MethodSource("componentNames")
    public void testElasticsearchSearchJSON(String component) {
        String indexName = UUID.randomUUID().toString();
        String indexKey = "camel-quarkus";
        String indexValue = "Sub Atomic, Super Fast Camel Quarkus";

        // Index data
        RestAssured.given()
                .queryParam("component", component)
                .queryParam("indexName", indexName)
                .queryParam("indexKey", indexKey)
                .contentType(ContentType.TEXT)
                .body(indexValue)
                .post("/elasticsearch-rest/index")
                .then()
                .statusCode(201)
                .extract()
                .body()
                .asString();

        // Verify index exists
        RestAssured.given()
                .queryParam("component", component)
                .queryParam("indexName", indexName)
                .get("/elasticsearch-rest/exists")
                .then()
                .statusCode(200)
                .body(is("true"));

        // Search data
        Awaitility.await().pollInterval(50, TimeUnit.MILLISECONDS).atMost(10, TimeUnit.SECONDS).until(() -> {
            String searchResult = RestAssured.given()
                    .queryParam("component", component)
                    .queryParam("indexKey", indexKey)
                    .body("Super Fast")
                    .get("/elasticsearch-rest/search/json")
                    .then()
                    .statusCode(200)
                    .extract()
                    .body()
                    .asString();
            return searchResult.equals(indexValue);
        });
    }

    @ParameterizedTest
    @MethodSource("componentNames")
    public void testElasticsearchMultiSearch(String component) {
        String indexName = UUID.randomUUID().toString();
        String indexKey = "camel-quarkus";
        String indexValue = "Sub Atomic, Super Fast Camel Quarkus";

        // Index data
        RestAssured.given()
                .queryParam("component", component)
                .queryParam("indexName", indexName)
                .queryParam("indexKey", indexKey)
                .contentType(ContentType.TEXT)
                .body(indexValue)
                .post("/elasticsearch-rest/index")
                .then()
                .statusCode(201)
                .extract()
                .body()
                .asString();

        // Verify index exists
        RestAssured.given()
                .queryParam("component", component)
                .queryParam("indexName", indexName)
                .get("/elasticsearch-rest/exists")
                .then()
                .statusCode(200)
                .body(is("true"));

        // Search data
        Awaitility.await().pollInterval(50, TimeUnit.MILLISECONDS).atMost(10, TimeUnit.SECONDS).until(() -> {
            String hits = RestAssured.given()
                    .queryParam("component", component)
                    .queryParam("indexName", indexName)
                    .queryParam("indexKey", indexKey)
                    .body("Sub Atomic,Super Fast")
                    .get("/elasticsearch-rest/search/multi")
                    .then()
                    .statusCode(200)
                    .extract()
                    .body()
                    .asString();
            return hits.equals("2");
        });
    }

    @SuppressWarnings("unused")
    private static String[] componentNames() {
        return new String[] { "elasticsearch-rest", "elasticsearch-rest-quarkus" };
    }
}
