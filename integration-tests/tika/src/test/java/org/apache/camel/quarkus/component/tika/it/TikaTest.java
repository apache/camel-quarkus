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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsString;
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
    public void testPdf() throws Exception {
        testParse("quarkus.pdf", "application/pdf", "Hello Quarkus");
    }

    @Test
    public void testPdfWithIdentityHFont() throws Exception {
        // PDType0Font embeds the font with the Identity-H encoding, a predefined CMap which FontBox loads by name
        try (PDDocument doc = new PDDocument();
                InputStream font = TikaTest.class
                        .getResourceAsStream("/org/apache/pdfbox/resources/ttf/LiberationSans-Regular.ttf")) {
            testParsePdf(createPdf(doc, PDType0Font.load(doc, font), "Hello Identity-H"), "Hello Identity-H");
        }
    }

    @Test
    public void testPdfWithNonEmbeddedFont() throws Exception {
        // A standard 14 font is not embedded, PDFBox reads its metrics from the AFM files in its resources
        try (PDDocument doc = new PDDocument()) {
            PDFont helvetica = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            testParsePdf(createPdf(doc, helvetica, "Hello Helvetica"), "Hello Helvetica");
        }
    }

    @Test
    public void testEncryptedPdf() throws Exception {
        // Only an owner password, so Tika opens the document with the empty user password
        try (PDDocument doc = new PDDocument()) {
            StandardProtectionPolicy policy = new StandardProtectionPolicy("owner", "", new AccessPermission());
            policy.setEncryptionKeyLength(256);
            doc.protect(policy);
            PDFont helvetica = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            testParsePdf(createPdf(doc, helvetica, "Hello encrypted"), "Hello encrypted");
        }
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

    private void testParsePdf(byte[] pdf, String expectedText) {
        RestAssured.given()
                .contentType(ContentType.BINARY)
                .body(pdf)
                .post("/tika/parse")
                .then()
                .statusCode(201)
                .header("Parsed-Content-Type", startsWith("application/pdf"))
                .body(containsString(expectedText));
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

    private static byte[] createPdf(PDDocument doc, PDFont font, String text) throws IOException {
        PDPage page = new PDPage();
        doc.addPage(page);
        try (PDPageContentStream content = new PDPageContentStream(doc, page)) {
            content.beginText();
            content.setFont(font, 12);
            content.newLineAtOffset(50, 700);
            content.showText(text);
            content.endText();
        }
        ByteArrayOutputStream pdf = new ByteArrayOutputStream();
        doc.save(pdf);
        return pdf.toByteArray();
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
