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
package org.apache.camel.quarkus.component.langchain4j.ragbridge.it;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

/**
 * Tests the auto-detection path: no augmentor config is set, but EmbeddingStore
 * and EmbeddingModel beans are present, so the bridge auto-produces a default
 * RetrievalAugmentor backed by the {@code @Default} store.
 */
@QuarkusTest
class RagBridgeTest {

    @Test
    void defaultAugmentorIsResolvable() {
        RestAssured.given()
                .get("/rag-bridge/augmentor-present")
                .then()
                .statusCode(200)
                .body(is("true"));
    }

    @Test
    void noNamedAugmentorInAutoDetection() {
        RestAssured.given()
                .get("/rag-bridge/named-augmentor-present")
                .then()
                .statusCode(200)
                .body(is("false"));
    }

    @Test
    void autoDetectedAugmentorRetrievesFromDefaultStore() {
        RestAssured.given()
                .body("Apache Camel is a powerful open source integration framework")
                .post("/rag-bridge/ingest")
                .then()
                .statusCode(200);

        RestAssured.given()
                .body("Tell me about integration frameworks")
                .post("/rag-bridge/ask")
                .then()
                .statusCode(200)
                .body(containsString("Apache Camel"));
    }

    @Test
    void namedEmbeddingStoreIsInCamelRegistry() {
        RestAssured.given()
                .get("/rag-bridge/registry/embedding-store/products")
                .then()
                .statusCode(200)
                .body(is("true"));
    }

    /**
     * Exercises the {@code @EmbeddingStoreName} registry bridge in isolation. The
     * {@code qualifier-only} store carries no {@code @Named} qualifier, so CDI does not
     * put it in the Camel registry by itself - the only thing that can bind it under
     * that name is {@code QuarkusLangchain4jRecorder.registerNamedEmbeddingStores()}.
     * Unlike {@link #namedEmbeddingStoreIsInCamelRegistry()}, this fails if the bridge
     * is removed.
     */
    @Test
    void qualifierOnlyEmbeddingStoreIsBridgedIntoCamelRegistry() {
        RestAssured.given()
                .get("/rag-bridge/registry/embedding-store/qualifier-only")
                .then()
                .statusCode(200)
                .body(is("true"));
    }

    @Test
    void unknownEmbeddingStoreIsNotInCamelRegistry() {
        RestAssured.given()
                .get("/rag-bridge/registry/embedding-store/nonexistent")
                .then()
                .statusCode(200)
                .body(is("false"));
    }

    @Test
    void defaultAugmentorDoesNotSeeDataInOtherStore() {
        RestAssured.given()
                .body("Quarkus is a supersonic subatomic Java framework")
                .post("/rag-bridge/ingest-products")
                .then()
                .statusCode(200);

        RestAssured.given()
                .body("Tell me about Java frameworks")
                .post("/rag-bridge/ask")
                .then()
                .statusCode(200)
                .body(not(containsString("Quarkus")));
    }
}
