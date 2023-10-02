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
package org.apache.camel.quarkus.component.xml.jaxp;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class XmlJaxpTest {
    private static final String XML_PAYLOAD = "<root><foo>Foo Text</foo></root>";
    private static final String XML_PAYLOAD_WITH_UMLAUT = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?><root><foo>Foo Text With Umlaut \u00E4\u00F6\u00FC</foo></root>";

    @Test
    void documentConversion() {
        RestAssured.given()
                .queryParam("endpointUri", "direct:documentConvert")
                .contentType("application/xml")
                .body(XML_PAYLOAD)
                .post("/xml/jaxp/convert")
                .then()
                .statusCode(200)
                .body(
                        "root.foo", equalTo("Foo Text"),
                        "root.bar", equalTo("Bar Text"));
    }

    @Test
    void elementConversion() {
        RestAssured.given()
                .queryParam("endpointUri", "direct:elementConvert")
                .contentType("application/xml")
                .body(XML_PAYLOAD)
                .post("/xml/jaxp/convert")
                .then()
                .statusCode(200)
                .body(
                        "root.foo", equalTo("Foo Text"),
                        "root.bar", equalTo("Bar Text"));
    }

    @Test
    void byteSourceConversion() {
        RestAssured.given()
                .queryParam("endpointUri", "direct:byteSourceConvert")
                .contentType("application/xml")
                .body(XML_PAYLOAD)
                .post("/xml/jaxp/convert")
                .then()
                .statusCode(200)
                .body(
                        "root.foo", equalTo("Foo Text"));
    }

    @Test
    void sourceConversion() {
        RestAssured.given()
                .queryParam("endpointUri", "direct:sourceConvert")
                .contentType("application/xml")
                .body(XML_PAYLOAD)
                .post("/xml/jaxp/convert")
                .then()
                .statusCode(200)
                .body(
                        "root.foo", equalTo("Foo Text"));
    }

    @Test
    void domSourceConversion() {
        RestAssured.given()
                .queryParam("endpointUri", "direct:domSourceConvert")
                .contentType("application/xml")
                .body(XML_PAYLOAD)
                .post("/xml/jaxp/convert")
                .then()
                .statusCode(200)
                .body(
                        "root.foo", equalTo("Foo Text"));
    }

    @Test
    void saxSourceConversion() {
        RestAssured.given()
                .queryParam("endpointUri", "direct:saxSourceConvert")
                .contentType("application/xml")
                .body(XML_PAYLOAD)
                .post("/xml/jaxp/convert")
                .then()
                .statusCode(200)
                .body(
                        "root.foo", equalTo("Foo Text"));
    }

    @Test
    void staxSourceConversion() {
        RestAssured.given()
                .queryParam("endpointUri", "direct:staxSourceConvert")
                .contentType("application/xml")
                .body(XML_PAYLOAD)
                .post("/xml/jaxp/convert")
                .then()
                .statusCode(200)
                .body(
                        "root.foo", equalTo("Foo Text"));
    }

    @Test
    void streamSourceConversion() {
        RestAssured.given()
                .queryParam("endpointUri", "direct:streamSourceConvert")
                .contentType("application/xml")
                .body(XML_PAYLOAD)
                .post("/xml/jaxp/convert")
                .then()
                .statusCode(200)
                .body(
                        "root.foo", equalTo("Foo Text"));
    }

    @Test
    void streamSourceReaderConversion() {
        RestAssured.given()
                .queryParam("endpointUri", "direct:streamSourceReaderConvert")
                .contentType("application/xml")
                .body(XML_PAYLOAD)
                .post("/xml/jaxp/convert")
                .then()
                .statusCode(200)
                .body(
                        "root.foo", equalTo("Foo Text"));
    }

    @Test
    void xmlStreamReaderCharsetConversion() {
        RestAssured.given()
                .queryParam("endpointUri", "direct:xmlStreamReaderCharsetConvert")
                .contentType("application/xml")
                .body(XML_PAYLOAD_WITH_UMLAUT)
                .post("/xml/jaxp/convert")
                .then()
                .statusCode(200)
                .body(
                        "root.foo", equalTo("Foo Text With Umlaut äöü"));
    }

    @Test
    void contextGlobalOptionsConversion() {
        String result = RestAssured.given()
                .queryParam("endpointUri", "direct:documentConvert")
                .contentType("application/xml")
                .body(XML_PAYLOAD)
                .post("/xml/jaxp/convert/context/global/options")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .asString();

        assertTrue(result.contains("encoding=\"UTF-8\""));
        assertTrue(result.contains("standalone=\"no\""));
    }

    @Test
    void nodeListConversion() {
        RestAssured.given()
                .queryParam("endpointUri", "direct:nodeListConvert")
                .contentType("application/xml")
                .body(XML_PAYLOAD)
                .post("/xml/jaxp/convert")
                .then()
                .statusCode(200)
                .body(
                        "foo", equalTo("Foo Text"));
    }
}
