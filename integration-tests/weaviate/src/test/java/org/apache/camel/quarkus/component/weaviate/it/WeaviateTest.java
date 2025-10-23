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
    public void simpleCrud() {
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
            }
        }
    }

    @Test
    public void queryByVector() {
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

            queryByVector(collectionName, Arrays.asList(0.15f, 0.25f, 0.35f), Map.of("title", "", "content", ""))
                    .body("result.data.Get." + collectionName, Matchers.hasSize(2))
                    .body("result.data.Get." + collectionName + "[0]", Matchers.aMapWithSize(2))
                    .body("result.data.Get." + collectionName + "[0].title", Matchers.equalTo("Second Article"))
                    .body("result.data.Get." + collectionName + "[1].title", Matchers.equalTo("First Article"));

            queryByVector(collectionName, Arrays.asList(0.3f, 0.4f, 0.5f), Map.of("title", "", "content", ""))
                    .body("result.data.Get." + collectionName, Matchers.hasSize(2))
                    .body("result.data.Get." + collectionName + "[0]", Matchers.aMapWithSize(2))
                    .body("result.data.Get." + collectionName + "[0].title", Matchers.equalTo("Third Article"))
                    .body("result.data.Get." + collectionName + "[1].title", Matchers.equalTo("Second Article"));

        } finally {
            if (collectionCreated) {
                deleteCollection(collectionName);
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
                .body("error", IsEmptyString.emptyOrNullString())
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
                .body("error", IsEmptyString.emptyOrNullString())
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
                .body("error", IsEmptyString.emptyOrNullString())
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
                .statusCode(200)
                .body("error", IsEmptyString.emptyOrNullString());
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
                .body("error", IsEmptyString.emptyOrNullString());
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
                .body("error", IsEmptyString.emptyOrNullString())
                .body("result", Matchers.is(true));
    }

    private ValidatableResponse queryByVector(String collectionName, List<Float> values, Map<String, String> fields) {

        Map<String, Object> payload = Map.of(
                "body", values,
                WeaviateVectorDbHeaders.ACTION, WeaviateVectorDbAction.QUERY,
                WeaviateVectorDbHeaders.COLLECTION_NAME, collectionName,
                WeaviateVectorDbHeaders.QUERY_TOP_K, 2,
                WeaviateVectorDbHeaders.FIELDS, fields);

        return RestAssured.given()
                .contentType(ContentType.JSON)
                .body(payload)
                .post("/weaviate/request")
                .then()
                .statusCode(200)
                .body("error", IsEmptyString.emptyOrNullString());
    }
}
