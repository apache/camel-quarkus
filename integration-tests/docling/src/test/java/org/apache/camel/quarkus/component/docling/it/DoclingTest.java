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
package org.apache.camel.quarkus.component.docling.it;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.StringStartsWith.startsWith;

@QuarkusTest
@QuarkusTestResource(DoclingTestResource.class)
class DoclingTest {

    @Test
    public void componentAvailable() {
        RestAssured.get("/docling/component/available")
                .then()
                .statusCode(200)
                .body(is("true"));
    }

    @Test
    public void testResourceDocument() {
        RestAssured.given()
                .queryParam("name", "test-document.txt")
                .when()
                .get("/docling/test/resource")
                .then()
                .statusCode(200)
                .body(containsString("Apache Camel Quarkus"))
                .body(containsString("Integration Test"));
    }

    @Test
    public void testMarkdownResourceDocument() {
        RestAssured.given()
                .queryParam("name", "test-document.md")
                .when()
                .get("/docling/test/resource")
                .then()
                .statusCode(200)
                .body(containsString("# Apache Camel Quarkus"))
                .body(containsString("## Docling Component Test"));
    }

    @Test
    public void testResourceNotFound() {
        RestAssured.given()
                .queryParam("name", "non-existent-file.txt")
                .when()
                .get("/docling/test/resource")
                .then()
                .statusCode(404)
                .body(containsString("Resource not found"));
    }

    @Test
    public void convertToMarkdown() {
        String testContent = "This is a test document for conversion.";

        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(testContent)
                .when()
                .post("/docling/convert/markdown")
                .then()
                .statusCode(200)
                .body(not(emptyString()));
    }

    @Test
    public void convertToHtml() {
        String testContent = "# Test Document\nThis is a test.";

        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(testContent)
                .when()
                .post("/docling/convert/html")
                .then()
                .statusCode(200)
                .body(not(emptyString()));
    }

    @Test
    public void convertToJson() {
        String testContent = "# Test Document\nThis is a test document for JSON conversion.";

        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(testContent)
                .when()
                .post("/docling/convert/json")
                .then()
                .statusCode(200)
                .body(not(emptyString()));
    }

    @Test
    void extractText() {
        String testContent = "Document with text to extract.";

        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(testContent)
                .when()
                .post("/docling/extract/text")
                .then()
                .statusCode(200)
                .body(containsString(testContent));
    }

    @Test
    void extractMetadataFromPdf() {
        RestAssured.given()
                .when()
                .post("/docling/metadata/extract/pdf")
                .then()
                .statusCode(200)
                .body("fileName", startsWith("docling-test"))
                .body("filePath", containsString("docling-test"));
        // TODO: improve test by checking other metadatas when https://issues.apache.org/jira/browse/CAMEL-22888 is fixed
    }

    @Test
    @Disabled("test to implement")
    void extractTextFromPassordProtectedPdf() {
    }

    @Test
    @Disabled("test to implement")
    void extractTextWithOCROnScannedDocument() {
    }

    @Test
    void extractMetadataFromMarkdown() {
        String testContent = """
                # Test Document

                Some content for metadata extraction.
                """;

        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(testContent)
                .when()
                .post("/docling/metadata/extract")
                .then()
                .statusCode(200)
                .body("fileName", startsWith("docling-test"))
                .body("filePath", containsString("docling-test"));
        // TODO: improve test by checking other metadatas when https://issues.apache.org/jira/browse/CAMEL-22888 is fixed
    }

    @Test
    public void convertToMarkdownAsync() {
        String testContent = "# Async Test\nThis is a test for async markdown conversion.";

        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(testContent)
                .when()
                .post("/docling/async/convert/markdown")
                .then()
                .statusCode(200)
                .body(not(emptyString()));
    }

    @Test
    @Disabled("test to implement")
    void convertToMarkdownAsyncInBatch() {
    }

    @Test
    public void convertToHtmlAsync() {
        String testContent = "# Async HTML Test\nThis is a test for async HTML conversion.";

        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(testContent)
                .when()
                .post("/docling/async/convert/html")
                .then()
                .statusCode(200)
                .body(not(emptyString()));
    }

    @Test
    public void convertToJsonAsync() {
        String testContent = "# Async JSON Test\nThis is a test for async JSON conversion.";

        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(testContent)
                .when()
                .post("/docling/async/convert/json")
                .then()
                .statusCode(200)
                .body(not(emptyString()));
    }
}
