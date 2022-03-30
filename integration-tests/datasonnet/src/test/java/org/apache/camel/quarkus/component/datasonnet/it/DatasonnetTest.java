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
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

@QuarkusTest
class DatasonnetTest {

    @Test
    public void testTransform() throws Exception {
        final String msg = loadResourceAsString("simpleMapping_payload.json");
        final String expected = loadResourceAsString("simpleMapping_result.json");
        final String response = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(msg)
                .post("/datasonnet/basicTransform")
                .then()
                .statusCode(201)
                .assertThat()
                .extract().asString();
        JSONAssert.assertEquals(expected, response, true);
    }

    @Test
    public void testTransformXML() throws Exception {
        final String msg = loadResourceAsString("payload.xml");
        final String expected = loadResourceAsString("readXMLExtTest.json");
        final String response = RestAssured.given()
                .contentType(ContentType.XML)
                .body(msg)
                .post("/datasonnet/transformXML")
                .then()
                .statusCode(201)
                .assertThat()
                .extract().asString();
        JSONAssert.assertEquals(expected, response, true);
    }

    @Test
    public void testTransformCSV() throws Exception {
        final String msg = loadResourceAsString("payload.csv");
        final String expected = "{\"account\":\"123\"}";
        final String response = RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(msg)
                .post("/datasonnet/transformCSV")
                .then()
                .statusCode(201)
                .assertThat()
                .extract().asString();
        JSONAssert.assertEquals(expected, response, true);
    }

    @Test
    public void testNamedImports() throws Exception {
        final String msg = "{}";
        final String expected = loadResourceAsString("namedImports_result.json");
        final String response = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(msg)
                .post("/datasonnet/namedImports")
                .then()
                .statusCode(201)
                .assertThat()
                .extract().asString();
        JSONAssert.assertEquals(expected, response, true);
    }

    @Test
    public void testExpressionLanguage() throws Exception {
        final String msg = "World";
        final String expected = "{ \"test\":\"Hello, World\"}";
        final String response = RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(msg)
                .post("/datasonnet/expressionLanguage")
                .then()
                .statusCode(201)
                .assertThat()
                .extract().asString();
        JSONAssert.assertEquals(expected, response, true);
    }

    @Test
    public void testNullInput() throws Exception {
        final String msg = "";
        final String expected = "{ \"test\":\"Hello, World\"}";
        final String response = RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(msg)
                .post("/datasonnet/nullInput")
                .then()
                .statusCode(201)
                .assertThat()
                .extract().asString();
        JSONAssert.assertEquals(expected, response, true);

        final String response2 = RestAssured.given()
                .contentType(ContentType.TEXT)
                .post("/datasonnet/nullInput")
                .then()
                .statusCode(201)
                .assertThat()
                .extract().asString();
        JSONAssert.assertEquals(expected, response2, true);
    }

    @Test
    public void testReadJava() throws Exception {
        final String msg = "fake";
        final String expected = loadResourceAsString("javaTest.json");
        final String response = RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(msg)
                .post("/datasonnet/readJava")
                .then()
                .statusCode(201)
                .assertThat()
                .extract().asString();
        JSONAssert.assertEquals(expected, response, true);
    }

    @Test
    public void testReadJavaDatasonnetHeader() throws Exception {
        final String msg = "fake";
        final String expected = loadResourceAsString("javaTest.json");
        final String response = RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(msg)
                .post("/datasonnet/readJavaDatasonnetHeader")
                .then()
                .statusCode(201)
                .assertThat()
                .extract().asString();
        JSONAssert.assertEquals(expected, response, true);
    }

    private String loadResourceAsString(String name) throws Exception {
        InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(name);
        return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }
}
