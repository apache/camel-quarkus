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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.apache.camel.quarkus.component.mail.CamelRoute.EMAIL_ADDRESS;
import static org.apache.camel.quarkus.component.mail.CamelRoute.PASSWORD;
import static org.apache.camel.quarkus.component.mail.CamelRoute.USERNAME;
import static org.hamcrest.core.Is.is;

@QuarkusTest
@QuarkusTestResource(MailTestResource.class)
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

    @BeforeEach
    public void beforeEach() {
        // Configure users
        Config config = ConfigProvider.getConfig();
        String userJson = String.format("{ \"email\": \"%s\", \"login\": \"%s\", \"password\": \"%s\"}", EMAIL_ADDRESS,
                USERNAME, PASSWORD);
        RestAssured.given()
                .port(config.getValue("mail.api.port", Integer.class))
                .contentType(ContentType.JSON)
                .body(userJson)
                .post("/api/user")
                .then()
                .statusCode(200);
    }

    @AfterEach
    public void afterEach() {
        // Clear mailboxes
        Config config = ConfigProvider.getConfig();
        RestAssured.given()
                .port(config.getValue("mail.api.port", Integer.class))
                .post("/api/service/reset")
                .then()
                .statusCode(200)
                .body("message", is("Performed reset"));
    }

    @Test
    public void sendSmtp() {
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .queryParam("subject", "Hello World")
                .queryParam("from", "camel@localhost")
                .queryParam("to", EMAIL_ADDRESS)
                .body("Hi how are you")
                .post("/mail/send")
                .then()
                .statusCode(204);

        // Need to receive using pop3 as there is no smtp consumer
        RestAssured.given()
                .get("/mail/inbox/pop3")
                .then()
                .statusCode(200)
                .body(
                        "subject", is("Hello World"),
                        "content", is("Hi how are you"));
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
                .post("/mail/mimeMultipartUnmarshalMarshal")
                .then()
                .statusCode(200)
                .extract().body().asString();

        assertMultipart(EXPECTED_TEMPLATE, unmarshalMarshal);
    }

    @Test
    public void sendSmtpAttachments() throws IOException {
        String mailBodyContent = "Test mail content";
        String attachmentContent = "Attachment " + mailBodyContent;
        java.nio.file.Path attachmentPath = Files.createTempFile("cq-attachment", ".txt");
        Files.write(attachmentPath, attachmentContent.getBytes(StandardCharsets.UTF_8));

        try {
            RestAssured.given()
                    .contentType(ContentType.TEXT)
                    .queryParam("subject", "Test attachment message")
                    .queryParam("from", "camel@localhost")
                    .queryParam("to", EMAIL_ADDRESS)
                    .body(mailBodyContent)
                    .post("/mail/send/attachment/{fileName}", attachmentPath.toAbsolutePath().toString())
                    .then()
                    .statusCode(204);

            RestAssured.given()
                    .get("/mail/inbox/pop3")
                    .then()
                    .statusCode(200)
                    .body(
                            "subject", is("Test attachment message"),
                            "content", is(mailBodyContent),
                            "attachments[0].attachmentFilename", is(attachmentPath.getFileName().toString()),
                            "attachments[0].attachmentContent", is(attachmentContent));
        } finally {
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
