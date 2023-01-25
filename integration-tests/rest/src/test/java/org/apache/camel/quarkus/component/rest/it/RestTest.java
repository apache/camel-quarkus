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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import org.apache.camel.component.platform.http.PlatformHttpConstants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesPattern;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
class RestTest {
    private static final Person person = new Person("John", "Doe", 64);

    @Test
    public void inspectConfiguration() {
        RestAssured.when()
                .get("/rest/inspect/configuration")
                .then()
                .statusCode(200)
                .body("component", is(PlatformHttpConstants.PLATFORM_HTTP_COMPONENT_NAME));
    }

    @ParameterizedTest
    @ValueSource(strings = { "DELETE", "GET", "HEAD", "PATCH", "POST", "PUT" })
    public void rest(String method) {
        String body = method.matches("PATCH|POST|PUT") ? method : "";

        ValidatableResponse response = RestAssured.given()
                .body(body)
                .request(method, "/rest")
                .then()
                .header("Access-Control-Allow-Credentials", equalTo("true"))
                .header("Access-Control-Allow-Headers", matchesPattern(".*Access-Control.*"))
                .header("Access-Control-Allow-Methods", equalTo("GET, POST"))
                .header("Access-Control-Allow-Origin", equalTo("*"))
                .header("Access-Control-Max-Age", equalTo("3600"));

        if (method.equals("HEAD")) {
            // Response body is ignored for HEAD so verify response headers
            response.statusCode(204);
            response.header("Content-Type", ContentType.TEXT.toString());
        } else {
            response.statusCode(200);
            response.body(is(method + ": /rest"));
        }
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
    public void jsonBindingProducer() {
        Person respondPerson = RestAssured.given()
                .queryParam("port", RestAssured.port)
                .get("/rest/producer/binding/mode/json")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .as(Person.class);
        assertEquals(respondPerson, person);
    }

    @Test
    public void xmlBinding() {
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
    public void xmlBindingProducer() {
        Person respondPerson = RestAssured.given()
                .queryParam("port", RestAssured.port)
                .get("/rest/producer/binding/mode/xml")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .as(Person.class);
        assertEquals(respondPerson, person);
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
    public void lightweight() {
        RestAssured.when()
                .get("/rest/inspect/camel-context/lightweight")
                .then()
                .statusCode(200)
                .body(is("true"));
    }

    @Test
    public void restLog() {
        String message = "Camel Quarkus Platform HTTP";
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(message)
                .when()
                .post("/rest/log")
                .then()
                .statusCode(200)
                .body(is(message));
    }

    @Test
    public void customVerb() {
        RestAssured.given()
                .head("/rest/custom/verb")
                .then()
                .statusCode(204)
                .header("Content-Type", is(ContentType.TEXT.toString()));
    }

    @Test
    public void multiPartUpload() throws IOException {
        Path txtFile = Files.createTempFile("multipartUpload", ".txt");
        Path csvFile = Files.createTempFile("multipartUpload", ".csv");

        try {
            RestAssured.given()
                    .multiPart(txtFile.toFile())
                    .post("/rest/multipart/upload")
                    .then()
                    .statusCode(200)
                    .body(is("1"));

            // fileNameExtWhitelist config will only allow the txt file extension
            RestAssured.given()
                    .multiPart(csvFile.toFile())
                    .post("/rest/multipart/upload")
                    .then()
                    .statusCode(200)
                    .body(is("0"));
        } finally {
            Files.deleteIfExists(txtFile);
            Files.deleteIfExists(csvFile);
        }
    }

    @Test
    public void corsOptionsRequest() {
        RestAssured.given()
                .options("/rest")
                .then()
                .header("Access-Control-Allow-Credentials", equalTo("true"))
                .header("Access-Control-Allow-Headers", matchesPattern(".*Access-Control.*"))
                .header("Access-Control-Allow-Methods", equalTo("GET, POST"))
                .header("Access-Control-Allow-Origin", equalTo("*"))
                .header("Access-Control-Max-Age", equalTo("3600"))
                .statusCode(204);
    }
}
