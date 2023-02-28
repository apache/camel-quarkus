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
package org.apache.camel.quarkus.component.telegram.it;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.apache.camel.component.telegram.model.EditMessageLiveLocationMessage;
import org.apache.camel.component.telegram.model.MessageResult;
import org.apache.camel.component.telegram.model.SendLocationMessage;
import org.apache.camel.component.telegram.model.SendVenueMessage;
import org.apache.camel.component.telegram.model.StopMessageLiveLocationMessage;
import org.apache.camel.quarkus.test.TrustStoreResource;
import org.apache.camel.quarkus.test.wiremock.MockServer;
import org.awaitility.Awaitility;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.request;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;

@QuarkusTest
@QuarkusTestResource(TrustStoreResource.class)
@QuarkusTestResource(TelegramTestResource.class)
public class TelegramTest {

    private static final Logger LOG = Logger.getLogger(TelegramTest.class);

    @MockServer
    WireMockServer server;

    @Test
    public void postText() {
        final String uuid = UUID.randomUUID().toString().replace("-", "");
        final String msg = "A message from camel-quarkus-telegram"
                + (System.getenv("TELEGRAM_AUTHORIZATION_TOKEN") != null ? " " + uuid : "");

        /* Send a message */
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(msg)
                .post("/telegram/messages")
                .then()
                .statusCode(201);

    }

    @Test
    public void getText() {
        // Manually stub the getUpdates response as the addition of the offset query param seems to confuse WireMock
        if (server != null) {
            server.stubFor(request("GET", urlPathMatching("/.*/getUpdates"))
                    .withQueryParam("limit", equalTo("100"))
                    .withQueryParam("timeout", equalTo("30"))
                    .willReturn(aResponse()
                            .withHeader("Content-Type", "application/json")
                            .withBody(
                                    "{\"ok\":true,\"result\":[{\"update_id\":123488937,\n\"message\":{\"message_id\":37,\"from\":{\"id\":1426416050,\"is_bot\":false,\"first_name\":\"Apache\",\"last_name\":\"Camel\",\"language_code\":\"en\"},\"chat\":{\"id\":1426416050,\"first_name\":\"Apache\",\"last_name\":\"Camel\",\"type\":\"private\"},\"date\":1604406332,\"text\":\"test\"}}]}")));
        }

        /* Telegram bots by design see neither their own messages nor other bots' messages.
         * So receiving messages is currently possible only if you ping the bot manually.
         * If you do so, you should see your messages in the test log. */
        for (int i = 0; i < 5; i++) { // For some reason several iterations are needed to pick the messages
            final String body = RestAssured.get("/telegram/messages")
                    .then()
                    .statusCode(is(both(greaterThanOrEqualTo(200)).and(lessThan(300))))
                    .extract().body().asString();
            LOG.info("Telegram Bot received messages: " + body);
        }
    }

    @Test
    public void png() throws IOException {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("camel-quarkus-rocks.png")) {
            /* Send a message */
            RestAssured.given()
                    .contentType("image/png")
                    .body(in)
                    .post("/telegram/media")
                    .then()
                    .statusCode(201);
        }
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "TELEGRAM_AUTHORIZATION_TOKEN", matches = ".+") //https://github.com/apache/camel-quarkus/issues/4513
    public void mp3() throws IOException {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("camel-quarkus-rocks.mp3")) {
            /* Send a message */
            RestAssured.given()
                    .contentType("audio/mpeg")
                    .body(in)
                    .post("/telegram/media")
                    .then()
                    .statusCode(201);
        }
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "TELEGRAM_AUTHORIZATION_TOKEN", matches = ".+") //https://github.com/apache/camel-quarkus/issues/4513
    public void mp4() throws IOException {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("camel-quarkus-rocks.mp4")) {
            /* Send a message */
            RestAssured.given()
                    .contentType("video/mp4")
                    .body(in)
                    .post("/telegram/media")
                    .then()
                    .statusCode(201);
        }
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "TELEGRAM_AUTHORIZATION_TOKEN", matches = ".+") //https://github.com/apache/camel-quarkus/issues/4513
    public void pdf() throws IOException {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("camel-quarkus-rocks.pdf")) {
            /* Send a message */
            RestAssured.given()
                    .contentType("application/pdf")
                    .body(in)
                    .post("/telegram/media")
                    .then()
                    .statusCode(201);
        }
    }

    @Test
    public void location() throws IOException {

        final SendLocationMessage sendLoc = new SendLocationMessage(29.974834, 31.138577);
        sendLoc.setLivePeriod(120);
        final MessageResult result = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(sendLoc)
                .post("/telegram/send-location")
                .then()
                .statusCode(201)
                .extract().body().as(MessageResult.class);

        /* Update the location */
        final EditMessageLiveLocationMessage edit = new EditMessageLiveLocationMessage(29.974928, 31.131115);
        edit.setChatId(result.getMessage().getChat().getId());
        edit.setMessageId(result.getMessage().getMessageId());
        /* The edit fails with various 400 errors unless we wait a bit */
        Awaitility.await()
                .pollDelay(500, TimeUnit.MILLISECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .atMost(10, TimeUnit.SECONDS).until(() -> {
                    final int code = RestAssured.given()
                            .contentType(ContentType.JSON)
                            .body(edit)
                            .post("/telegram/edit-location")
                            .then()
                            .extract().statusCode();
                    return code == 201;
                });

        /* Stop updating */
        final StopMessageLiveLocationMessage stop = new StopMessageLiveLocationMessage();
        stop.setChatId(result.getMessage().getChat().getId());
        stop.setMessageId(result.getMessage().getMessageId());
        // Poll until success as there's some potential for HTTP 400 responses to sometimes be returned
        Awaitility.await()
                .pollDelay(500, TimeUnit.MILLISECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .atMost(10, TimeUnit.SECONDS).until(() -> {
                    final int code = RestAssured.given()
                            .contentType(ContentType.JSON)
                            .body(stop)
                            .post("/telegram/stop-location")
                            .then()
                            .extract().statusCode();
                    return code == 201;
                });
    }

    @Test
    public void venue() throws IOException {
        final SendVenueMessage venue = new SendVenueMessage(29.977818, 31.136329, "Pyramid of Queen Henutsen",
                "El-Hussein Ibn Ali Ln, Nazlet El-Semman, Al Haram, Giza Governorate, Egypt");
        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(venue)
                .post("/telegram/venue")
                .then()
                .statusCode(201);
    }

}
