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
    private static final String SEND_ENDPOINT = "/langchain4j-agent-ql4j/send";
    private static final String STATELESS_SECRET = "PARROT42";

    @Test
    void simpleUserMessage() {
        RestAssured.given()
                .body(Langchain4jAgentTest.TEST_USER_MESSAGE_SIMPLE)
                .post("/langchain4j-agent/simple")
                .then()
                .statusCode(200)
                .body(
                        not(Langchain4jAgentTest.TEST_USER_MESSAGE_SIMPLE),
                        containsString("Apache Camel"));
    }

    /**
     * Verifies that AgentWithoutMemory is truly stateless when Quarkus LangChain4j is on the classpath.
     * WireMock variant: calls the agent twice with the same message; if QL4J's ChatMemoryProvider leaks in,
     * the second request's body includes chat history and WireMock's strict equalToJson matching rejects it.
     *
     * @see <a href="https://github.com/apache/camel-quarkus/issues/8836">#8836</a>
     */
    @Test
    @DisabledIfEnvironmentVariable(named = "LANGCHAIN4J_OLLAMA_BASE_URL", matches = ".+", disabledReason = "This test uses WireMock strict matching")
    void agentWithoutMemoryIsStatelessWiremock() {

        String first = RestAssured.given()
                .body(Langchain4jAgentTest.TEST_USER_MESSAGE_SIMPLE)
                .post(SEND_ENDPOINT)
                .then()
                .statusCode(200)
                .extract().asString();

        String second = RestAssured.given()
                .body(Langchain4jAgentTest.TEST_USER_MESSAGE_SIMPLE)
                .post(SEND_ENDPOINT)
                .then()
                .statusCode(200)
                .extract().asString();

        assertFalse(first.isEmpty(), "First response should not be empty");
        assertFalse(second.isEmpty(), "Second response should not be empty");
    }

    /**
     * Verifies statelessness against a real LLM using a secret-based approach: first call tells the
     * agent a secret, second call asks for it. If memory leaks, the agent remembers; if stateless, it doesn't.
     *
     * @see <a href="https://github.com/apache/camel-quarkus/issues/8836">#8836</a>
     */
    @Test
    @EnabledIfEnvironmentVariable(named = "LANGCHAIN4J_OLLAMA_BASE_URL", matches = ".+", disabledReason = "Requires a real LLM — set LANGCHAIN4J_OLLAMA_BASE_URL")
    void agentWithoutMemoryIsStatelessRealApi() {

        RestAssured.given()
                .body("The secret code is " + STATELESS_SECRET + ". Acknowledge it.")
                .post(SEND_ENDPOINT)
                .then()
                .statusCode(200);

        String second = RestAssured.given()
                .body("What secret code did I tell you? Reply ONLY the code or say UNKNOWN.")
                .post(SEND_ENDPOINT)
                .then()
                .statusCode(200)
                .extract().asString();

        assertFalse(second.contains(STATELESS_SECRET),
                "Agent should NOT remember the secret across calls — memory leaked");
    }
}
