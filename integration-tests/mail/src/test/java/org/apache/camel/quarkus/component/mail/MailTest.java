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
package org.apache.camel.quarkus.component.mail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.hamcrest.core.Is.is;

@QuarkusTest
public class MailTest {
    private static final Pattern DELIMITER_PATTERN = Pattern.compile("\r\n[^\r\n]+");
    private static final String EXPECTED_TEMPLATE = "${delimiter}\r\n"
            + "Content-Type: text/plain; charset=UTF8; other-parameter=true\r\n"
            + "Content-Transfer-Encoding: 8bit\r\n"
            + "\r\n"
            + "Hello multipart!"
            + "${delimiter}\r\n"
            + "Content-Type: text/plain\r\n"
            + "Content-Transfer-Encoding: 8bit\r\n"
            + "Content-Disposition: attachment; filename=file.txt\r\n"
            + "Content-Description: Sample Attachment Data\r\n"
            + "X-AdditionalData: additional data\r\n"
            + "\r\n"
            + "Hello attachment!"
            + "${delimiter}--\r\n";

    @AfterEach
    public void afterEach() {
        // Clear mock mailbox
        RestAssured.given()
                .delete("/mock/clear")
                .then()
                .statusCode(204);
    }

    @Test
    public void testSendAsMail() {
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body("Hi how are you")
                .post("/mail/route/mailtext")
                .then()
                .statusCode(200);

        RestAssured.given()
                .get("/mock/{username}/size", "james@localhost")
                .then()
                .body(is("1"));

        RestAssured.given()
                .get("/mock/{username}/{id}/content", "james@localhost", 0)
                .then()
                .body(is("Hi how are you"));

        RestAssured.given()
                .get("/mock/{username}/{id}/subject", "james@localhost", 0)
                .then()
                .body(is("Hello World"));
    }

    @Test
    public void mimeMultipartDataFormat() {
        final String actual = RestAssured.given()
                .contentType(ContentType.TEXT)
                .body("Hello multipart!")
                .post("/mail/mimeMultipartMarshal/file.txt/Hello attachment!")
                .then()
                .statusCode(200)
                .extract().body().asString();
        assertMultipart(EXPECTED_TEMPLATE, actual);

        final String unmarshalMarshal = RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(actual)
                .post("/mail/route/mimeMultipartUnmarshalMarshal")
                .then()
                .statusCode(200)
                .extract().body().asString();

        assertMultipart(EXPECTED_TEMPLATE, unmarshalMarshal);
    }

    @Test
    public void consumer() throws IOException {
        String content = "Test mail content";
        Path attachmentPath = Paths.get(
                RestAssured.given()
                        .body(content)
                        .post("/mock/{username}/create/attachments", "james@localhost")
                        .then()
                        .statusCode(200)
                        .extract()
                        .body()
                        .asString());

        try {
            // Start consumer
            RestAssured.given()
                    .post("/mail/consumer/true")
                    .then()
                    .statusCode(204);

            RestAssured.given()
                    .body(content)
                    .get("/mail/inbox")
                    .then()
                    .statusCode(200)
                    .body(
                            "subject", is("Test attachment message"),
                            "content", is(content),
                            "attachmentFilename", is(attachmentPath.getFileName().toString()),
                            "attachmentContent", is("Attachment " + content));
        } finally {
            // Stop consumer
            RestAssured.given()
                    .post("/mail/consumer/false")
                    .then()
                    .statusCode(204);

            // Clean up temporary files
            Files.deleteIfExists(attachmentPath);
        }
    }

    private void assertMultipart(final String expectedPattern, final String actual) {
        final Matcher m = DELIMITER_PATTERN.matcher(actual);
        if (!m.find()) {
            Assertions.fail("Mime delimiter not found in body: " + actual);
        }
        final String delim = m.group();
        final String expected = expectedPattern.replace("${delimiter}", delim);
        Assertions.assertEquals(expected, actual);
    }

}
