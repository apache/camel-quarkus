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
package org.apache.camel.quarkus.component.aws.bedrock.it;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.apache.camel.quarkus.test.support.aws2.Aws2MockBackend;
import org.apache.camel.quarkus.test.support.aws2.Aws2TestResource;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.oneOf;

@QuarkusTest
@QuarkusTestResource(Aws2TestResource.class)
class AwsBedrockTest {
    private static final String PROMPT = "Reply with the single word: Hello";
    private static final String FLOCI_STUB_RESPONSE = "Floci stub response";

    @Aws2MockBackend
    boolean mockBackend;

    @Test
    void converse() {
        assertConverseResponse("/aws-bedrock/converse");
    }

    @Test
    void converseStream() {
        assertConverseResponse("/aws-bedrock/converse-stream");
    }

    @Test
    void invokeTextModel() {
        if (mockBackend) {
            given()
                    .contentType(ContentType.TEXT)
                    .body(PROMPT)
                    .post("/aws-bedrock/invoke-text-model")
                    .then()
                    .statusCode(200)
                    .body("outputs[0].text", is(FLOCI_STUB_RESPONSE));
        } else {
            given()
                    .contentType(ContentType.TEXT)
                    .body(PROMPT)
                    .post("/aws-bedrock/invoke-text-model")
                    .then()
                    .statusCode(200)
                    .body("output.message.content[0].text", not(emptyOrNullString()));
        }
    }

    private void assertConverseResponse(String path) {
        given()
                .contentType(ContentType.TEXT)
                .body(PROMPT)
                .post(path)
                .then()
                .statusCode(200)
                .body(
                        "text", mockBackend ? containsString(FLOCI_STUB_RESPONSE) : not(emptyOrNullString()),
                        "stopReason", oneOf("end_turn", "max_tokens"),
                        "inputTokens", greaterThan(0),
                        "outputTokens", greaterThan(0));
    }
}
