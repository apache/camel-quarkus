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
package org.apache.camel.quarkus.main;

import javax.ws.rs.core.MediaType;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import org.apache.camel.dsl.yaml.YamlRoutesBuilderLoader;
import org.apache.camel.dsl.yaml.common.YamlDeserializationMode;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.is;

@QuarkusTest
public class CoreMainYamlTest {
    @Test
    public void testMainInstanceWithYamlRoutes() {
        JsonPath p = RestAssured.given()
                .accept(MediaType.APPLICATION_JSON)
                .get("/main/yaml/describe")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .jsonPath();

        assertThat(p.getString("yaml-routes-builder-loader"))
                .isEqualTo(YamlRoutesBuilderLoader.class.getName());
        assertThat(p.getList("routeBuilders", String.class))
                .isEmpty();
        assertThat(p.getList("routes", String.class))
                .contains("my-yaml-route", "rest-route");
        assertThat(p.getMap("global-options", String.class, String.class))
                .containsEntry(YamlRoutesBuilderLoader.DESERIALIZATION_MODE, YamlDeserializationMode.FLOW.name());
    }

    @Test
    public void yamlRoute() {
        RestAssured.get("/main/yaml/greet")
                .then()
                .statusCode(200)
                .body(is("Hello World!"));

        RestAssured.given()
                .queryParam("forceFailure", "true")
                .get("/main/yaml/greet")
                .then()
                .statusCode(200)
                .body(is("Sorry something went wrong"));
    }

    @Test
    public void beanDeclaredInJavaYamlRoute() {
        RestAssured.get("/main/yaml/greet/from/java/bean")
                .then()
                .statusCode(200)
                .body(is("Hello from bean declared in java!"));
    }

    @Test
    public void tryCatchYamlRoute() {
        RestAssured.given()
                .get("/main/yaml/try/catch")
                .then()
                .statusCode(200)
                .body(is("do-catch caught an exception"));
    }

    @ParameterizedTest
    @ValueSource(strings = { "GET", "POST", "PATCH", "PUT", "DELETE", "HEAD" })
    public void yamlRests(String method) {
        Matcher<String> matcher;
        if (method.equals("HEAD")) {
            matcher = emptyString();
        } else {
            matcher = is(method + ": " + "/rest");
        }

        RestAssured.request(method, "/rest")
                .then()
                .statusCode(200)
                .body(matcher);
    }

    @Test
    public void yamlTemplate() {
        RestAssured.get("/templated/greeting")
                .then()
                .statusCode(200)
                .body(is("Hello World!"));

        RestAssured.post("/templated/greeting")
                .then()
                .statusCode(404);
    }

}
