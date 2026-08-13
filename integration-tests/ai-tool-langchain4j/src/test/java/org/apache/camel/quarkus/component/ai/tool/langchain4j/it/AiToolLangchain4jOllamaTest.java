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
package org.apache.camel.quarkus.component.ai.tool.langchain4j.it;

import java.util.Map;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

@QuarkusTest
@TestProfile(AiToolLangchain4jOllamaTest.OllamaProfile.class)
@EnabledIfSystemProperty(named = "ollama.test", matches = "true")
class AiToolLangchain4jOllamaTest {

    @Test
    void weatherFromToolTest() {
        RestAssured.given()
                .contentType("text/plain")
                .body("What is the weather in Prague?")
                .post("/ai-tool-langchain4j-ollama/chat")
                .then()
                .statusCode(200)
                .body(containsString("1111"));
    }

    // Verifies that the @CamelAiTools interceptor filters tools by tag: a service tagged "adminTag"
    // should not see the getWeather tool (tagged "weatherTag"), so the real LLM won't call it
    // and "1111" won't appear in the response. Fails if the interceptor is broken and all tools are visible.
    @Test
    void weatherWithWrongTag() {
        RestAssured.given()
                .contentType("text/plain")
                .body("What is the weather in Prague?")
                .post("/ai-tool-langchain4j-ollama/chat-wrong-tag")
                .then()
                .statusCode(200)
                .body(not(containsString("1111")));
    }

    public static class OllamaProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "quarkus.devservices.enabled", "true",
                    "quarkus.langchain4j.devservices.enabled", "true");
        }
    }
}
