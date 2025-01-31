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
package org.apache.camel.quarkus.component.tika.it;

import java.io.InputStream;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;

@QuarkusTest
class TikaTest {
    @Test
    public void testOffice() throws Exception {
        testParse("test.doc", "application/x-tika-msoffice", null);
    }

    @Test
    public void testImagePng() throws Exception {
        testParse("black.png", "image/png", null);
    }

    @Test
    public void testXml() throws Exception {
        testParse("quarkus.xml", "application/xml", "Hello Quarkus");
    }

    @Test
    public void testDetectDoc() throws Exception {
        testDetect("test.doc", "application/x-tika-msoffice");
    }

    @Test
    public void testDetectImagePng() throws Exception {
        testDetect("black.png", "image/png");
    }

    @Test
    public void testDetectPdf() throws Exception {
        testDetect("quarkus.pdf", "application/pdf");
    }

    //---------------------------------------------------------------------------------------------------------

    private void testParse(String fileName, String expectedContentType, String expectedBody) throws Exception {
        post(fileName, "/tika/parse")
                .header("Parsed-Content-Type", startsWith(expectedContentType))
                .body(containsStringIgnoringCase(expectedBody == null ? "<body/>" : expectedBody));
    }

    private void testDetect(String fileName, String expectedContentType) throws Exception {
        post(fileName, "/tika/detect")
                .body(is(expectedContentType));
    }

    private ValidatableResponse post(String fileName, String s) throws Exception {
        return RestAssured.given()
                .contentType(ContentType.BINARY)
                .body(readQuarkusFile(fileName))
                .post(s)
                .then()
                .statusCode(201);
    }

    private byte[] readQuarkusFile(String fileName) throws Exception {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("assets/" + fileName)) {
            if (is == null) {
                throw new IllegalStateException("Unable to read file: " + fileName);
            }
            return is.readAllBytes();
        }
    }
}
