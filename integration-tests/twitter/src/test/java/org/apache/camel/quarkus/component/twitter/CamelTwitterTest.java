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
package org.apache.camel.quarkus.component.twitter;

import java.util.UUID;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@QuarkusTest
@EnabledIfEnvironmentVariable(named = "TWITTER_CONSUMER_KEY", matches = "[a-zA-Z0-9]+")
public class CamelTwitterTest {

    @Test
    public void direct() {
        final String uuid = UUID.randomUUID().toString().replace("-", "");
        final String msg = String.format("Direct message from camel-quarkus-twitter %s", uuid);
        /* Direct message */
        RestAssured.given() //
                .contentType(ContentType.TEXT).body(msg).post("/twitter/directmessage") //
                .then().statusCode(201);

        /* Check that the above message or a message sent by a previous run of this test was polled by the consumer. */
        final int retries = 5;
        final int delayMs = 3000;
        String body = null;
        boolean passed = false;
        for (int i = 0; i < retries; i++) {
            body = RestAssured.get("/twitter/directmessage").asString();
            if (body.contains("camel-quarkus-twitter")) {
                /* test passed */
                passed = true;
                break;
            }
            if (i + 1 < retries) {
                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        if (!passed) {
            Assertions.fail("Could not find a message containing 'camel-quarkus-twitter' in user's direct messages within ~"
                    + (retries * delayMs) + " ms; got messages: " + body);
        }
    }

    @Test
    public void e2e() {

        final String uuid = UUID.randomUUID().toString().replace("-", "");
        final String msg = String.format("Hello from camel-quarkus-twitter %s", uuid);
        final String expectedMessage = ") " + msg;

        /* Post a message */
        final String messageId = RestAssured.given().contentType(ContentType.TEXT).body(msg).post("/twitter/timeline") //
                .then().statusCode(201).body(StringContains.containsString(msg)).extract().header("messageId");
        final long sinceId = Long.parseLong(messageId) - 1;
        /* Check that the message is seen in the timeline by the polling consumer */
        {
            final int retries = 5;
            final int delayMs = 3000;
            String body = null;
            boolean passed = false;
            for (int i = 0; i < retries; i++) {
                body = RestAssured.given().param("sinceId", sinceId).get("/twitter/timeline").asString();
                if (body.contains(expectedMessage)) {
                    /* test passed */
                    passed = true;
                    break;
                }
                if (i + 1 < retries) {
                    try {
                        Thread.sleep(delayMs);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
            if (!passed) {
                Assertions.fail("Could not find the expected message '" + expectedMessage + "' in user's timeline within ~"
                        + (retries * delayMs) + " ms; got messages: " + body);
            }
        }

        /*
         * Check that the message we posted above or a message posted by this test in the past can be found via twitter search
         */
        {
            final int retries = 4;
            final int delayMs = 10000;
            String body = null;
            boolean passed = false;
            for (int i = 0; i < retries; i++) {
                body = RestAssured.given()
                        .param("keywords", "camel-quarkus-twitter")
                        .get("/twitter/search").asString();
                if (body.contains("camel-quarkus-twitter")) {
                    /* test passed */
                    passed = true;
                    break;
                }
                if (i + 1 < retries) {
                    try {
                        Thread.sleep(delayMs);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
            if (!passed) {
                Assertions.fail("Could not find the expected message '" + uuid + "' via twitter-search within "
                        + (retries * delayMs) + " ms; got messages: " + body);
            }
        }
    }

}
