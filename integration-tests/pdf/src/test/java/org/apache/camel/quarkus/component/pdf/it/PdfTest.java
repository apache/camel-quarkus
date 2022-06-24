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
package org.apache.camel.quarkus.component.pdf.it;

import java.io.IOException;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class PdfTest {

    @Order(1)
    @Test
    public void createFromTextShouldReturnANewPdfDocument() throws IOException {
        byte[] bytes = RestAssured.given().contentType(ContentType.TEXT)
                .body("content to be included in the created pdf document").post("/pdf/createFromText").then().statusCode(201)
                .extract().asByteArray();

        PDDocument doc = PDDocument.load(bytes);
        PDFTextStripper pdfTextStripper = new PDFTextStripper();
        String text = pdfTextStripper.getText(doc);
        assertEquals(1, doc.getNumberOfPages());
        assertTrue(text.contains("content to be included in the created pdf document"));
        doc.close();
    }

    @Order(2)
    @Test
    public void appendTextShouldReturnAnUpdatedPdfDocument() throws IOException {
        byte[] bytes = RestAssured.given().contentType(ContentType.TEXT).body("another line that should be appended")
                .put("/pdf/appendText").then().statusCode(200).extract().asByteArray();

        PDDocument doc = PDDocument.load(bytes);
        PDFTextStripper pdfTextStripper = new PDFTextStripper();
        String text = pdfTextStripper.getText(doc);
        assertEquals(2, doc.getNumberOfPages());
        assertTrue(text.contains("content to be included in the created pdf document"));
        assertTrue(text.contains("another line that should be appended"));
        doc.close();
    }

    @Order(3)
    @Test
    public void extractTextShouldReturnUpdatedText() {
        String pdfText = RestAssured.get("/pdf/extractText").then().statusCode(200).extract().asString();

        assertTrue(pdfText.contains("content to be included in the created pdf document"));
        assertTrue(pdfText.contains("another line that should be appended"));
    }

    @Test
    public void encryptDecrypt() throws IOException {
        final String ownerPassword = "p4ssw0rd";
        final String userPassword = "2s3cr3t!";

        // Encrypt
        byte[] bytes = RestAssured.given()
                .contentType(ContentType.TEXT)
                .queryParam("ownerPassword", ownerPassword)
                .queryParam("userPassword", userPassword)
                .body("content to be included in the created pdf document")
                .post("/pdf/encrypt/standard")
                .then()
                .statusCode(201)
                .extract()
                .asByteArray();

        // Test loading the PDF without credentials
        Assertions.assertThrows(InvalidPasswordException.class, () -> PDDocument.load(bytes));

        // Decrypt
        RestAssured.given()
                .contentType(ContentType.BINARY)
                .queryParam("password", ownerPassword)
                .body(bytes)
                .post("/pdf/decrypt/standard")
                .then()
                .statusCode(200)
                .body(containsString("content to be included in the created pdf document"));
    }
}
