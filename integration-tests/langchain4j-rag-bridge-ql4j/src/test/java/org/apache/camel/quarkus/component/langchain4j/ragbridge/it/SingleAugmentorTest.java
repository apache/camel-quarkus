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
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

/**
 * Verifies explicit single-augmentor configuration: a single named augmentor is
 * configured via {@code quarkus.camel.langchain4j.rag.augmentors.products}, so
 * it is also marked as {@code defaultBean()} for auto-discovery by
 * {@code @RegisterAiService}.
 *
 * <p>
 * No {@code @QuarkusIntegrationTest} counterpart exists because this test uses
 * {@code @TestProfile} to change {@code BUILD_AND_RUN_TIME_FIXED} config.
 * Native binaries are pre-built with the default config baked in, so profile
 * overrides have no effect in {@code @QuarkusIntegrationTest}.
 */
@QuarkusTest
@TestProfile(SingleAugmentorProfile.class)
class SingleAugmentorTest {

    @Test
    void defaultAugmentorIsResolvable() {
        RestAssured.given()
                .get("/rag-bridge/augmentor-present")
                .then()
                .statusCode(200)
                .body(is("true"));
    }

    @Test
    void namedAugmentorIsResolvable() {
        RestAssured.given()
                .get("/rag-bridge/named-augmentor-present")
                .then()
                .statusCode(200)
                .body(is("true"));
    }

    @Test
    void namedAugmentorRetrievesFromCorrectStore() {
        RestAssured.given()
                .body("Apache Camel is a powerful open source integration framework")
                .post("/rag-bridge/ingest-products")
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
    void namedAugmentorDoesNotSeeDataInOtherStore() {
        RestAssured.given()
                .body("Quarkus is a supersonic subatomic Java framework")
                .post("/rag-bridge/ingest")
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
