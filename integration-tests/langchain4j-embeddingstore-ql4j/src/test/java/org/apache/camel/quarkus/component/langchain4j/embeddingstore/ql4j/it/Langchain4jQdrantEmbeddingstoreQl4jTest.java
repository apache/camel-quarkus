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
package org.apache.camel.quarkus.component.langchain4j.embeddingstore.ql4j.it;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@QuarkusTestResource(OllamaWireMockTestResource.class)
@QuarkusTestResource(QdrantTestResource.class)
@QuarkusTest
class Langchain4jQdrantEmbeddingstoreQl4jTest {
    static final String CAMEL_MESSAGE = "Hello Camel Quarkus LangChain4j Embedding Store";
    static final String QL4J_MESSAGE = "Hello from Quarkus LangChain4j direct ingest";

    @Test
    void qdrantEmbeddingstoreQl4jInterop() {
        // 1. Camel ADD
        String id = RestAssured.given()
                .body(CAMEL_MESSAGE)
                .post("/langchain4j-embeddingstore/qdrant/add")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .asString();
        assertNotNull(UUID.fromString(id));

        // 2. Camel SEARCH
        RestAssured.given()
                .body(CAMEL_MESSAGE)
                .post("/langchain4j-embeddingstore/qdrant/search")
                .then()
                .statusCode(200)
                .body(
                        "size()", is(1),
                        "[0].embeddingId", is(id),
                        "[0].text", is(CAMEL_MESSAGE));

        // 3. QL4J retrieves Camel-written data
        RestAssured.given()
                .body(CAMEL_MESSAGE)
                .post("/langchain4j-embeddingstore/qdrant/retrieve")
                .then()
                .statusCode(200)
                .body(
                        "size()", greaterThanOrEqualTo(1),
                        "$", hasItem(CAMEL_MESSAGE));

        // 4. QL4J ingests directly (bypassing Camel)
        String ql4jId = RestAssured.given()
                .body(QL4J_MESSAGE)
                .post("/langchain4j-embeddingstore/qdrant/ingest")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .asString();
        assertNotNull(UUID.fromString(ql4jId));

        // 5. Camel SEARCH finds QL4J-written data
        Awaitility.await().pollInterval(250, TimeUnit.MILLISECONDS).atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            RestAssured.given()
                    .body(QL4J_MESSAGE)
                    .post("/langchain4j-embeddingstore/qdrant/search")
                    .then()
                    .statusCode(200)
                    .body(
                            "size()", greaterThanOrEqualTo(1),
                            "[0].text", is(QL4J_MESSAGE));
        });

        // 6. Camel REMOVE both embeddings with confirmation
        RestAssured.given()
                .queryParam("embeddingId", id)
                .delete("/langchain4j-embeddingstore/qdrant/remove")
                .then()
                .statusCode(204);

        RestAssured.given()
                .queryParam("embeddingId", ql4jId)
                .delete("/langchain4j-embeddingstore/qdrant/remove")
                .then()
                .statusCode(204);

        Awaitility.await().pollInterval(250, TimeUnit.MILLISECONDS).atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            RestAssured.given()
                    .body(CAMEL_MESSAGE)
                    .post("/langchain4j-embeddingstore/qdrant/search")
                    .then()
                    .statusCode(200)
                    .body(
                            "size()", is(0));
        });
    }
}
