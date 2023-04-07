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
package org.apache.camel.quarkus.component.xml.it;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

import io.quarkus.test.junit.DisabledOnIntegrationTest;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hamcrest.Matchers.is;

@QuarkusTest
public class XsltTest {
    private static final String BODY = "<mail><subject>Hey</subject><body>Hello world!</body></mail>";

    @ParameterizedTest
    @ValueSource(strings = { "string", "bytes", "dom", "file" })
    public void xslt(String output) throws Exception {
        final String actual = RestAssured.given()
                .body(BODY)
                .post("/xml/xslt?output=" + output)
                .then()
                .statusCode(200)
                .extract().body().asString().trim().replaceAll(">\\s+<", "><");

        if (output.equals("dom")) {
            Assertions.assertEquals(
                    "<classpath-xsl subject=\"Hey\"><cheese><mail><subject>Hey</subject><body>Hello world!</body></mail></cheese></classpath-xsl>",
                    actual);
        } else {
            Assertions.assertEquals(
                    "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?><classpath-xsl subject=\"Hey\"><cheese><mail><subject>Hey</subject><body>Hello world!</body></mail></cheese></classpath-xsl>",
                    actual);
        }

        if (output.equals("file")) {
            Assertions.assertEquals(
                    "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?><classpath-xsl subject=\"Hey\"><cheese><mail><subject>Hey</subject><body>Hello world!</body></mail></cheese></classpath-xsl>",
                    Files.readString(Path.of("target/xsltme.xml"), Charset.forName("ISO-8859-1")).replaceAll(">\\s+<", "><")
                            .replaceAll("[\\n\\r]", ""));
        }
    }

    @Test
    public void xsltExtensionFunction() {
        final String actual = RestAssured.given()
                .body(BODY)
                .post("/xml/xslt-extension-function")
                .then()
                .statusCode(200)
                .extract().body().asString().trim().replaceAll(">\\s+<", "><");

        Assertions.assertEquals(
                "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?><extension-function-xsl>Hey-Hello world!</extension-function-xsl>",
                actual);
    }

    @Test
    public void xsltCustomURIResolver() {
        final String actual = RestAssured.given()
                .body(BODY)
                .post("/xml/xslt-custom-uri-resolver")
                .then()
                .statusCode(200)
                .extract().body().asString().trim().replaceAll(">\\s+<", "><");

        Assertions.assertEquals(
                "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>" + XsltProducers.EXPECTED_XML_CONSTANT,
                actual);
    }

    @Test
    @DisabledOnIntegrationTest("Generating xslt templates dynamically does not be supported in native mode")
    public void xsltInclude() {
        final String actual = RestAssured.given()
                .body(BODY)
                .post("/xml/xslt_include")
                .then()
                .statusCode(200)
                .extract().body().asString().trim().replaceAll(">\\s+<", "><");

        Assertions.assertEquals(
                "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?><classpath-xsl subject=\"Hey\"><cheese><mail><subject>Hey</subject><body>Hello world!</body></mail></cheese></classpath-xsl>",
                actual);
    }

    @Test
    @DisabledOnIntegrationTest("forwarding xslt error and warn messages does not be supported in native mode")
    public void xsltTerminate() {
        RestAssured.given()
                .body("<staff><programmer><name>Daisy Duck</name><dob></dob></programmer></staff>")
                .post("/xml/xslt_terminate")
                .then()
                .statusCode(200)
                .body(is("Error: DOB is an empty string!"));
    }

    @Test
    public void htmlTransform() throws Exception {
        String html = IOUtils.toString(getClass().getResourceAsStream("/test.html"), Charset.forName("UTF-8"));

        final String actual = RestAssured.given()
                .contentType(ContentType.HTML)
                .accept(ContentType.TEXT)
                .body(html)
                .post("/xml/html-transform")
                .then()
                .statusCode(200)
                .extract().body().asString().trim().replaceAll(">\\s+<", "><");

        Assertions.assertEquals(
                "<html><head><META http-equiv=\"Content-Type\" content=\"text/html; charset=ISO-8859-1\"><title>Title</title></head><body><h1>Title</h1><p>Paragraph Contents</p></body></html>",
                actual);
    }

    @Test
    public void htmlToText() throws Exception {
        String html = IOUtils.toString(getClass().getResourceAsStream("/test.html"), Charset.forName("UTF-8"));

        final String actual = RestAssured.given()
                .contentType(ContentType.HTML)
                .accept(ContentType.TEXT)
                .body(html)
                .post("/xml/html-to-text")
                .then()
                .statusCode(200)
                .extract().body().asString().trim();

        Assertions.assertEquals(
                "= Title" + System.lineSeparator()
                        + System.lineSeparator()
                        + "Paragraph Contents",
                actual);
    }

    @Test
    public void xpathLanguage() {
        // Tests a simple xpath driven content based router
        RestAssured.given()
                .contentType(ContentType.XML)
                .body("<orders><order><id>1</id><country>UK</country><total>2.54</total></order></orders>")
                .post("/xml/xpath")
                .then()
                .statusCode(200)
                .body(is("Country UK"));

        RestAssured.given()
                .contentType(ContentType.XML)
                .body("<orders><order><id>1</id><country>FR</country><total>9.99</total></order></orders>")
                .post("/xml/xpath")
                .then()
                .statusCode(200)
                .body(is("Invalid country code"));
    }

    @Test
    public void xtokenizeLanguage() {
        String expectedResult = "<c:child some_attr='a' anotherAttr='a' xmlns:c=\"urn:c\"></c:child>,<c:child some_attr='b' anotherAttr='b' xmlns:c=\"urn:c\"></c:child>";
        String xml = "<?xml version='1.0' encoding='UTF-8'?>"
                + "<c:parent xmlns:c='urn:c'>"
                + "<c:child some_attr='a' anotherAttr='a'>"
                + "</c:child>"
                + "<c:child some_attr='b' anotherAttr='b'>"
                + "</c:child>"
                + "</c:parent>";

        // Tests a simple xpath driven content based router
        RestAssured.given()
                .contentType(ContentType.XML)
                .body(xml)
                .post("/xml/xtokenize")
                .then()
                .statusCode(200)
                .body(is(expectedResult));
    }
}
