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
package org.apache.camel.quarkus.component.langchain4j.agent.it;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertFalse;

@ExtendWith(Langchain4jTestWatcher.class)
@QuarkusTestResource(Langchain4jAgentTestResource.class)
@QuarkusTest
class Langchain4jAgentQl4jTest {
    private static final String SIMPLE_ENDPOINT = "/langchain4j-agent/simple";
    private static final String SIMPLE_WITHOUT_REQUEST_CONTEXT_ENDPOINT = "/langchain4j-agent-ql4j/simple";
    private static final String STATELESS_SECRET = "PARROT42";

    @Test
    void simpleUserMessage() {
        RestAssured.given()
                .body(Langchain4jAgentTest.TEST_USER_MESSAGE_SIMPLE)
                .post(SIMPLE_ENDPOINT)
                .then()
                .statusCode(200)
                .body(
                        not(Langchain4jAgentTest.TEST_USER_MESSAGE_SIMPLE),
                        containsString("Apache Camel"));
    }

    @Test
    @DisabledIfEnvironmentVariable(named = "LANGCHAIN4J_OLLAMA_BASE_URL", matches = ".+", disabledReason = "This test uses WireMock strict matching")
    void agentWithoutMemoryIsStatelessWiremock() {
        String first = RestAssured.given()
                .body(Langchain4jAgentTest.TEST_USER_MESSAGE_SIMPLE)
                .post(SIMPLE_WITHOUT_REQUEST_CONTEXT_ENDPOINT)
                .then()
                .statusCode(200)
                .extract().asString();

        String second = RestAssured.given()
                .body(Langchain4jAgentTest.TEST_USER_MESSAGE_SIMPLE)
                .post(SIMPLE_WITHOUT_REQUEST_CONTEXT_ENDPOINT)
                .then()
                .statusCode(200)
                .extract().asString();

        assertFalse(first.isEmpty(), "First response should not be empty");
        assertFalse(second.isEmpty(), "Second response should not be empty");
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "LANGCHAIN4J_OLLAMA_BASE_URL", matches = ".+", disabledReason = "Requires a real LLM — set LANGCHAIN4J_OLLAMA_BASE_URL")
    void agentWithoutMemoryIsStatelessRealApi() {
        RestAssured.given()
                .body("The secret code is " + STATELESS_SECRET + ". Acknowledge it.")
                .post(SIMPLE_WITHOUT_REQUEST_CONTEXT_ENDPOINT)
                .then()
                .statusCode(200);

        String second = RestAssured.given()
                .body("What secret code did I tell you? Reply ONLY the code or say UNKNOWN.")
                .post(SIMPLE_WITHOUT_REQUEST_CONTEXT_ENDPOINT)
                .then()
                .statusCode(200)
                .extract().asString();

        assertFalse(second.contains(STATELESS_SECRET),
                "Agent should NOT remember the secret across calls — memory leaked");
    }
}
