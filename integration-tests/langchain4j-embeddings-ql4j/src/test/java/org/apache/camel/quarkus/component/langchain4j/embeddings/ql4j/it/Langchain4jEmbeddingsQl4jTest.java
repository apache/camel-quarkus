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
package org.apache.camel.quarkus.component.langchain4j.embeddings.ql4j.it;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

@QuarkusTestResource(Langchain4jEmbeddingsQl4jTestResource.class)
@QuarkusTest
class Langchain4jEmbeddingsQl4jTest {

    @Test
    void createTextEmbeddings() {
        RestAssured.given()
                .body("Hello World")
                .post("/langchain4j-embeddings/create")
                .then()
                .statusCode(200)
                // WireMock stub returns a 5-element vector
                .body("vectorLength", is(5))
                // Token count not available from Quarkus LangChain4j embedding model
                .body("$", not(hasKey("inputTokenLength")));
    }

    @Test
    void createTextEmbeddingsMultiWord() {
        RestAssured.given()
                .body("The quick brown fox jumps over the lazy dog")
                .post("/langchain4j-embeddings/create")
                .then()
                .statusCode(200)
                // WireMock stub returns a 5-element vector
                .body("vectorLength", is(5))
                .body("$", not(hasKey("inputTokenLength")));
    }
}
