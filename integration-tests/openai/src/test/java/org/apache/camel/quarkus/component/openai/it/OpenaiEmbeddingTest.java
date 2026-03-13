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
package org.apache.camel.quarkus.component.openai.it;

import java.util.List;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTestResource(value = OpenaiEmbeddingTestResource.class, restrictToAnnotatedClass = true)
@QuarkusTest
class OpenaiEmbeddingTest {

    private static final Logger LOG = Logger.getLogger(OpenaiEmbeddingTest.class);

    @BeforeEach
    void logTestName(TestInfo testInfo) {
        LOG.info(String.format("Running OpenaiEmbeddingTest test %s", testInfo.getDisplayName()));
    }

    @Test
    void simpleEmbedding() {
        String simpleText = "Simple text";
        List<?> result = RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(simpleText)
                .post("/openai/embeddings")
                .then()
                .statusCode(200)
                .body(".", hasSize(greaterThan(0)))
                .extract()
                .body()
                .as(List.class);

        Double similarity = RestAssured.given()
                .contentType(ContentType.JSON)
                .queryParam("embeddingContent", simpleText)
                .body(result)
                .post("/openai/vector/similarity")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .as(Double.class);

        assertTrue(similarity >= 0.9);
    }

    @SuppressWarnings("unchecked")
    @Test
    void batchEmbedding() {
        String batchText = "Text content 1,Text content 2,Text content 3";
        List<List<?>> result = RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(batchText)
                .post("/openai/embeddings")
                .then()
                .statusCode(200)
                .body(
                        ".", hasSize(equalTo(3)),
                        "[0]", hasSize(greaterThan(0)),
                        "[1]", hasSize(greaterThan(0)),
                        "[2]", hasSize(greaterThan(0)))
                .extract()
                .body()
                .as(List.class);

        String[] batchTextParts = batchText.split(",");
        for (int i = 0; i < batchTextParts.length; i++) {
            Double similarity = RestAssured.given()
                    .contentType(ContentType.JSON)
                    .queryParam("embeddingContent", batchTextParts[i])
                    .body(result.get(i))
                    .post("/openai/vector/similarity")
                    .then()
                    .statusCode(200)
                    .extract()
                    .body()
                    .as(Double.class);
            assertTrue(similarity >= 0.9);
        }
    }

    @Test
    void vectorSimilarity() {
        String simpleText = "Simple text";
        List<?> result = RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(simpleText)
                .post("/openai/embeddings")
                .then()
                .statusCode(200)
                .body(".", hasSize(greaterThan(0)))
                .extract()
                .body()
                .as(List.class);

        Double similarity = RestAssured.given()
                .contentType(ContentType.JSON)
                .queryParam("embeddingContent", simpleText + " extra content")
                .body(result)
                .post("/openai/vector/similarity")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .as(Double.class);

        assertTrue(similarity >= 0.6);
    }
}
