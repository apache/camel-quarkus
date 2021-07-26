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
package org.apache.camel.quarkus.component.mustache.it;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

import static org.hamcrest.Matchers.is;

@QuarkusTest
class MustacheTest {

    //@Test
    void templateFromClassPathResource() {
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body("FromClassPath")
                .post("/mustache/templateFromClassPathResource")
                .then()
                .statusCode(200)
                .body(is("\nMessage with body 'FromClassPath' and some header 'value'"));
    }

    //@Test
    void templateFromHeader() {
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body("FromHeader")
                .post("/mustache/templateFromHeader")
                .then()
                .statusCode(200)
                .body(is("Body='FromHeader'"));
    }

    //@Test
    void templateUriFromHeader() {
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body("UriFromHeader")
                .post("/mustache/templateUriFromHeader")
                .then()
                .statusCode(200)
                .body(is("\nAnother body 'UriFromHeader'!"));
    }

    //@Test
    void templateWithInheritance() {
        RestAssured.get("/mustache/templateWithInheritance")
                .then()
                .statusCode(200)
                .body(is("\n\nStart ContentFrom(Child) End"));
    }

    //@Test
    void templateWithPartials() {
        RestAssured.get("/mustache/templateWithPartials")
                .then()
                .statusCode(200)
                .body(is("\nStart-\nIncluded-End"));
    }

    //@Test
    void templateFromRegistry() {
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body("Entry")
                .post("/mustache/templateFromRegistry")
                .then()
                .statusCode(200)
                .body(is("Begin-FromRegistry-Entry-End"));
    }
}
