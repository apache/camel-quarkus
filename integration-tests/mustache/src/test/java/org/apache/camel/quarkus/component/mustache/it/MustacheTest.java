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
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
class MustacheTest {

    @Test
    void applyMustacheTemplateFromClassPathResourceShouldSucceed() {
        String response = RestAssured.given().contentType(ContentType.TEXT).body("FromClassPath")
                .post("/mustache/applyMustacheTemplateFromClassPathResource").then().statusCode(200)
                .extract().asString();
        assertEquals("\nMessage with body 'FromClassPath' and some header 'value'", response);
    }

    @Test
    void applyMustacheTemplateFromHeaderShouldSucceed() {
        String response = RestAssured.given().contentType(ContentType.TEXT).body("FromHeader")
                .post("/mustache/applyMustacheTemplateFromHeader").then().statusCode(200).extract()
                .asString();
        assertEquals("Body='FromHeader'", response);
    }

    @Test
    void applyMustacheTemplateUriFromHeaderShouldSucceed() {
        String response = RestAssured.given().contentType(ContentType.TEXT).body("UriFromHeader")
                .post("/mustache/applyMustacheTemplateUriFromHeader").then().statusCode(200).extract()
                .asString();
        assertEquals("\nAnother body 'UriFromHeader'!", response);
    }

    @Test
    void applyMustacheTemplateWithInheritanceShouldSucceed() {
        String response = RestAssured.get("/mustache/applyMustacheTemplateWithInheritance").then().statusCode(200).extract()
                .asString();
        assertEquals("\n\nStart ContentFrom(Child) End", response);
    }

    @Test
    void applyMustacheTemplateWithPartialsShouldSucceed() {
        String response = RestAssured.get("/mustache/applyMustacheTemplateWithPartials").then().statusCode(200).extract()
                .asString();
        assertEquals("\nStart-\nIncluded-End", response);
    }

    @Test
    void applyMustacheTemplateFromRegistryShouldSucceed() {
        String response = RestAssured.given().contentType(ContentType.TEXT).body("Entry")
                .post("/mustache/applyMustacheTemplateFromRegistry").then().statusCode(200).extract()
                .asString();
        assertEquals("Begin-FromRegistry-Entry-End", response);
    }
}
