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
package org.apache.camel.quarkus.component.twilio.it;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Note: The scenarios tested here are the only ones supported with Twilio test credentials
 *
 * https://www.twilio.com/docs/iam/test-credentials
 */
@QuarkusTestResource(TwilioTestResource.class)
@QuarkusTest
class TwilioTest {

    //@Test
    public void sendMessage() {
        String messageId = RestAssured.given()
                .body("Hello Camel Quarkus Twilio")
                .post("/twilio/message")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .asString();

        assertFalse(messageId.isEmpty());
    }

    //@Test
    public void purchasePhoneNumber() {
        String phoneNumber = RestAssured.given()
                .post("/twilio/purchase")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .asString();

        assertEquals("+15005550006", phoneNumber);
    }

    //@Test
    public void phoneCall() {
        String phoneNumber = RestAssured.given()
                .post("/twilio/call")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .asString();

        assertFalse(phoneNumber.isEmpty());
    }
}
