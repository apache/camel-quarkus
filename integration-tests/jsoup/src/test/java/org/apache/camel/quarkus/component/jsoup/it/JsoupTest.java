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
package org.apache.camel.quarkus.component.jsoup.it;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

@QuarkusTest
class JsoupTest {

    @Test
    void testHtmlClean() {
        String maliciousHtml = "<p><a href='https://example.com/' onclick='stealCookies()'>Link</a></p>";

        RestAssured.given()
                .contentType(ContentType.HTML)
                .body(maliciousHtml)
                .post("/jsoup/clean")
                .then()
                .statusCode(200)
                .body(containsString("<a href=\"https://example.com/\""))
                .body(containsString("Link"))
                .body(not(containsString("onclick")));
    }

    @Test
    void testHtmlDecode() {
        String html = "<html><title>My Camel</title><body><p>Some blah blah</p></body></html>";

        RestAssured.given()
                .contentType(ContentType.HTML)
                .body(html)
                .post("/jsoup/decode")
                .then()
                .statusCode(200)
                .body(equalTo("My Camel Some blah blah"));
    }

    @Test
    void testHtmlParseTitle() {
        String html = "<html><title>Test Title</title><body><p>Content</p></body></html>";

        RestAssured.given()
                .contentType(ContentType.HTML)
                .body(html)
                .post("/jsoup/parse-title")
                .then()
                .statusCode(200)
                .body(equalTo("Test Title"));
    }

    @Test
    void testCssSelection() {
        String html = "<html><body><p>First paragraph</p><p>Second paragraph</p></body></html>";

        RestAssured.given()
                .contentType(ContentType.HTML)
                .body(html)
                .post("/jsoup/select-css")
                .then()
                .statusCode(200)
                .body(equalTo("First paragraph Second paragraph"));
    }

    @Test
    void testHtmlCleanYaml() {
        String maliciousHtml = "<p><a href='https://example.com/' onclick='stealCookies()'>Link</a></p>";

        RestAssured.given()
                .contentType(ContentType.HTML)
                .body(maliciousHtml)
                .post("/jsoup/clean-yaml")
                .then()
                .statusCode(200)
                .body(containsString("<a href=\"https://example.com/\""))
                .body(containsString("Link"))
                .body(not(containsString("onclick")));
    }

    @Test
    void testHtmlDecodeYaml() {
        String html = "<html><title>My Camel</title><body><p>Some blah blah</p></body></html>";

        RestAssured.given()
                .contentType(ContentType.HTML)
                .body(html)
                .post("/jsoup/decode-yaml")
                .then()
                .statusCode(200)
                .body(equalTo("My Camel Some blah blah"));
    }

    @Test
    void testHtmlParseYaml() {
        String html = "<html><title>Test Title</title><body><p>Content</p></body></html>";

        RestAssured.given()
                .contentType(ContentType.HTML)
                .body(html)
                .post("/jsoup/parse-yaml")
                .then()
                .statusCode(200)
                .body(containsString("<title>Test Title</title>"))
                .body(containsString("<p>Content</p>"));
    }

    @Test
    void testHtmlCleanEmptyInput() {
        RestAssured.given()
                .contentType(ContentType.HTML)
                .body("")
                .post("/jsoup/clean")
                .then()
                .statusCode(200)
                .body(equalTo(""));
    }

    @Test
    void testHtmlDecodeEmptyInput() {
        RestAssured.given()
                .contentType(ContentType.HTML)
                .body("")
                .post("/jsoup/decode")
                .then()
                .statusCode(200)
                .body(equalTo(""));
    }

    @Test
    void testHtmlParseNullBody() {
        String html = "<html><body></body></html>";

        RestAssured.given()
                .contentType(ContentType.HTML)
                .body(html)
                .post("/jsoup/parse-title")
                .then()
                .statusCode(200)
                .body(equalTo(""));
    }
}
