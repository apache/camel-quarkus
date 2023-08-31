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
package org.apache.camel.quarkus.dsl.groovy;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.apache.camel.dsl.groovy.GroovyRoutesBuilderLoader;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;

@QuarkusTest
class GroovyDslTest {

    @Test
    void groovyHello() {
        RestAssured.given()
                .body("John Smith")
                .post("/groovy-dsl/hello")
                .then()
                .statusCode(200)
                .body(CoreMatchers.is("Hello John Smith from Groovy!"));
    }

    @Test
    void testMainInstanceWithJavaRoutes() {
        RestAssured.given()
                .get("/groovy-dsl/main/groovyRoutesBuilderLoader")
                .then()
                .statusCode(200)
                .body(CoreMatchers.is(GroovyRoutesBuilderLoader.class.getName()));

        RestAssured.given()
                .get("/groovy-dsl/main/routeBuilders")
                .then()
                .statusCode(200)
                .body(CoreMatchers.is(""));

        RestAssured.given()
                .get("/groovy-dsl/main/routes")
                .then()
                .statusCode(200)
                .body(CoreMatchers.is(
                        "my-groovy-route,routes-with-components-configuration,routes-with-dataformats-configuration,routes-with-eip-body,routes-with-eip-exchange,routes-with-eip-message,routes-with-eip-process,routes-with-eip-setBody,routes-with-endpoint-dsl,routes-with-error-handler,routes-with-languages-configuration,routes-with-rest,routes-with-rest-dsl-get,routes-with-rest-dsl-post,routes-with-rest-get,routes-with-rest-post"));
        RestAssured.given()
                .get("/groovy-dsl/main/successful/routes")
                .then()
                .statusCode(200)
                .body(CoreMatchers.is("10"));
    }

    @Test
    void testRestEndpoints() {
        RestAssured.given()
                .get("/root/my/path/get")
                .then()
                .statusCode(200)
                .body(CoreMatchers.is("Hello World"));
        RestAssured.given()
                .body("Will")
                .post("/root/post")
                .then()
                .statusCode(200)
                .body(CoreMatchers.is("Hello Will"));
    }
}
