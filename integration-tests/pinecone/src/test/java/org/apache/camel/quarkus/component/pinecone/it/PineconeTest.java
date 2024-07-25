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
package org.apache.camel.quarkus.component.pinecone.it;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import io.pinecone.clients.Pinecone;
import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openapitools.client.model.IndexModel;

import static org.apache.camel.quarkus.component.pinecone.it.PineconeResource.createPineconeClient;
import static org.apache.camel.quarkus.component.pinecone.it.PineconeRoutes.INDEX_NAME;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.openapitools.client.model.IndexModelStatus.StateEnum.READY;

@WithTestResource(PineconeTestResource.class)
@QuarkusTest
class PineconeTest {
    private static Pinecone pinecone;

    @BeforeAll
    public static void beforeAll() {
        pinecone = createPineconeClient();
    }

    @Test
    void serverlessIndex() {
        try {
            // Create serverless index
            RestAssured.given()
                    .post("/pinecone/index")
                    .then()
                    .body(is(INDEX_NAME));

            // Wait for the index to be provisioned
            await().pollInterval(Duration.ofSeconds(1)).atMost(Duration.ofSeconds(30)).until(() -> {
                List<IndexModel> indexes = pinecone.listIndexes().getIndexes();
                if (indexes == null || indexes.isEmpty()) {
                    return false;
                }

                return indexes.stream()
                        .filter(index -> index.getName().equals(INDEX_NAME))
                        .map(IndexModel::getStatus)
                        .anyMatch(status -> status.getState().equals(READY));
            });

            // Some index endpoints are not mocked:
            // - They use a dynamic host, separate from the core pinecone API endpoints
            // - They are gRPC endpoints
            Optional<String> wireMockUrl = ConfigProvider.getConfig().getOptionalValue("wiremock.url", String.class);
            if (!wireMockUrl.isPresent()) {
                // Upsert
                List<Float> vectors = List.of(1.0f, 2.0f, 3.0f);
                RestAssured.given()
                        .contentType(ContentType.JSON)
                        .body(vectors)
                        .put("/pinecone/index")
                        .then()
                        .statusCode(200)
                        .body(is("1"));

                // Query upserted data by vector
                await().pollInterval(Duration.ofSeconds(1)).atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
                    RestAssured.given()
                            .contentType(ContentType.JSON)
                            .body(vectors)
                            .get("/pinecone/index")
                            .then()
                            .statusCode(200)
                            .body(startsWith("0.9"));
                });
            }
        } finally {
            RestAssured.given()
                    .delete("/pinecone/index")
                    .then()
                    .statusCode(204);

            await().pollInterval(Duration.ofSeconds(1)).atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
                List<IndexModel> indexes = pinecone.listIndexes().getIndexes();
                assertTrue(indexes != null && indexes.stream().noneMatch(index -> index.getName().equals(INDEX_NAME)));
            });
        }
    }
}
