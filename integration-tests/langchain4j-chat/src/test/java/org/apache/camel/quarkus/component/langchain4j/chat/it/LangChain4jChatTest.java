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
package org.apache.camel.quarkus.component.langchain4j.chat.it;

import java.util.stream.Stream;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@QuarkusTest
@QuarkusTestResource(OllamaTestResource.class)
class LangChain4jChatTest {

    @ParameterizedTest
    @MethodSource("simpleMessageEndpoints")
    void simpleMessage(String directEndpointUri, String mockEndpointUri) {
        RestAssured.given()
                .queryParam("directEndpointUri", directEndpointUri)
                .queryParam("mockEndpointUri", mockEndpointUri)
                .get("/langchain4j-chat/simple-message")
                .then()
                .statusCode(200);
    }

    @Test
    void promptMessage() {
        RestAssured.get("/langchain4j-chat/prompt-message")
                .then()
                .statusCode(200);
    }

    @Test
    void multipleMessages() {
        RestAssured.get("/langchain4j-chat/multiple-messages")
                .then()
                .statusCode(200);
    }

    private static Stream<Arguments> simpleMessageEndpoints() {
        return Stream.of(
                Arguments.of("direct:send-simple-message", "mock:simpleMessageResponse"),
                Arguments.of("direct:send-simple-message-m1", "mock:simpleMessageResponseM1"),
                Arguments.of("direct:send-simple-message-m2", "mock:simpleMessageResponseM2"));
    }
}
