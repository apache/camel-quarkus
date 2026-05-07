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
package org.apache.camel.quarkus.component.google.it;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static org.hamcrest.Matchers.is;

@EnabledIfEnvironmentVariable(named = "GOOGLE_API_APPLICATION_NAME", matches = ".+")
@EnabledIfEnvironmentVariable(named = "GOOGLE_API_CLIENT_ID", matches = ".+")
@EnabledIfEnvironmentVariable(named = "GOOGLE_API_CLIENT_SECRET", matches = ".+")
@EnabledIfEnvironmentVariable(named = "GOOGLE_API_REFRESH_TOKEN", matches = ".+")
@QuarkusTest
class GoogleComponentsTest {

    @Test
    public void testGoogleCalendarComponent() {
        String summary = "Camel Quarkus Google Calendar";
        String eventText = summary += " Event";

        // Create calendar
        String calendarId = RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(summary)
                .post("/google-calendar/create")
                .then()
                .statusCode(201)
                .extract()
                .body()
                .asString();

        // Read calendar
        RestAssured.given()
                .queryParam("calendarId", calendarId)
                .get("/google-calendar/read")
                .then()
                .statusCode(200)
                .body(is(summary));

        // Create event
        String eventId = RestAssured.given()
                .contentType(ContentType.TEXT)
                .queryParam("calendarId", calendarId)
                .body(eventText)
                .post("/google-calendar/create/event")
                .then()
                .statusCode(201)
                .extract()
                .body()
                .asString();

        // Read event
        RestAssured.given()
                .queryParam("calendarId", calendarId)
                .queryParam("eventId", eventId)
                .get("/google-calendar/read/event")
                .then()
                .statusCode(200)
                .body(is(eventText));

        // Update event
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .queryParam("calendarId", calendarId)
                .queryParam("eventId", eventId)
                .patch("/google-calendar/update/event")
                .then()
                .statusCode(200);

        RestAssured.given()
                .queryParam("calendarId", calendarId)
                .queryParam("eventId", eventId)
                .get("/google-calendar/read/event")
                .then()
                .statusCode(200)
                .body(is(eventText + " Updated"));

        // Delete calendar
        RestAssured.given()
                .queryParam("calendarId", calendarId)
                .delete("/google-calendar/delete")
                .then()
                .statusCode(204);

        // Wait for calendar deletion to occur
        Awaitility.await()
                .pollDelay(500, TimeUnit.MILLISECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .atMost(10, TimeUnit.SECONDS).until(() -> {
                    final int code = RestAssured.given()
                            .queryParam("calendarId", calendarId)
                            .post("/google-calendar/read")
                            .then()
                            .extract()
                            .statusCode();
                    return code != 404;
                });
    }

    @Test
    public void testGoogleDriveComponent() {
        String title = UUID.randomUUID().toString();

        // Create
        String fileId = RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(title)
                .post("/google-drive/create")
                .then()
                .statusCode(201)
                .extract()
                .body()
                .asString();

        // Read
        RestAssured.given()
                .queryParam("fileId", fileId)
                .get("/google-drive/read")
                .then()
                .statusCode(200)
                .body(is(title));

        // Delete
        RestAssured.given()
                .queryParam("fileId", fileId)
                .delete("/google-drive/delete")
                .then()
                .statusCode(204);

        RestAssured.given()
                .queryParam("fileId", fileId)
                .get("/google-drive/read")
                .then()
                .statusCode(404);
    }

    @Test
    public void testGoogleMailComponent() {
        String message = "Hello Camel Quarkus Google Mail";

        // Create
        String messageId = RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(message)
                .post("/google-mail/create")
                .then()
                .statusCode(201)
                .extract()
                .body()
                .asString();

        // Read
        RestAssured.given()
                .queryParam("messageId", messageId)
                .get("/google-mail/read")
                .then()
                .statusCode(200)
                .body(is(message));

        // Delete
        Awaitility.await()
                .pollDelay(500, TimeUnit.MILLISECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .atMost(10, TimeUnit.SECONDS).until(() -> {
                    final int code = RestAssured.given()
                            .queryParam("messageId", messageId)
                            .delete("/google-mail/delete")
                            .then()
                            .extract()
                            .statusCode();
                    return code == 204;
                });

        RestAssured.given()
                .queryParam("messageId", messageId)
                .get("/google-mail/read")
                .then()
                .statusCode(404);
    }

    @Test
    public void testGoogleMailUpdateLabels() {
        String message = "Test message for label update";
        String customLabelName = "TestLabel_" + UUID.randomUUID().toString().substring(0, 8);

        // Create custom label to test name-to-ID resolution
        String customLabelId = RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(customLabelName)
                .post("/google-mail/label/create")
                .then()
                .statusCode(201)
                .extract()
                .body()
                .asString();

        // Create message
        String messageId = RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(message)
                .post("/google-mail/create")
                .then()
                .statusCode(201)
                .extract()
                .body()
                .asString();

        // Test DataTypeTransformer: Use label NAME (not ID)
        // Transformer should resolve label name to ID automatically
        RestAssured.given()
                .queryParam("messageId", messageId)
                .queryParam("addLabel", customLabelName)
                .patch("/google-mail/update-labels")
                .then()
                .statusCode(200);

        // Update labels - remove custom label and add INBOX (system label)
        RestAssured.given()
                .queryParam("messageId", messageId)
                .queryParam("removeLabel", customLabelName)
                .queryParam("addLabel", "INBOX")
                .patch("/google-mail/update-labels")
                .then()
                .statusCode(200);

        // Clean up message
        Awaitility.await()
                .pollDelay(2, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(15, TimeUnit.SECONDS).until(() -> {
                    final int code = RestAssured.given()
                            .queryParam("messageId", messageId)
                            .delete("/google-mail/delete")
                            .then()
                            .extract()
                            .statusCode();
                    return code == 204;
                });

        // Clean up custom label
        RestAssured.given()
                .queryParam("labelId", customLabelId)
                .delete("/google-mail/label/delete")
                .then()
                .statusCode(204);
    }

    @Test
    public void testGoogleMailDraftOperations() {
        String draftMessage = "Draft message content";
        String updatedMessage = "Updated draft message content";

        // Create draft
        String draftId = RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(draftMessage)
                .post("/google-mail/draft/create")
                .then()
                .statusCode(201)
                .extract()
                .body()
                .asString();

        // Read draft
        RestAssured.given()
                .queryParam("draftId", draftId)
                .get("/google-mail/draft/read")
                .then()
                .statusCode(200)
                .body(is(draftMessage));

        // Update draft
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .queryParam("draftId", draftId)
                .body(updatedMessage)
                .patch("/google-mail/draft/update")
                .then()
                .statusCode(200);

        // Verify update
        RestAssured.given()
                .queryParam("draftId", draftId)
                .get("/google-mail/draft/read")
                .then()
                .statusCode(200)
                .body(is(updatedMessage));

        // Send draft
        String sentMessageId = RestAssured.given()
                .queryParam("draftId", draftId)
                .post("/google-mail/draft/send")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .asString();

        // Verify draft no longer exists after sending
        RestAssured.given()
                .queryParam("draftId", draftId)
                .get("/google-mail/draft/read")
                .then()
                .statusCode(404);

        // Clean up sent message
        Awaitility.await()
                .pollDelay(2, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(15, TimeUnit.SECONDS).until(() -> {
                    final int code = RestAssured.given()
                            .queryParam("messageId", sentMessageId)
                            .delete("/google-mail/delete")
                            .then()
                            .extract()
                            .statusCode();
                    return code == 204;
                });
    }

    @Test
    public void testGoogleMailDraftWithThreadingMetadata() {
        String originalMessage = "Original message";
        String replyMessage = "Reply to original message";

        // Create original message to get messageId and threadId
        String originalMessageId = RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(originalMessage)
                .post("/google-mail/create")
                .then()
                .statusCode(201)
                .extract()
                .body()
                .asString();

        // Get the actual threadId from the original message
        String threadId = RestAssured.given()
                .queryParam("messageId", originalMessageId)
                .get("/google-mail/thread-id")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .asString();

        // Get the actual Message-ID header from the original message
        String messageIdHeader = RestAssured.given()
                .queryParam("messageId", originalMessageId)
                .get("/google-mail/message-id-header")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .asString();

        // Create draft with threading metadata
        // DataTypeTransformer should use threadId and messageId to set In-Reply-To and References headers
        String draftId = RestAssured.given()
                .contentType(ContentType.TEXT)
                .queryParam("threadId", threadId)
                .queryParam("messageId", messageIdHeader)
                .body(replyMessage)
                .post("/google-mail/draft/create")
                .then()
                .statusCode(201)
                .extract()
                .body()
                .asString();

        // Read draft to verify it was created
        RestAssured.given()
                .queryParam("draftId", draftId)
                .get("/google-mail/draft/read")
                .then()
                .statusCode(200)
                .body(is(replyMessage));

        // Send the draft
        String sentMessageId = RestAssured.given()
                .queryParam("draftId", draftId)
                .post("/google-mail/draft/send")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .asString();

        // Clean up original message
        Awaitility.await()
                .pollDelay(2, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(15, TimeUnit.SECONDS).until(() -> {
                    final int code = RestAssured.given()
                            .queryParam("messageId", originalMessageId)
                            .delete("/google-mail/delete")
                            .then()
                            .extract()
                            .statusCode();
                    return code == 204;
                });

        // Clean up sent message
        Awaitility.await()
                .pollDelay(2, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(15, TimeUnit.SECONDS).until(() -> {
                    final int code = RestAssured.given()
                            .queryParam("messageId", sentMessageId)
                            .delete("/google-mail/delete")
                            .then()
                            .extract()
                            .statusCode();
                    return code == 204;
                });
    }

    @Test
    public void testGoogleSheetsComponent() {
        String title = "Camel Quarkus Google Sheet";

        // Create
        String sheetId = RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(title)
                .post("/google-sheets/create")
                .then()
                .statusCode(201)
                .extract()
                .body()
                .asString();

        // Read
        RestAssured.given()
                .queryParam("spreadsheetId", sheetId)
                .get("/google-sheets/read")
                .then()
                .statusCode(200)
                .body(is(title));

        // Update
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .queryParam("spreadsheetId", sheetId)
                .body(title + " Updated")
                .patch("/google-sheets/update")
                .then()
                .statusCode(200);

        RestAssured.given()
                .queryParam("spreadsheetId", sheetId)
                .get("/google-sheets/read")
                .then()
                .statusCode(200)
                .body(is(title + " Updated"));

        // Delete sheet via google-drive component
        RestAssured.given()
                .queryParam("fileId", sheetId)
                .delete("/google-drive/delete")
                .then()
                .statusCode(204);

        RestAssured.given()
                .queryParam("fileId", sheetId)
                .get("/google-drive/read")
                .then()
                .statusCode(404);
    }
}
