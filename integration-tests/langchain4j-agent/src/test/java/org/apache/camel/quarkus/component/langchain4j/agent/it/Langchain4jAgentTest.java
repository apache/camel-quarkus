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
import org.apache.camel.quarkus.component.langchain4j.agent.it.guardrail.ValidationFailureInputGuardrail;
import org.apache.camel.quarkus.component.langchain4j.agent.it.guardrail.ValidationFailureOutputGuardrail;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.apache.camel.quarkus.component.langchain4j.agent.it.Langchain4jAgentRoutes.USER_JOHN;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;

@ExtendWith(Langchain4jTestWatcher.class)
@QuarkusTestResource(OllamaTestResource.class)
@QuarkusTest
class Langchain4jAgentTest {
    static final String TEST_USER_MESSAGE_SIMPLE = "What is Apache Camel?";
    static final String TEST_USER_MESSAGE_STORY = "Write a short story about a lost cat.";
    static final String TEST_SYSTEM_MESSAGE = """
            You are a whimsical storyteller. Your responses should be imaginative, descriptive, and always include a touch of magic. Start every story with 'Once upon a starlit night...'""";
    static final String EXPECTED_STORY_START = "Once upon a starlit night";
    static final String EXPECTED_STORY_CONTENT = "cat";

    static final String USER_ALICE = "Alice";
    static final String USER_FAVORITE_COLOR = "blue";
    static final String MEMORY_ID = "camel-quarkus-memory-1";

    @Test
    void simpleUserMessage() {
        RestAssured.given()
                .body(TEST_USER_MESSAGE_SIMPLE)
                .post("/langchain4j-agent/simple")
                .then()
                .statusCode(200)
                .body(
                        not(TEST_USER_MESSAGE_SIMPLE),
                        containsString("Apache Camel"));
    }

    @Test
    void simpleUserMessageWithSystemMessagePrompt() {
        RestAssured.given()
                .queryParam("systemMessage", TEST_SYSTEM_MESSAGE)
                .body(TEST_USER_MESSAGE_STORY)
                .post("/langchain4j-agent/simple")
                .then()
                .statusCode(200)
                .body(
                        not(TEST_USER_MESSAGE_SIMPLE),
                        startsWith(EXPECTED_STORY_START),
                        containsString(EXPECTED_STORY_CONTENT));
    }

    @Test
    void simpleUserMessageWithAiAgentBody() {
        RestAssured.given()
                .queryParam("bodyAsBean", true)
                .queryParam("systemMessage", TEST_SYSTEM_MESSAGE)
                .body(TEST_USER_MESSAGE_STORY)
                .post("/langchain4j-agent/simple")
                .then()
                .statusCode(200)
                .body(
                        not(TEST_USER_MESSAGE_SIMPLE),
                        startsWith(EXPECTED_STORY_START),
                        containsString(EXPECTED_STORY_CONTENT));
    }

    @Test
    void agentMemory() {
        RestAssured.given()
                .queryParam("memoryId", MEMORY_ID)
                .body("Hello - my name is " + USER_ALICE)
                .post("/langchain4j-agent/memory")
                .then()
                .statusCode(200);

        RestAssured.given()
                .queryParam("memoryId", MEMORY_ID)
                .body("And my favorite color is " + USER_FAVORITE_COLOR)
                .post("/langchain4j-agent/memory")
                .then()
                .statusCode(200);

        RestAssured.given()
                .queryParam("memoryId", MEMORY_ID)
                .body("Now tell me about myself - what's my name and favorite color?")
                .post("/langchain4j-agent/memory")
                .then()
                .statusCode(200)
                .body(
                        containsString(USER_ALICE),
                        containsString(USER_FAVORITE_COLOR));
    }

    @Test
    void inputGuardrailSuccess() {
        RestAssured.given()
                .body("Hello - my name is " + USER_ALICE)
                .post("/langchain4j-agent/input/guardrail/success")
                .then()
                .statusCode(200)
                .body(is("true"));
    }

    @Test
    void inputGuardrailFailure() {
        RestAssured.given()
                .body("Hello - my name is " + USER_ALICE)
                .post("/langchain4j-agent/input/guardrail/failure")
                .then()
                .statusCode(500)
                .body(containsString("guardrail %s failed".formatted(ValidationFailureInputGuardrail.class.getName())));
    }

    @Test
    void outputGuardrailSuccess() {
        RestAssured.given()
                .body("Hello - my name is " + USER_ALICE)
                .post("/langchain4j-agent/output/guardrail/success")
                .then()
                .statusCode(200)
                .body(is("true"));
    }

    @Test
    void outputGuardrailFailure() {
        RestAssured.given()
                .body("Hello - my name is " + USER_ALICE)
                .post("/langchain4j-agent/output/guardrail/failure")
                .then()
                .statusCode(500)
                .body(containsString("guardrail %s failed".formatted(ValidationFailureOutputGuardrail.class.getName())));
    }

    @Test
    void jsonExtractorOutputGuardrailSuccess() {
        RestAssured.given()
                .body("Return an example JSON object about a person named '%s' with the fields name and description"
                        .formatted(USER_JOHN))
                .post("/langchain4j-agent/output/guardrail/json/extractor")
                .then()
                .statusCode(200)
                .body(
                        "name", is(USER_JOHN),
                        "description", notNullValue());
    }

    @Test
    void jsonExtractorOutputGuardrailFailure() {
        RestAssured.given()
                // Returns field age which is not defined in TestPojo
                .body("Return an example JSON object about a person named '%s' with the fields age and description"
                        .formatted(USER_JOHN))
                .post("/langchain4j-agent/output/guardrail/json/extractor")
                .then()
                .statusCode(500)
                .body(containsString("Invalid JSON"));
    }

    @Test
    void simpleRag() {
        RestAssured.given()
                .body("Describe the Miles of Camels Car Rental cancellations policy for cancelling 24 hours before pickup. What is the refund amount?")
                .post("/langchain4j-agent/rag")
                .then()
                .statusCode(200)
                .body(containsStringIgnoringCase("full refund"));
    }

    @Test
    void simpleToolInvocation() {
        RestAssured.given()
                .body("What is the name of user ID 123?")
                .post("/langchain4j-agent/tools")
                .then()
                .statusCode(200)
                .body(containsStringIgnoringCase(USER_JOHN));
    }

    @Test
    void customAiService() {
        RestAssured.given()
                .body(USER_JOHN)
                .post("/langchain4j-agent/custom/service")
                .then()
                .statusCode(200)
                .body(
                        "name", is(USER_JOHN),
                        "description", notNullValue());
    }

    @Test
    void agentWithCustomTools() {
        RestAssured.given()
                .body("Calculate the addition of 10 + 5")
                .post("/langchain4j-agent/custom/tools")
                .then()
                .statusCode(200)
                .body(
                        "result", containsStringIgnoringCase("15"),
                        "toolWasInvoked", is(true));
    }
}
