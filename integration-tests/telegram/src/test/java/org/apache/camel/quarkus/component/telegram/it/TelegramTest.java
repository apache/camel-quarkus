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

import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import org.jboss.logging.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.config.EncoderConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;

@QuarkusTest
@EnabledIfEnvironmentVariable(named = "TELEGRAM_AUTHORIZATION_TOKEN", matches = "[^ ]+")
public class TelegramTest {

    private static final Logger LOG = Logger.getLogger(TelegramTest.class);

    @Test
    public void message() {
        final String uuid = UUID.randomUUID().toString().replace("-", "");
        final String msg = String.format("A message from camel-quarkus-telegram %s", uuid);

        /* Send a message */
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(msg)
                .post("/telegram/messages")
                .then()
                .statusCode(201);

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

}
