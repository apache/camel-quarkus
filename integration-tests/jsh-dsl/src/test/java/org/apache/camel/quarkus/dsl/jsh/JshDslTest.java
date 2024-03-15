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
package org.apache.camel.quarkus.dsl.jsh;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.apache.camel.dsl.jsh.JshRoutesBuilderLoader;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;

@QuarkusTest
class JshDslTest {

    @Test
    void jshHello() {
        RestAssured.given()
                .body("Brad Smith")
                .post("/jsh-dsl/hello")
                .then()
                .statusCode(200)
                .body(CoreMatchers.is("Hello Brad Smith from JShell!"));
    }

    @Test
    void testMainInstanceWithJavaRoutes() {
        RestAssured.given()
                .get("/jsh-dsl/main/jshRoutesBuilderLoader")
                .then()
                .statusCode(200)
                .body(CoreMatchers.is(JshRoutesBuilderLoader.class.getName()));

        RestAssured.given()
                .get("/jsh-dsl/main/routeBuilders")
                .then()
                .statusCode(200)
                .body(CoreMatchers.is(""));

        RestAssured.given()
                .get("/jsh-dsl/main/routes")
                .then()
                .statusCode(200)
                .body(CoreMatchers.is("my-jsh-route"));
    }

}
