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
package org.apache.camel.quarkus.component.datasonnet.it;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

@QuarkusTest
class DatasonnetTest {

    @Test
    public void testTransform() throws Exception {
        final String msg = loadResourceAsString("simpleMapping_payload.json");
        final JsonPath expectedJson = new JsonPath(loadResourceAsString("simpleMapping_result.json"));

        given()
                .contentType(ContentType.JSON)
                .body(msg)
                .post("/datasonnet/basicTransform")
                .then()
                .statusCode(201)
                .body("", equalTo(expectedJson.getMap("")));
    }

    @Test
    public void testTransformXML() throws Exception {
        final String msg = loadResourceAsString("payload.xml");
        final JsonPath expectedJson = new JsonPath(loadResourceAsString("readXMLExtTest.json"));
        given()
                .contentType(ContentType.XML)
                .body(msg)
                .post("/datasonnet/transformXML")
                .then()
                .statusCode(201)
                .body("", equalTo(expectedJson.getMap("")));
    }

    @Test
    public void testTransformCSV() throws Exception {
        final String msg = loadResourceAsString("payload.csv");
        final JsonPath expectedJson = new JsonPath("{\"account\":\"123\"}");
        given()
                .contentType(ContentType.TEXT)
                .body(msg)
                .post("/datasonnet/transformCSV")
                .then()
                .statusCode(201)
                .body("", equalTo(expectedJson.getMap("")));
    }

    @Test
    public void testExpressionLanguage() throws Exception {
        final String msg = "World";
        final JsonPath expectedJson = new JsonPath("{ \"test\":\"Hello, World\"}");
        given()
                .contentType(ContentType.TEXT)
                .body(msg)
                .post("/datasonnet/expressionLanguage")
                .then()
                .statusCode(201)
                .body("", equalTo(expectedJson.getMap("")));
    }

    @Test
    public void testNullInput() throws Exception {
        final String msg = "";
        final JsonPath expectedJson = new JsonPath("{ \"test\":\"Hello, World\"}");
        given()
                .contentType(ContentType.TEXT)
                .body(msg)
                .post("/datasonnet/nullInput")
                .then()
                .statusCode(201)
                .body("", equalTo(expectedJson.getMap("")));

        given()
                .contentType(ContentType.TEXT)
                .post("/datasonnet/nullInput")
                .then()
                .statusCode(201)
                .body("", equalTo(expectedJson.getMap("")));
    }

    @Test
    public void testReadJava() throws Exception {
        final String msg = "fake";
        final JsonPath expectedJson = new JsonPath(loadResourceAsString("javaTest.json"));
        given()
                .contentType(ContentType.TEXT)
                .body(msg)
                .post("/datasonnet/readJava")
                .then()
                .statusCode(201)
                .body("", equalTo(expectedJson.getMap("")));
    }

    @Test
    public void testReadJavaDatasonnetHeader() throws Exception {
        final String msg = "fake";
        final JsonPath expectedJson = new JsonPath(loadResourceAsString("javaTest.json"));
        given()
                .contentType(ContentType.TEXT)
                .body(msg)
                .post("/datasonnet/readJavaDatasonnetHeader")
                .then()
                .statusCode(201)
                .body("", equalTo(expectedJson.getMap("")));
    }

    private String loadResourceAsString(String name) throws Exception {
        InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(name);
        return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }
}
