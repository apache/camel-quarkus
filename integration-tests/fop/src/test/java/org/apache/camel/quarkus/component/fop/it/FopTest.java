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
package org.apache.camel.quarkus.component.fop.it;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.util.function.Function;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ExtractableResponse;
import io.restassured.specification.RequestSpecification;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
class FopTest {

    public static final String MSG = "hello";

    @Test
    public void convertToPdf() throws IOException {
        convertToPdf(msg -> decorateTextWithXSLFO(msg, null), null);
    }

    @Test
    public void convertToPdfWithCustomFont() throws IOException {
        convertToPdf(msg -> decorateTextWithXSLFO(msg, "Freedom"), "/mycfg.xml");
    }

    private void convertToPdf(Function<String, String> msgCreator, String userConfigFile) throws IOException {
        RequestSpecification requestSpecification = RestAssured.given()
                .contentType(ContentType.XML);
        if (userConfigFile != null) {
            requestSpecification.queryParam("userConfigURL", "file:" + getClass().getResource(userConfigFile).getFile());
        }
        ExtractableResponse response = requestSpecification
                .body(msgCreator.apply(MSG))
                .post("/fop/post") //
                .then()
                .statusCode(201)
                .extract();

        PDDocument document = getDocumentFrom(response.asInputStream());
        String content = extractTextFrom(document);
        assertEquals(MSG, content);
    }

    public static String decorateTextWithXSLFO(String text, String font) {
        String foBlock = font == null ? "      <fo:block>" + text + "</fo:block>\n"
                : "      <fo:block font-family=\"" + font + "\">" + text + "</fo:block>\n";
        return "<fo:root xmlns:fo=\"http://www.w3.org/1999/XSL/Format\">\n"
                + "  <fo:layout-master-set>\n"
                + "    <fo:simple-page-master master-name=\"only\">\n"
                + "      <fo:region-body region-name=\"xsl-region-body\" margin=\"0.7in\"  padding=\"0\" />\n"
                + "      <fo:region-before region-name=\"xsl-region-before\" extent=\"0.7in\" />\n"
                + "        <fo:region-after region-name=\"xsl-region-after\" extent=\"0.7in\" />\n"
                + "      </fo:simple-page-master>\n"
                + "    </fo:layout-master-set>\n"
                + "    <fo:page-sequence master-reference=\"only\">\n"
                + "      <fo:flow flow-name=\"xsl-region-body\">\n"
                + foBlock
                + "    </fo:flow>\n"
                + "  </fo:page-sequence>\n"
                + "</fo:root>";
    }

    private PDDocument getDocumentFrom(InputStream inputStream) throws IOException {
        return PDDocument.load(inputStream);
    }

    private String extractTextFrom(PDDocument document) throws IOException {
        Writer output = new StringWriter();
        PDFTextStripper stripper = new PDFTextStripper();
        stripper.writeText(document, output);
        return output.toString().trim();
    }
}
