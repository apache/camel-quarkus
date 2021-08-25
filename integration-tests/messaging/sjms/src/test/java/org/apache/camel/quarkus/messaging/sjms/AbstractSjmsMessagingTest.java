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
package org.apache.camel.quarkus.messaging.sjms;

import java.util.UUID;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.apache.camel.quarkus.component.messaging.it.AbstractMessagingTest;
import org.junit.jupiter.api.Test;

import static org.hamcrest.core.Is.is;

public class AbstractSjmsMessagingTest extends AbstractMessagingTest {

    @Test
    @Override
    public void testJmsSelector() {
        RestAssured.given()
                .get("/messaging/sjms/selector")
                .then()
                .statusCode(204);
    }

    @Test
    public void testJmsCustomDestination() {
        String message = UUID.randomUUID().toString();

        // Send a message
        String destination = "queue-" + UUID.randomUUID().toString().split("-")[0];
        RestAssured.given()
                .body(message)
                .post("/messaging/sjms/custom/destination/{destinationName}", destination)
                .then()
                .statusCode(201);

        // Verify message sent to destination
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .get("/messaging/{destinationName}", destination)
                .then()
                .statusCode(200)
                .body(is(message));
    }
}
