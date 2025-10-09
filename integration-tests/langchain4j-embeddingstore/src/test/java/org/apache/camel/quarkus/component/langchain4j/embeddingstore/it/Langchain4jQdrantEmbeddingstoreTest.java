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
package org.apache.camel.quarkus.component.langchain4j.embeddingstore.it;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import io.qdrant.client.grpc.Collections;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.awaitility.Awaitility;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@QuarkusTestResource(QdrantTestResource.class)
@QuarkusTest
class Langchain4jQdrantEmbeddingstoreTest {
    static final String MESSAGE = "Hello Camel Quarkus LangChain4j Embedding Store";

    @Test
    void qdrantEmbeddingStoreCrud() throws Exception {
        Config config = ConfigProvider.getConfig();
        try (QdrantClient client = new QdrantClient(
                QdrantGrpcClient.newBuilder(
                        config.getValue("qdrant.host", String.class),
                        config.getValue("qdrant.port", Integer.class),
                        false)
                        .build())) {

            // Create collection
            client.createCollectionAsync(config.getValue("qdrant.collection", String.class),
                    Collections.VectorParams.newBuilder()
                            .setDistance(Collections.Distance.Cosine)
                            .setSize(384)
                            .build())
                    .get(10, TimeUnit.SECONDS);

            // Store embedding
            String id = RestAssured.given()
                    .body(MESSAGE)
                    .post("/langchain4j-embeddingstore/qdrant/add")
                    .then()
                    .statusCode(200)
                    .extract()
                    .body()
                    .asString();

            // Verify Qdrant returned the id of new item
            assertNotNull(UUID.fromString(id));

            // Search embedding
            RestAssured.given()
                    .body(MESSAGE)
                    .post("/langchain4j-embeddingstore/qdrant/search")
                    .then()
                    .statusCode(200)
                    .body(
                            "size()", is(1),
                            "[0].embeddingId", is(id),
                            "[0].text", is(MESSAGE));

            // Remove embedding
            RestAssured.given()
                    .queryParam("embeddingId", id)
                    .delete("/langchain4j-embeddingstore/qdrant/remove")
                    .then()
                    .statusCode(204);

            // Confirm removal
            Awaitility.await().pollInterval(250, TimeUnit.MILLISECONDS).atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
                RestAssured.given()
                        .body(MESSAGE)
                        .post("/langchain4j-embeddingstore/qdrant/search")
                        .then()
                        .statusCode(200)
                        .body(
                                "size()", is(0));
            });
        }
    }
}
