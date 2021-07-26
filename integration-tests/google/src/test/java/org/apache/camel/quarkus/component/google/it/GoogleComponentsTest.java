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
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static org.hamcrest.Matchers.is;

@EnabledIfEnvironmentVariable(named = "GOOGLE_API_APPLICATION_NAME", matches = ".+")
@EnabledIfEnvironmentVariable(named = "GOOGLE_API_CLIENT_ID", matches = ".+")
@EnabledIfEnvironmentVariable(named = "GOOGLE_API_CLIENT_SECRET", matches = ".+")
@EnabledIfEnvironmentVariable(named = "GOOGLE_API_REFRESH_TOKEN", matches = ".+")
@QuarkusTest
class GoogleComponentsTest {

    //@Test
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

    //@Test
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

    //@Test
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

    //@Test
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
