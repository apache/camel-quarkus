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
package org.apache.camel.quarkus.component.atlasmap.it;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.apache.camel.quarkus.component.atlasmap.it.model.Account;
import org.apache.camel.quarkus.component.atlasmap.it.model.Person;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
@TestHTTPEndpoint(AtlasmapResource.class)
class AtlasmapTest {

    @Test
    void testJava2JsonWithJson() {
        Person person = new Person("foo", "bar", 35);
        given()
                .contentType(ContentType.JSON)
                .body(toJson(person))
                .when()
                .get("/json/java2json")
                .then()
                .body("name1", equalTo("foo"))
                .body("name2", equalTo("bar"))
                .body("age", equalTo(35));
    }

    @Test
    void testJson2JavaWithJson() {
        String person = "{\"name1\":\"foo\", \"name2\":\"bar\", \"age\":35}";
        given()
                .contentType(ContentType.JSON)
                .body(person)
                .when()
                .get("/json/json2java")
                .then()
                .body("firstName", equalTo("foo"))
                .body("lastName", equalTo("bar"))
                .body("age", equalTo(35));
    }

    @Test
    void testXml2XmlWithJson() {
        String request = "<tns:Patient xmlns:tns=\"http://hl7.org/fhir\"><tns:id value=\"101138\"></tns:id></tns:Patient>";
        String expectedResponse = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?><tns:Person xmlns:tns=\"http://hl7.org/fhir\"><tns:id value=\"101138\"/></tns:Person>";
        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .get("/json/xml2xml")
                .then()
                .body(equalTo(expectedResponse));
    }

    @Test
    void testJson2XmlWithJson() {
        String request = "{\"id\":\"101138\"}";
        String expectedResponse = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?><tns:Person xmlns:tns=\"http://hl7.org/fhir\"><tns:id value=\"101138\"/></tns:Person>";
        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .get("/json/json2xml")
                .then()
                .body(equalTo(expectedResponse));
    }

    @Test
    void testXml2JsonWithJson() {
        String request = "<tns:Patient xmlns:tns=\"http://hl7.org/fhir\"><tns:id value=\"101138\"></tns:id></tns:Patient>";
        String expectedResponse = "{\"id\":\"101138\"}";
        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .get("/json/xml2json")
                .then()
                .body(equalTo(expectedResponse));
    }

    @Test
    void testJava2XmlWithJson() {
        Person request = new Person("foo", "bar", 35);
        String expectedResponse = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?><tns:Person xmlns:tns=\"http://hl7.org/fhir\"><tns:firstName value=\"foo\"/><tns:lastName value=\"bar\"/><tns:age value=\"35\"/></tns:Person>";
        given()
                .contentType(ContentType.JSON)
                .body(toJson(request))
                .when()
                .get("/json/java2xml")
                .then()
                .body(equalTo(expectedResponse));
    }

    static String toJson(Object request) {
        try {
            return new ObjectMapper().writeValueAsString(request);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Could not serialize " + request.getClass().getName() + " " + request, e);
        }
    }

    @Test
    void testXml2JavaWithJson() {
        String request = "<tns:Person xmlns:tns=\"http://hl7.org/fhir\"><tns:firstName value=\"foo\"/><tns:lastName value=\"bar\"/><tns:age value=\"35\"/></tns:Person>";
        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .get("/json/xml2java")
                .then()
                .body("firstName", equalTo("foo"))
                .body("lastName", equalTo("bar"))
                .body("age", equalTo(35));
    }

    @Test
    void testXml2XmlWithAdm() {
        String request = "<tns:Patient xmlns:tns=\"http://hl7.org/fhir\"><tns:id>101138</tns:id></tns:Patient>";
        String expectedResponse = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?><tns:Person xmlns:tns=\"http://hl7.org/fhir\"><tns:id>101138</tns:id></tns:Person>";
        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .get("/adm/xml2xml")
                .then()
                .body(equalTo(expectedResponse));
    }

    @Test
    void testJson2JsonWithAdm() {
        String request = "{\"name1\":\"foo\", \"name2\":\"bar\", \"age\":35}";
        String expectedResponse = "{\"age\":35,\"firstName\":\"foo\",\"lastName\":\"bar\"}";

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .get("/adm/json2json")
                .then()
                .body(equalTo(expectedResponse));
    }

    @Test
    void testXml2JsonWithAdm() {
        String request = "<tns:Patient xmlns:tns=\"http://hl7.org/fhir\"><tns:id>101138</tns:id></tns:Patient>";
        String expectedResponse = "{\"id\":\"101138\"}";
        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .get("/adm/xml2json")
                .then()
                .body(equalTo(expectedResponse));
    }

    @Test
    void testJson2XmlWithAdm() {
        String request = "{\"id\":\"101138\"}";
        String expectedResponse = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?><tns:Patient xmlns:tns=\"http://hl7.org/fhir\"><tns:id>101138</tns:id></tns:Patient>";
        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .get("/adm/json2xml")
                .then()
                .body(equalTo(expectedResponse));
    }

    @Test
    void testJson2CsvWithJson() {
        String person = "{\"name1\":\"foo\", \"name2\":\"bar\", \"age\":35}";
        String experctedResult = "firstName,lastName,age\r\n" +
                "foo,bar,35\r\n";
        String result = given()
                .contentType(ContentType.JSON)
                .body(person)
                .when()
                .get("/json/json2csv")
                .then()
                .extract()
                .asString();
        assertEquals(experctedResult, result);
    }

    @Test
    void testCsv2JsonWithAdm() {
        String request = "foo,bar,35\r\n";
        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .get("/adm/csv2json")
                .then()
                .body(equalTo("{\"firstName\":\"foo\",\"lastName\":\"bar\",\"age\":35}"));
    }

    @Test
    void testCsv2JsonWithJson() {
        String request = "firstName,lastName,age\r\n" +
                "foo,bar,35\r\n";
        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .get("/json/csv2json")
                .then()
                .body(equalTo("{\"firstName\":\"foo\",\"lastName\":\"bar\",\"age\":\"35\"}"));
    }

    @Test
    void testCsv2JavaWithJson() {
        String request = "id,userName\r\n" +
                "1,user\r\n";
        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/json/csv2java")
                .then()
                .body("id", equalTo("1"))
                .body("userName", equalTo("user"));
    }

    @Test
    void testCsv2XmlWithJson() {
        String request = "firstName,lastName,age\r\n" +
                "foo,bar,35\r\n";
        String expectedResult = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?><tns:Person xmlns:tns=\"http://hl7.org/fhir\"><tns:firstName value=\"foo\"/><tns:lastName value=\"bar\"/><tns:age value=\"35\"/></tns:Person>";
        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .get("/json/csv2xml")
                .then()
                .body(equalTo(expectedResult));
    }

    @Test
    void testXml2CsvWithJson() {
        String request = "<tns:Person xmlns:tns=\"http://hl7.org/fhir\"><tns:firstName value=\"foo\"/><tns:lastName value=\"bar\"/><tns:age value=\"35\"/></tns:Person>";
        String expectedResult = "firstName,lastName,age\r\n" +
                "foo,bar,35\r\n";
        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .get("/json/xml2csv")
                .then()
                .body(equalTo(expectedResult));
    }

    @Test
    void testJava2CsvWithJson() {
        String expectedResult = "id,userName\r\n" +
                "1,user\r\n";
        Account person = new Account("1", "user");
        given()
                .contentType(ContentType.JSON)
                .body(toJson(person))
                .when()
                .post("/json/java2csv")
                .then()
                .body(equalTo(expectedResult));
    }

}
