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
package org.apache.camel.quarkus.js;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.apache.camel.dsl.js.JavaScriptRoutesBuilderLoader;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;

@QuarkusTest
class JavaScriptDslTest {

    @Test
    void jsHello() {
        RestAssured.given()
                .body("David Smith")
                .post("/js-dsl/hello")
                .then()
                .statusCode(200)
                .body(CoreMatchers.is("Hello David Smith from JavaScript!"));
    }

    @Test
    void testMainInstanceWithJavaRoutes() {
        RestAssured.given()
                .get("/js-dsl/main/jsRoutesBuilderLoader")
                .then()
                .statusCode(200)
                .body(CoreMatchers.is(JavaScriptRoutesBuilderLoader.class.getName()));

        RestAssured.given()
                .get("/js-dsl/main/routeBuilders")
                .then()
                .statusCode(200)
                .body(CoreMatchers.is(""));

        RestAssured.given()
                .get("/js-dsl/main/routes")
                .then()
                .statusCode(200)
                .body(CoreMatchers.is(
                        "my-js-route,routes-with-component-configuration,routes-with-context-configuration,routes-with-endpoint-dsl,routes-with-modules,routes-with-processors-consumer,routes-with-processors-processor,routes-with-rest-configuration,routes-with-rest-configuration-goodbye,routes-with-rest-dsl,routes-with-rest-dsl-hello"));

        RestAssured.given()
                .get("/js-dsl/main/successful/routes")
                .then()
                .statusCode(200)
                .body(CoreMatchers.is("6"));
    }

    @Test
    void testRestEndpoints() {
        RestAssured.given()
                .get("/say/hello")
                .then()
                .statusCode(200)
                .body(CoreMatchers.is("Hello World"));
        RestAssured.given()
                .get("/say/goodbye")
                .then()
                .statusCode(200)
                .body(CoreMatchers.is("Bye World"));
    }
}
