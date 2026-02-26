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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.function.Function;
import java.util.stream.Stream;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import io.smallrye.common.os.OS;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class FopTest {

    public static final String MSG = "hello";
    private static Path tmpDir;

    @BeforeAll
    static void copyResources() throws IOException {
        /*
         * Local files are not available when this test is run in Quarkus Platform.
         * ppalaga was not able to find a way to make FOP load the font declared in mycfg.xml from the classpath
         * As a workaround, the resources are simply copied from the class loader to target/tmp directory
         * before the test starts
         */
        tmpDir = Paths.get("target/tmp");
        Files.createDirectories(tmpDir);
        final ClassLoader cl = FopTest.class.getClassLoader();
        Stream.of("Freedom-10eM.ttf", "mycfg.xml")
                .forEach(resource -> {
                    final Path target = tmpDir.resolve(resource);
                    if (!Files.exists(target)) {
                        try (InputStream in = cl.getResourceAsStream(resource)) {
                            Files.copy(in, target);
                        } catch (IOException e) {
                            throw new RuntimeException("Could not read resource " + resource, e);
                        }
                    }
                });
    }

    @BeforeEach
    public void beforeEach() {
        // Disable tests on GitHub Actions Windows runners. Font cache building is too slow and restoring saved caches is too unreliable
        Assumptions.assumeFalse(OS.current().equals(OS.WINDOWS) && "true".equals(System.getenv("CI")));
    }

    @Test
    public void convertToPdf() throws IOException {
        convertToPdf(msg -> decorateTextWithXSLFO(msg, null), null);
    }

    @Test
    void convertToPdfWithCustomFont() throws IOException {
        convertToPdf(msg -> decorateTextWithXSLFO(msg, "Freedom"),
                tmpDir.resolve("mycfg.xml").toAbsolutePath().toUri().toString());
    }

    @Test
    void convertToPdfWithArialFont() throws IOException {
        convertToPdf(msg -> decorateTextWithXSLFO(msg, "Arial"), null);
    }

    @Test
    void convertToPdfWithImage() throws Exception {
        RequestSpecification requestSpecification = RestAssured.given()
                .contentType(ContentType.XML);
        ExtractableResponse<?> response = requestSpecification
                .body(createFoContentWithBlock(
                        """
                                <fo:block>
                                          <fo:external-graphic
                                              content-width="150pt"
                                              content-height="150pt"
                                              src="url('data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAUAAAAFCAYAAACNbyblAAAAHElEQVQI12P4//8/w38GIAXDIBKE0DHxgljNBAAO9TXL0Y4OHwAAAABJRU5ErkJggg==')"/>
                                      </fo:block>
                                      """))
                .post("/fop/post")
                .then()
                .statusCode(201)
                .extract();
        PDDocument document = getDocumentFrom(response.asInputStream());
        PDPage page = document.getPage(0);
        PDResources resources = page.getResources();
        Iterator<COSName> iterator = resources.getXObjectNames().iterator();
        COSName imageName = iterator.next();
        Assertions.assertInstanceOf(PDImageXObject.class, resources.getXObject(imageName));
    }

    private void convertToPdf(Function<String, String> msgCreator, String userConfigFile) throws IOException {
        RequestSpecification requestSpecification = RestAssured.given()
                .contentType(ContentType.XML);
        if (userConfigFile != null) {
            requestSpecification.queryParam("userConfigURL", userConfigFile);
        }
        ExtractableResponse<Response> response = requestSpecification
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
                : "      <fo:block font-size=\"14pt\" font-family=\"" + font + "\">" + text + "</fo:block>\n";
        return createFoContentWithBlock(foBlock);
    }

    private static String createFoContentWithBlock(String foBlock) {
        return """
                <fo:root xmlns:fo="http://www.w3.org/1999/XSL/Format">
                   <fo:layout-master-set>
                     <fo:simple-page-master master-name="A4" page-height="29.7cm" page-width="21cm">
                       <fo:region-body region-name="xsl-region-body" margin="0.7in"  padding="0" />
                       <fo:region-before region-name="xsl-region-before" extent="0.7in" />
                         <fo:region-after region-name="xsl-region-after" extent="0.7in" />
                       </fo:simple-page-master>
                     </fo:layout-master-set>
                     <fo:page-sequence master-reference="A4">
                       <fo:flow flow-name="xsl-region-body">
                 %s
                     </fo:flow>
                   </fo:page-sequence>
                 </fo:root>
                 """.formatted(foBlock);
    }

    private PDDocument getDocumentFrom(InputStream inputStream) throws IOException {
        return Loader.loadPDF(new RandomAccessReadBuffer(inputStream));
    }

    private String extractTextFrom(PDDocument document) throws IOException {
        Writer output = new StringWriter();
        PDFTextStripper stripper = new PDFTextStripper();
        stripper.writeText(document, output);
        return output.toString().trim();
    }
}
