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

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

@QuarkusTest
class AiToolLangchain4jTest {

    @Test
    void weatherServiceTest() {
        RestAssured.given()
                .contentType("text/plain")
                .body("What is the weather in Prague?")
                .post("/ai-tool-langchain4j/weather/chat")
                .then()
                .statusCode(200)
                .body(containsString("1111"));
    }

    @Test
    void weatherServiceTestWithWrongTag() {
        RestAssured.given()
                .contentType("text/plain")
                .body("What is the weather in Prague?")
                .post("/ai-tool-langchain4j/admin/chat")
                .then()
                .statusCode(200)
                .body(containsString("Latest news about camel"));
    }

    @Test
    void aiToolRegistryToolsByWeatherTagTest() {
        RestAssured.given()
                .get("/ai-tool-langchain4j/tools/weatherTag")
                .then()
                .statusCode(200)
                .body(containsString("getWeather"))
                .body(not(containsString("getNews")));
    }

    @Test
    void aiToolRegistryToolsByAdminTagTest() {
        RestAssured.given()
                .get("/ai-tool-langchain4j/tools/adminTag")
                .then()
                .statusCode(200)
                .body(containsString("getNews"))
                .body(not(containsString("getWeather")));
    }

    @Test
    void adminServiceTest() {
        RestAssured.given()
                .contentType("text/plain")
                .body("What are the latest news about camel?")
                .post("/ai-tool-langchain4j/admin/chat")
                .then()
                .statusCode(200)
                .body(containsString("camel"))
                .body(containsString("news"));
    }

    // Verifies that the @CamelAiTools interceptor filters tools by tag: a service tagged "adminTag"
    // should not see the getWeather tool (tagged "weatherTag"), so the mock model won't call it
    // and "1111" won't appear in the response. Fails if the interceptor is broken and all tools are visible.
    @Test
    void weatherWithWrongTagTest() {
        RestAssured.given()
                .contentType("text/plain")
                .body("What is the weather in Prague?")
                .post("/ai-tool-langchain4j/weather-wrong-tag/chat")
                .then()
                .statusCode(200)
                .body(not(containsString("1111")));
    }

}
