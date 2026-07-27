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
package org.apache.camel.quarkus.component.ai.tool.it;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class AiToolTest {

    @Test
    void toolRegistrationByTag() {
        RestAssured.get("/ai-tool/tools/tag/weather")
                .then()
                .statusCode(200)
                .body(containsString("getWeather"));

        RestAssured.get("/ai-tool/tools/tag/math")
                .then()
                .statusCode(200)
                .body(containsString("calculate"));
    }

    @Test
    void taglessToolInDefaultPool() {
        RestAssured.get("/ai-tool/tools/all")
                .then()
                .statusCode(200)
                .body(containsString("greet"));
    }

    @Test
    void defaultPoolMergedWithTaggedTools() {
        RestAssured.get("/ai-tool/tools/tag/weather")
                .then()
                .statusCode(200)
                .body(containsString("greet"));
    }

    @Test
    void allToolsReturnsEveryRegisteredTool() {
        String tools = RestAssured.get("/ai-tool/tools/all")
                .then()
                .statusCode(200)
                .extract().asString();

        assertTrue(tools.contains("getWeather"), "Expected getWeather in: " + tools);
        assertTrue(tools.contains("calculate"), "Expected calculate in: " + tools);
        assertTrue(tools.contains("greet"), "Expected greet in: " + tools);
        assertTrue(tools.contains("failingTool"), "Expected failingTool in: " + tools);
    }

    @Test
    void toolDescription() {
        RestAssured.get("/ai-tool/tools/getWeather/description")
                .then()
                .statusCode(200)
                .body(is("Get the current weather for a city"));
    }

    @Test
    void toolSchema() {
        String schema = RestAssured.get("/ai-tool/tools/getWeather/schema")
                .then()
                .statusCode(200)
                .extract().asString();

        assertTrue(schema.contains("\"city\""), "Expected city in schema: " + schema);
        assertTrue(schema.contains("\"unit\""), "Expected unit in schema: " + schema);
        assertTrue(schema.contains("\"string\""), "Expected string type in schema: " + schema);
    }

    @Test
    void executeWeatherTool() {
        RestAssured.given()
                .queryParam("param", "city=Prague")
                .queryParam("param", "unit=celsius")
                .post("/ai-tool/execute/getWeather")
                .then()
                .statusCode(200)
                .body(containsString("Prague"))
                .body(containsString("celsius"));
    }

    @Test
    void executeCalculateTool() {
        RestAssured.given()
                .queryParam("param", "a=10")
                .queryParam("param", "b=3")
                .queryParam("param", "operation=add")
                .post("/ai-tool/execute/calculate")
                .then()
                .statusCode(200)
                .body(is("13"));

        RestAssured.given()
                .queryParam("param", "a=10")
                .queryParam("param", "b=3")
                .queryParam("param", "operation=multiply")
                .post("/ai-tool/execute/calculate")
                .then()
                .statusCode(200)
                .body(is("30"));
    }

    @Test
    void executeGreetTool() {
        RestAssured.given()
                .queryParam("param", "name=Alice")
                .post("/ai-tool/execute/greet")
                .then()
                .statusCode(200)
                .body(is("Hello, Alice!"));
    }

    @Test
    void executeMissingRequiredParam() {
        RestAssured.given()
                .post("/ai-tool/execute/getWeather")
                .then()
                .statusCode(400)
                .body(containsString("city"));
    }

    @Test
    void executeFailingTool() {
        RestAssured.given()
                .post("/ai-tool/execute/failingTool")
                .then()
                .statusCode(500);
    }

    @Test
    void executeNonExistentTool() {
        RestAssured.given()
                .post("/ai-tool/execute/nonExistent")
                .then()
                .statusCode(404);
    }
}
