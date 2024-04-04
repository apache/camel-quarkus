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
package org.apache.camel.quarkus.component.csimple.it;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.is;

@QuarkusTest
class CSimpleTest {

    @Test
    void csimpleHello() {
        RestAssured.given()
                .body("Joe")
                .contentType(ContentType.TEXT)
                .post("/csimple/csimple-hello")
                .then()
                .body(is("Hello Joe"));
    }

    @Test
    void csimpleHi() {
        RestAssured.given()
                .get("/csimple/csimple-hi")
                .then()
                .body(is("Hi Bill"));
    }

    @Test
    void csimpleXml() {
        RestAssured.given()
                .body("Joe")
                .contentType(ContentType.TEXT)
                .post("/csimple/csimple-xml-dsl")
                .then()
                .body(is("Hi Joe"));
    }

    @Test
    void csimpleYaml() {
        RestAssured.given()
                .body("John")
                .contentType(ContentType.TEXT)
                .post("/csimple/csimple-yaml-dsl")
                .then()
                .body(is("Bonjour John"));
    }

    @Test
    void csimpleHigh() {
        RestAssured.given()
                .body("15")
                .post("/csimple/predicate")
                .then()
                .statusCode(200)
                .body(CoreMatchers.is("High"));
    }

    @Test
    void csimpleLow() {
        RestAssured.given()
                .body("3")
                .post("/csimple/predicate")
                .then()
                .statusCode(200)
                .body(CoreMatchers.is("Low"));
    }
}
