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
package org.apache.camel.quarkus.messaging.jms;

import java.util.UUID;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.apache.camel.quarkus.component.messaging.it.AbstractMessagingTest;
import org.junit.jupiter.api.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AbstractJmsMessagingTest extends AbstractMessagingTest {

    @Test
    public void testJmsTransferExchange() {
        String message = "Test transfer message";
        RestAssured.given()
                .body(message)
                .post("/messaging/jms/transfer/exchange")
                .then()
                .statusCode(200)
                .body(is(message));
    }

    @Test
    public void testJmsTransferException() {
        RestAssured.given()
                .get("/messaging/jms/transfer/exception")
                .then()
                .statusCode(200)
                .body(is("java.lang.IllegalStateException"));
    }

    @Test
    public void testJmsMessageListenerContainerFactory() {
        String message = "Camel JMS With Custom MessageListenerContainerFactory";
        RestAssured.given()
                .body(message)
                .post("/messaging/jms/custom/message/listener/factory")
                .then()
                .statusCode(200)
                .body(is(message));
    }

    @Test
    public void testJmsDestinationResolver() {
        String message = "Camel JMS With Custom DestinationResolver";
        RestAssured.given()
                .body(message)
                .post("/messaging/jms/custom/destination/resolver")
                .then()
                .statusCode(200)
                .body(is(message));
    }

    @Test
    public void testJmsMessageConverter() {
        String result = RestAssured.given()
                .body("a test message")
                .post("/messaging/jms/custom/message/converter")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .asString();

        assertTrue(result.startsWith("converter prefix"));
        assertTrue(result.endsWith("converter suffix"));
    }

    @Test
    public void testJmsCustomDestination() {
        String message = UUID.randomUUID().toString();

        // Send a message with java.lang.String destination header
        String destinationA = "queue-" + UUID.randomUUID().toString().split("-")[0];
        RestAssured.given()
                .queryParam("isStringDestination", "true")
                .body(message)
                .post("/messaging/jms/custom/destination/{destinationName}", destinationA)
                .then()
                .statusCode(201);

        // Send a message with javax.jms.Destination destination header
        String destinationB = "queue-" + UUID.randomUUID().toString().split("-")[0];
        RestAssured.given()
                .queryParam("isStringDestination", "false")
                .body(message)
                .post("/messaging/jms/custom/destination/{destinationName}", destinationB)
                .then()
                .statusCode(201);

        // Verify messages sent to destinations
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .get("/messaging/{destinationName}", destinationA)
                .then()
                .statusCode(200)
                .body(is(message));

        RestAssured.given()
                .contentType(ContentType.TEXT)
                .get("/messaging/{destinationName}", destinationB)
                .then()
                .statusCode(200)
                .body(is(message));
    }
}
