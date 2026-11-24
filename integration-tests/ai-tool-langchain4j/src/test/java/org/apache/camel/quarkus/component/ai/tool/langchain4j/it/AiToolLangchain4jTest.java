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
import io.restassured.parsing.Parser;
import io.restassured.response.ValidatableResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;

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
    void adminServiceCallsNewsToolForAnyQuestion() {
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

    // Verifies that @CamelAiTools filters tools by tag: a service tagged "adminTag"
    // should not see the getWeather tool (tagged "weatherTag"), so the mock model won't call it
    // and "1111" won't appear in the response. Fails if the tag filter is broken and all tools are visible.
    @Test
    void weatherWithWrongTagTest() {
        RestAssured.given()
                .contentType("text/plain")
                .body("What is the weather in Prague?")
                .post("/ai-tool-langchain4j/weather-wrong-tag/chat")
                .then()
                .statusCode(200)
                .body(not(containsString("1111")))
                // the service still receives the tools of its own tag
                .body(containsString("offered tools: getNews"));
    }

    @Test
    void serviceWithoutCamelAiToolsReceivesAllTools() {
        chat("all-tools", "list")
                .body(is("createOrder,failingTool,getNews,getWeather"));
    }

    @Test
    void invalidJsonArgumentsAreReportedToModel() {
        chat("all-tools", "call getWeather {not json")
                .body(startsWith("Invalid arguments: could not parse the provided JSON arguments"));
    }

    @Test
    void toolFailureIsSanitized() {
        chat("all-tools", "call failingTool {}")
                .body(is("Tool execution failed"));
    }

    // Without the isCamelAgentRequest guard in CamelAiToolProvider, the agent gets every tool plus a duplicate getWeather
    @Test
    void camelAgentReceivesOnlyItsTaggedTools() {
        chat("camel-agent", "list")
                .body(is("getWeather"));
    }

    @Test
    void nestedArgSchemaReachesModel() {
        chat("all-tools", "schema createOrder")
                .parser("text/plain", Parser.JSON)
                .body("parameters.properties.customer.properties.name.type", is("string"))
                .body("parameters.properties.customer.required", contains("name"))
                .body("parameters.properties.items.type", is("array"))
                .body("parameters.properties.items.items.properties.quantity.type", is("integer"))
                .body("parameters.properties.items.items.required", contains("sku", "quantity"))
                .body("parameters.properties.priority.enum", contains("LOW", "HIGH"))
                .body("parameters.required", contains("customer", "items"));
    }

    @Test
    void nestedArgumentsReachRoute() {
        chat("all-tools",
                "call createOrder {\"customer\":{\"name\":\"Joe\"},\"items\":[{\"sku\":\"A1\",\"quantity\":2}],\"priority\":\"HIGH\"}")
                .body(is("Joe,A1,2,HIGH"));
    }

    private static ValidatableResponse chat(String service, String message) {
        return RestAssured.given()
                .contentType("text/plain")
                .body(message)
                .post("/ai-tool-langchain4j/" + service + "/chat")
                .then()
                .statusCode(200);
    }

    // The streaming model answers with the names of the tools it was offered: only the tagged tool is expected,
    // also when the Multi service is called on the event loop
    @ParameterizedTest
    @ValueSource(strings = { "non-blocking", "blocking" })
    void weatherStreamingServiceGetsOnlyTaggedTools(String mode) {
        RestAssured.given()
                .contentType("text/plain")
                .body("What is the weather in Prague?")
                .post("/ai-tool-langchain4j/weather-streaming/" + mode + "/chat")
                .then()
                .statusCode(200)
                .body(is("getWeather"));
    }

}
