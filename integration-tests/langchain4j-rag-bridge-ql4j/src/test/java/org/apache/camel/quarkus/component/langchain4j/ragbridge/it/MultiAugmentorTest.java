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
import io.quarkus.test.junit.TestProfile;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.is;

/**
 * Verifies that with multiple augmentors configured and one marked {@code default=true}, the
 * designated one serves the unqualified lookup (RAG stays on) while the others remain
 * resolvable by name. Two augmentors with no default marked fail the build instead of silently
 * disabling RAG — covered by {@code RagAugmentorDefaultResolutionTest} in the support module.
 *
 * <p>
 * No {@code @QuarkusIntegrationTest} counterpart exists because this test uses
 * {@code @TestProfile} to change {@code BUILD_AND_RUN_TIME_FIXED} config.
 * Native binaries are pre-built with the default config baked in, so profile
 * overrides have no effect in {@code @QuarkusIntegrationTest}. A native-mode
 * test would require a separate Maven module with its own
 * {@code application.properties}.
 */
@QuarkusTest
@TestProfile(MultiAugmentorProfile.class)
class MultiAugmentorTest {

    @Test
    void designatedDefaultAugmentorResolvableWithMultipleConfigured() {
        RestAssured.given()
                .get("/rag-bridge/augmentor-present")
                .then()
                .statusCode(200)
                .body(is("true"));
    }

    @Test
    void namedAugmentorStillResolvable() {
        RestAssured.given()
                .get("/rag-bridge/named-augmentor-present")
                .then()
                .statusCode(200)
                .body(is("true"));
    }

    /**
     * The augmentor that is not the designated default: it carries {@code @RagAugmentorName} to
     * suppress the implicit {@code @Default}, and the whole design depends on that qualifier
     * leaving {@code @Named} lookup intact.
     */
    @Test
    void nonDefaultAugmentorStillResolvableByName() {
        RestAssured.given()
                .get("/rag-bridge/support-augmentor-present")
                .then()
                .statusCode(200)
                .body(is("true"));
    }

    /**
     * The retrieval filter is one application-wide bean shared by every augmentor, so it is told
     * both which augmentor retrieves and which store is being searched.
     */
    @Test
    void retrievalFilterReceivesTheAugmentorAndStoreNames() {
        RestAssured.delete("/rag-bridge/filter/last-call").then().statusCode(200);

        RestAssured.given()
                .contentType(ContentType.TEXT)
                .queryParam("augmentor", "support")
                .body("anything")
                .post("/rag-bridge/augment")
                .then()
                .statusCode(200);

        RestAssured.get("/rag-bridge/filter/last-augmentor")
                .then()
                .statusCode(200)
                .body(is("support"));

        // the store this augmentor is configured with, not the augmentor's own name
        RestAssured.get("/rag-bridge/filter/last-store")
                .then()
                .statusCode(200)
                .body(is("products"));
    }
}
