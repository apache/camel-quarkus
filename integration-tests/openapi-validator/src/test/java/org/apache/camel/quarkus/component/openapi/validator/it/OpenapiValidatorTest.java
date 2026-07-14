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
package org.apache.camel.quarkus.component.openapi.validator.it;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

@QuarkusTest
class OpenapiValidatorTest {

    @Test
    void validRequestShouldSucceed() {
        RestAssured.given()
                .contentType(ContentType.JSON)
                .body("{\"name\":\"Apple\",\"description\":\"A fruit\"}")
                .post("/api/fruits")
                .then()
                .statusCode(200)
                .body(is("Fruit created"));
    }

    @Test
    void emptyRequestBodyShouldFail() {
        RestAssured.given()
                .contentType(ContentType.JSON)
                .post("/api/fruits")
                .then()
                .statusCode(400)
                .body(containsString("request body"));
    }

    @Test
    void missingRequiredFieldShouldFail() {
        RestAssured.given()
                .contentType(ContentType.JSON)
                .body("{\"name\":\"Apple\"}")
                .post("/api/fruits")
                .then()
                .statusCode(400)
                .body(containsString("description"));
    }

    @Test
    void wrongPropertyTypeShouldFail() {
        RestAssured.given()
                .contentType(ContentType.JSON)
                .body("{\"name\":123,\"description\":\"A fruit\"}")
                .post("/api/fruits")
                .then()
                .statusCode(400)
                .body(containsString("name"));
    }
}
