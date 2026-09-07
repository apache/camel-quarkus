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
package org.apache.camel.quarkus.component.langchain4j.chat.ql4j.it;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.containsStringIgnoringCase;

@QuarkusTest
@QuarkusTestResource(OllamaTestResource.class)
class LangChain4jChatQl4jTest {

    @Test
    void simpleMessageWithDefaultModel() {
        RestAssured.given()
                .body("Hello my name is Darth Vader!")
                .post("/langchain4j-chat-ql4j/simple-message")
                .then()
                .statusCode(200)
                .body(containsString("Hello"));
    }

    @Test
    void promptMessageWithDefaultModel() {
        RestAssured.get("/langchain4j-chat-ql4j/prompt-message")
                .then()
                .statusCode(200)
                .body(allOf(
                        containsString("potato"),
                        containsString("tomato"),
                        containsString("feta"),
                        containsString("olive oil")));
    }

    @Test
    void multiTurnConversationWithDefaultModel() {
        RestAssured.get("/langchain4j-chat-ql4j/multi-turn-conversation")
                .then()
                .statusCode(200)
                .body(containsStringIgnoringCase("Moroccan"));
    }

    /**
     * The stub answering this scenario matches only requests naming the model configured for the Quarkus LangChain4j
     * named model 'custom', so it passes only if Camel picked that model over the default one.
     */
    @Test
    void simpleMessageWithNamedModel() {
        RestAssured.given()
                .body("Which large language model are you?")
                .post("/langchain4j-chat-ql4j/named-model-simple-message")
                .then()
                .statusCode(200)
                .body(containsStringIgnoringCase("Granite"));
    }
}
