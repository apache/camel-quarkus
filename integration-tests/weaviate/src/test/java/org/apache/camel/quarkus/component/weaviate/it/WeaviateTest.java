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
package org.apache.camel.quarkus.component.weaviate.it;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import org.apache.camel.component.weaviate.WeaviateVectorDbAction;
import org.apache.camel.component.weaviate.WeaviateVectorDbHeaders;
import org.hamcrest.Matchers;
import org.hamcrest.text.IsEmptyString;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@QuarkusTest
@QuarkusTestResource(WeaviateTestResource.class)
class WeaviateTest {
    private static final Logger LOG = Logger.getLogger(WeaviateTest.class);

    @Test
    public void operations() {
        String collectionName = "WeaviateCQCollectionCrud" + System.currentTimeMillis();
        List<Float> values = Arrays.asList(1.0f, 2.0f, 3.0f);
        Map<String, String> properties = Map.of("sky", "blue", "age", "34");
        Map<String, String> updatedProperties = Map.of("dog", "dachshund");

        boolean collectionCreated = false;
        try {
            createCollection(collectionName);
            LOG.infof("Collection created: %s", collectionName);
            collectionCreated = true;

            String id = createEntry(collectionName, values, properties);

            queryById(collectionName, id)
                    .body("result", Matchers.aMapWithSize(1))
                    .body("result." + id, Matchers.aMapWithSize(2))
                    .body("result." + id, Matchers.hasKey("sky"))
                    .body("result." + id, Matchers.hasKey("age"))
                    .body("result." + id, Matchers.not(Matchers.hasKey("dog")));

            updateById(collectionName, id, values, updatedProperties);

            queryById(collectionName, id)
                    .body("result", Matchers.aMapWithSize(1))
                    .body("result." + id, Matchers.aMapWithSize(3))
                    .body("result." + id, Matchers.hasKey("sky"))
                    .body("result." + id, Matchers.hasKey("age"))
                    .body("result." + id, Matchers.hasKey("dog"));

            deleteById(collectionName, id);

            queryById(collectionName, id)
                    .body("result", Matchers.nullValue());

        } finally {
            if (collectionCreated) {
                deleteCollection(collectionName);

                query(collectionName, Arrays.asList(0.15f, 0.25f, 0.35f), Map.of("title", "", "content", ""), true)
                        .body("error", Matchers.not(IsEmptyString.emptyOrNullString()));
            }
        }
    }

    @Test
    public void query() {
        String collectionName = "WeaviateCQCollectionVector" + System.currentTimeMillis();

        boolean collectionCreated = false;
        try {
            createCollection(collectionName);
            LOG.infof("Collection created: %s", collectionName);
            collectionCreated = true;

            createEntry(collectionName, Arrays.asList(0.1f, 0.2f, 0.3f),
                    Map.of("title", "First Article", "content", "The content of the first article."));
            createEntry(collectionName, Arrays.asList(0.2f, 0.3f, 0.4f),
                    Map.of("title", "Second Article", "content", "The content of the second article."));
            createEntry(collectionName, Arrays.asList(0.3f, 0.4f, 0.5f),
                    Map.of("title", "Third Article", "content", "The content of the third article."));

            query(collectionName, Arrays.asList(0.15f, 0.25f, 0.35f), Map.of("title", "", "content", ""))
                    .body("result", Matchers.hasSize(2))
                    .body("result[0]", Matchers.aMapWithSize(2))
                    .body("result[0].title", Matchers.equalTo("Second Article"))
                    .body("result[1].title", Matchers.equalTo("First Article"));

            query(collectionName, Arrays.asList(0.3f, 0.4f, 0.5f), Map.of("title", "", "content", ""))
                    .body("result", Matchers.hasSize(2))
                    .body("result[0]", Matchers.aMapWithSize(2))
                    .body("result[0].title", Matchers.equalTo("Third Article"))
                    .body("result[1].title", Matchers.equalTo("Second Article"));

        } finally {
            if (collectionCreated) {
                deleteCollection(collectionName);

                query(collectionName, Arrays.asList(0.15f, 0.25f, 0.35f), Map.of("title", "", "content", ""), true)
                        .body("error", Matchers.not(IsEmptyString.emptyOrNullString()));
            }
        }
    }

    private void createCollection(String name) {
        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(Map.of(WeaviateVectorDbHeaders.ACTION, WeaviateVectorDbAction.CREATE_COLLECTION,
                        WeaviateVectorDbHeaders.COLLECTION_NAME, name))
                .post("/weaviate/request")
                .then()
                .statusCode(200)
                .body("result", Matchers.is(true));
    }

    private void deleteCollection(String name) {
        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(Map.of(WeaviateVectorDbHeaders.ACTION, WeaviateVectorDbAction.DELETE_COLLECTION,
                        WeaviateVectorDbHeaders.COLLECTION_NAME, name))
                .post("/weaviate/request")
                .then()
                .statusCode(200)
                .body("result", Matchers.is(true));
    }

    private String createEntry(String collectionName, List<Float> values, Map<String, String> properties) {

        Map<String, Object> payload = Map.of(
                "body", values,
                WeaviateVectorDbHeaders.ACTION, WeaviateVectorDbAction.CREATE,
                WeaviateVectorDbHeaders.COLLECTION_NAME, collectionName,
                WeaviateVectorDbHeaders.PROPERTIES, properties);

        String createdId = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(payload)
                .post("/weaviate/request")
                .then()
                .statusCode(200)
                .extract().path("result");

        Assertions.assertNotNull(createdId);

        return createdId;
    }

    public ValidatableResponse queryById(String collectionName, String id) {
        Map<String, Object> payload = Map.of(
                WeaviateVectorDbHeaders.ACTION, WeaviateVectorDbAction.QUERY_BY_ID,
                WeaviateVectorDbHeaders.COLLECTION_NAME, collectionName,
                WeaviateVectorDbHeaders.INDEX_ID, id);

        return RestAssured.given()
                .contentType(ContentType.JSON)
                .body(payload)
                .post("/weaviate/request")
                .then()
                .statusCode(200);
    }

    private void updateById(String collectionName, String id, List<Float> values,
            Map<String, String> properties) {

        Map<String, Object> payload = Map.of(
                "body", values,
                WeaviateVectorDbHeaders.ACTION, WeaviateVectorDbAction.UPDATE_BY_ID,
                WeaviateVectorDbHeaders.COLLECTION_NAME, collectionName,
                WeaviateVectorDbHeaders.INDEX_ID, id,
                WeaviateVectorDbHeaders.PROPERTIES, properties);

        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(payload)
                .post("/weaviate/request")
                .then()
                .statusCode(200)
                .body("result", Matchers.is(true));
    }

    public void deleteById(String collectionName, String id) {

        Map<String, Object> payload = Map.of(
                WeaviateVectorDbHeaders.ACTION, WeaviateVectorDbAction.DELETE_BY_ID,
                WeaviateVectorDbHeaders.COLLECTION_NAME, collectionName,
                WeaviateVectorDbHeaders.INDEX_ID, id);

        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(payload)
                .post("/weaviate/request")
                .then().statusCode(200)
                .body("result", Matchers.is(true));
    }

    @Test
    public void newOperations() {
        String collectionName = "WeaviateCQNewOps" + System.currentTimeMillis();

        boolean collectionCreated = false;
        try {
            createCollection(collectionName);
            LOG.infof("Collection created: %s", collectionName);
            collectionCreated = true;

            batchCreate(collectionName);

            aggregate(collectionName);

            // BM25 for "10" finds green and yellow (both have age=10) — pure text, no ranking preference
            bm25Query(collectionName);

            // Hybrid for "10" with vector [7.5, 8.5, 9.5] and alpha=0.75 also matches both,
            // but ranks yellow first because its vector is closest to the query vector
            // and the vector-heavy alpha makes that dominate
            hybridQuery(collectionName);

        } finally {
            if (collectionCreated) {
                deleteCollection(collectionName);
            }
        }
    }

    private void batchCreate(String collectionName) {
        List<Map<String, Object>> objects = List.of(
                Map.of("properties", Map.of("sky", "green", "age", "10"),
                        "vector", Arrays.asList(4.0, 5.0, 6.0)),
                Map.of("properties", Map.of("sky", "red", "age", "20"),
                        "vector", Arrays.asList(7.0, 8.0, 9.0)),
                Map.of("properties", Map.of("sky", "yellow", "age", "10"),
                        "vector", Arrays.asList(7.5, 8.5, 9.5)));

        Map<String, Object> payload = Map.of(
                "body", objects,
                WeaviateVectorDbHeaders.ACTION, WeaviateVectorDbAction.BATCH_CREATE,
                WeaviateVectorDbHeaders.COLLECTION_NAME, collectionName);

        List<String> uuids = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(payload)
                .post("/weaviate/request")
                .then()
                .statusCode(200)
                .body("uuids", Matchers.hasSize(3))
                .body("errors", Matchers.empty())
                .extract().path("uuids");

        queryById(collectionName, uuids.get(0))
                .body("result", Matchers.aMapWithSize(1))
                .body("result." + uuids.get(0), Matchers.is(Map.of("sky", "green", "age", "10")));

        queryById(collectionName, uuids.get(1))
                .body("result", Matchers.aMapWithSize(1))
                .body("result." + uuids.get(1), Matchers.is(Map.of("sky", "red", "age", "20")));

        queryById(collectionName, uuids.get(2))
                .body("result", Matchers.aMapWithSize(1))
                .body("result." + uuids.get(2), Matchers.is(Map.of("sky", "yellow", "age", "10")));
    }

    private void aggregate(String collectionName) {
        Map<String, Object> payload = Map.of(
                WeaviateVectorDbHeaders.ACTION, WeaviateVectorDbAction.AGGREGATE,
                WeaviateVectorDbHeaders.COLLECTION_NAME, collectionName);

        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(payload)
                .post("/weaviate/request")
                .then()
                .statusCode(200)
                .body("totalCount", Matchers.equalTo(3));
    }

    private void bm25Query(String collectionName) {
        Map<String, Object> fields = Map.of("sky", "", "age", "");

        Map<String, Object> payload = Map.of(
                "body", "10",
                WeaviateVectorDbHeaders.ACTION, WeaviateVectorDbAction.BM25_QUERY,
                WeaviateVectorDbHeaders.COLLECTION_NAME, collectionName,
                WeaviateVectorDbHeaders.QUERY_TOP_K, 10,
                WeaviateVectorDbHeaders.FIELDS, fields);

        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(payload)
                .post("/weaviate/request")
                .then()
                .statusCode(200)
                .body("result", Matchers.hasSize(2))
                .body("result.sky", Matchers.containsInAnyOrder("green", "yellow"));
    }

    private void hybridQuery(String collectionName) {
        Map<String, Object> fields = Map.of("sky", "", "age", "");

        Map<String, Object> payload = Map.of(
                "body", "10",
                WeaviateVectorDbHeaders.ACTION, WeaviateVectorDbAction.HYBRID_QUERY,
                WeaviateVectorDbHeaders.COLLECTION_NAME, collectionName,
                WeaviateVectorDbHeaders.QUERY_TOP_K, 10,
                WeaviateVectorDbHeaders.HYBRID_ALPHA, 0.75,
                WeaviateVectorDbHeaders.QUERY_VECTOR, Arrays.asList(7.5, 8.5, 9.5),
                WeaviateVectorDbHeaders.FIELDS, fields);

        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(payload)
                .post("/weaviate/request")
                .then()
                .statusCode(200)
                .body("result", Matchers.hasSize(Matchers.greaterThanOrEqualTo(2)))
                .body("result[0].sky", Matchers.equalTo("yellow"));
    }

    private ValidatableResponse query(String collectionName, List<Float> values, Map<String, String> fields) {
        return query(collectionName, values, fields, false);
    }

    private ValidatableResponse query(String collectionName, List<Float> values, Map<String, String> fields,
            boolean expectError) {

        Map<String, Object> payload = Map.of(
                "body", values,
                WeaviateVectorDbHeaders.ACTION, WeaviateVectorDbAction.QUERY,
                WeaviateVectorDbHeaders.COLLECTION_NAME, collectionName,
                WeaviateVectorDbHeaders.QUERY_TOP_K, 2,
                WeaviateVectorDbHeaders.FIELDS, fields);

        ValidatableResponse response = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(payload)
                .post("/weaviate/request")
                .then()
                .statusCode(200);

        return response;
    }
}
