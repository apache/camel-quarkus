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
package org.apache.camel.quarkus.component.ibm.watson.discovery.it;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@EnabledIfSystemProperty(named = "camel.ibm.watson.apiKey", matches = ".*", disabledReason = "IBM Watson API Key not provided")
@EnabledIfSystemProperty(named = "camel.ibm.watson.serviceUrl", matches = ".*", disabledReason = "IBM Watson Service URL not provided")
@EnabledIfSystemProperty(named = "camel.ibm.watson.projectId", matches = ".*", disabledReason = "IBM Watson Project ID not provided")
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class IbmWatsonDiscoveryTest {

    private static String collectionId;
    private static String documentId;

    @Test
    @Order(1)
    public void testCreateCollection() {
        collectionId = RestAssured.given()
                .post("/ibm-watson-discovery/collections/camel-test-collection")
                .then()
                .statusCode(200)
                .body("collection_id", notNullValue())
                .body("name", equalTo("camel-test-collection"))
                .extract()
                .path("collection_id");

        assertNotNull(collectionId, "Collection ID should not be null");
    }

    @Test
    @Order(2)
    public void testListCollections() {
        RestAssured.given()
                .get("/ibm-watson-discovery/collections")
                .then()
                .statusCode(200)
                .body("collections.name", hasItem("camel-test-collection"));
    }

    @Test
    @Order(3)
    public void testAddDocument() {
        if (collectionId == null || collectionId.isEmpty()) {
            throw new IllegalStateException("Collection ID not set - create collection test may have failed");
        }

        String documentContent = "{\"title\": \"Test Document\", \"content\": \"This is a test document for Camel Quarkus integration test\"}";

        documentId = RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(documentContent)
                .post("/ibm-watson-discovery/collections/" + collectionId + "/documents")
                .then()
                .statusCode(200)
                .body("document_id", notNullValue())
                .extract()
                .path("document_id");

        assertNotNull(documentId, "Document ID should not be null");
    }

    @Test
    @Order(4)
    public void testUpdateDocument() {
        if (collectionId == null || documentId == null) {
            throw new IllegalStateException("Collection or Document ID not set");
        }

        String updatedContent = "{\"title\": \"Updated Test Document\", \"content\": \"This document has been updated\"}";

        String updatedDocId = RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(updatedContent)
                .put("/ibm-watson-discovery/collections/" + collectionId + "/documents/" + documentId)
                .then()
                .statusCode(200)
                .body("document_id", equalTo(documentId))
                .extract()
                .path("document_id");

        assertEquals(documentId, updatedDocId, "Document ID should match");
    }

    @Test
    @Order(5)
    public void testQuery() {
        if (collectionId == null) {
            throw new IllegalStateException("Collection ID not set");
        }

        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body("test")
                .post("/ibm-watson-discovery/collections/" + collectionId + "/query")
                .then()
                .statusCode(200);
    }

    @Test
    @Order(6)
    public void testDeleteDocument() {
        if (collectionId == null || documentId == null) {
            throw new IllegalStateException("Collection or Document ID not set");
        }

        RestAssured.given()
                .delete("/ibm-watson-discovery/collections/" + collectionId + "/documents/" + documentId)
                .then()
                .statusCode(204);
    }

    @Test
    @Order(7)
    public void testDeleteCollection() {
        if (collectionId == null) {
            throw new IllegalStateException("Collection ID not set");
        }

        RestAssured.given()
                .delete("/ibm-watson-discovery/collections/" + collectionId)
                .then()
                .statusCode(204);
    }

}
