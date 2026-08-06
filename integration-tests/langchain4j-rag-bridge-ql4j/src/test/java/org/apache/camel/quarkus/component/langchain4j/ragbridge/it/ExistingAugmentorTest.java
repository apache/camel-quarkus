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

import static org.hamcrest.Matchers.is;

/**
 * Verifies that the bridge yields when a user-provided {@code RetrievalAugmentor}
 * already exists in CDI. The bridge should not produce a conflicting bean.
 *
 * <p>
 * No {@code @QuarkusIntegrationTest} counterpart exists because this test uses
 * {@code @TestProfile} to change {@code BUILD_AND_RUN_TIME_FIXED} config.
 * Native binaries are pre-built with the default config baked in, so profile
 * overrides have no effect in {@code @QuarkusIntegrationTest}.
 */
@QuarkusTest
@TestProfile(ExistingAugmentorProfile.class)
class ExistingAugmentorTest {

    @Test
    void existingAugmentorIsResolvable() {
        RestAssured.given()
                .get("/rag-bridge/augmentor-present")
                .then()
                .statusCode(200)
                .body(is("true"));
    }

    @Test
    void noNamedAugmentorWhenExistingPresent() {
        RestAssured.given()
                .get("/rag-bridge/named-augmentor-present")
                .then()
                .statusCode(200)
                .body(is("false"));
    }
}
