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
package org.apache.camel.quarkus.component.rest.it;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.apache.camel.component.platform.http.PlatformHttpConstants;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesPattern;

@QuarkusTest
class RestTest {

    @Test
    public void inspectConfiguration() {
        RestAssured.when()
                .get("/rest/inspect/configuration")
                .then()
                .statusCode(200)
                .body("component", is(PlatformHttpConstants.PLATFORM_HTTP_COMPONENT_NAME));
    }

    @Test
    public void rest() {
        RestAssured.get("/rest/get")
                .then()
                .header("Access-Control-Allow-Headers", matchesPattern(".*Access-Control.*"))
                .header("Access-Control-Allow-Methods", matchesPattern("GET, POST"))
                .header("Access-Control-Allow-Credentials", equalTo("true"))
                .body(equalTo("GET: /rest/get"));

        RestAssured.given()
                .contentType(ContentType.TEXT)
                .post("/rest/post")
                .then()
                .statusCode(200)
                .body(equalTo("POST: /rest/post"));
    }

    @Test
    public void pathTemplate() {
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .get("/rest/template/Hello/World")
                .then()
                .statusCode(200)
                .body(equalTo("Hello World"));
    }

    @Test
    public void requestValidation() {
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body("Camel Quarkus")
                .header("messageEnd", "REST")
                .post("/rest/validation")
                .then()
                .statusCode(400)
                .body(equalTo("Some of the required query parameters are missing."));

        RestAssured.given()
                .contentType(ContentType.TEXT)
                .queryParam("messageStart", "Hello")
                .header("messageEnd", "REST")
                .post("/rest/validation")
                .then()
                .statusCode(400)
                .body(equalTo("The request body is missing."));

        RestAssured.given()
                .contentType(ContentType.TEXT)
                .queryParam("messageStart", "Hello")
                .body("Camel Quarkus")
                .post("/rest/validation")
                .then()
                .statusCode(400)
                .body(equalTo("Some of the required HTTP headers are missing."));

        RestAssured.given()
                .contentType(ContentType.TEXT)
                .queryParam("messageStart", "Hello")
                .body("Camel Quarkus")
                .header("messageEnd", "REST")
                .post("/rest/validation")
                .then()
                .statusCode(200)
                .body(equalTo("Hello Camel Quarkus REST"));
    }

    @Test
    public void jsonBinding() {
        Person person = new Person();
        person.setFirstName("John");
        person.setLastName("Doe");
        person.setAge(64);

        String result = String.format(
                "Name: %s %s, Age: %d",
                person.getFirstName(),
                person.getLastName(),
                person.getAge());

        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(person)
                .post("/rest/pojo/binding/json")
                .then()
                .statusCode(200)
                .body(equalTo(result));
    }

    @Test
    public void xmlBinding() {
        Person person = new Person();
        person.setFirstName("John");
        person.setLastName("Doe");
        person.setAge(64);

        String result = String.format(
                "Name: %s %s, Age: %d",
                person.getFirstName(),
                person.getLastName(),
                person.getAge());

        RestAssured.given()
                .contentType(ContentType.XML)
                .body(person)
                .post("/rest/pojo/binding/xml")
                .then()
                .statusCode(200)
                .body(equalTo(result));
    }

    @Test
    public void testRestProducer() {
        RestAssured.given()
                .queryParam("port", RestAssured.port)
                .get("/rest/invoke/route")
                .then()
                .statusCode(200)
                .body(equalTo("Hello Invoked"));
    }

    @Test
    public void lightweight() throws Throwable {
        RestAssured.when()
                .get("/rest/inspect/camel-context/lightweight")
                .then()
                .statusCode(200)
                .body(is("true"));
    }

}
