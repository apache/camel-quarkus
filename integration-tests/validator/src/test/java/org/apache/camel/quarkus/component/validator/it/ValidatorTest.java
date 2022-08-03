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
package org.apache.camel.quarkus.component.validator.it;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

@QuarkusTest
class ValidatorTest {

    @Test
    public void validXMLFromClassPath() {

        RestAssured.given()
                .contentType(ContentType.XML)
                .body("<message><firstName>MyFirstname</firstName><lastName>MyLastname</lastName></message>")
                .post("/validator/classpath")
                .then()
                .statusCode(200);

    }

    @Test
    public void invalidXMLFromClassPath() {

        RestAssured.given()
                .contentType(ContentType.XML)
                .body("<message><firstName>MyFirstname</firstName></message>")
                .post("/validator/classpath")
                .then()
                .statusCode(500);

    }

    @Test
    public void validXMLFromFileSystem() {

        RestAssured.given()
                .contentType(ContentType.XML)
                .body("<message><firstName>MyFirstname</firstName><lastName>MyLastname</lastName></message>")
                .post("/validator/filesystem")
                .then()
                .statusCode(200);

    }

    @Test
    public void invalidXMLFromFileSystem() {

        RestAssured.given()
                .contentType(ContentType.XML)
                .body("<message><firstName>MyFirstname</firstName></message>")
                .post("/validator/filesystem")
                .then()
                .statusCode(500);

    }

    @Test
    public void validXMLFromHTTPEndPoint() {

        RestAssured.given()
                .contentType(ContentType.XML)
                .body("<message><firstName>MyFirstname</firstName><lastName>MyLastname</lastName></message>")
                .post("/validator/http")
                .then()
                .statusCode(200);

    }

    @Test
    public void invalidXMLFromHTTPEndPoint() {

        RestAssured.given()
                .contentType(ContentType.XML)
                .body("<message><firstName>MyFirstname</firstName></message>")
                .post("/validator/http")
                .then()
                .statusCode(500);

    }

}
