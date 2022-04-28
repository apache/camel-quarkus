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
package org.apache.camel.quarkus.component.azure.storage.queue.it;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.apache.camel.quarkus.test.support.azure.AzureStorageTestResource;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.org.awaitility.Awaitility;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@QuarkusTest
@QuarkusTestResource(AzureStorageTestResource.class)
class AzureStorageQueueTest {

    @Test
    public void crud() {
        try {
            String message = "Hello Camel Quarkus Azure Queue ";

            // Create
            given()
                    .contentType(ContentType.TEXT)
                    .post("/azure-storage-queue/queue/create")
                    .then()
                    .statusCode(201);

            // create 2 messages
            for (int i = 1; i < 2; i++) {
                addMessage(message + i);
            }

            // peek one message
            RestAssured.get("/azure-storage-queue/queue/peek")
                    .then()
                    .statusCode(200)
                    .body(is(message + "1"));

            // Read 2 messages
            List<LinkedHashMap<String, String>> response = null;
            for (int i = 1; i < 2; i++) {
                response = readMessage();
                assertNotNull(response);
                assertEquals(1, response.size());
                assertNotNull(response.get(0));
                assertEquals(message + i, response.get(0).get("body"));
            }

            // updating message
            // get needed informations from last message
            var id = response.get(0).get("id");
            var popReceipt = response.get(0).get("popReceipt");

            message = "Update Camel Quarkus example message";
            given()
                    .contentType(ContentType.TEXT)
                    .body(message)
                    .post(String.format("/azure-storage-queue/queue/update/%s/%s", id, popReceipt))
                    .then()
                    .statusCode(201);

            // reading update message
            response = readMessage();
            assertNotNull(response);
            assertNotNull(response.get(0));
            assertEquals(message, response.get(0).get("body"));

            // list queues
            RestAssured.given()
                    .contentType(ContentType.TEXT)
                    .get("/azure-storage-queue/queue/list")
                    .then()
                    .statusCode(200)
                    .body(containsString("camel-quarkus"));

            // adding another message to the queue
            addMessage(message);

            // clear queue
            given()
                    .get("/azure-storage-queue/queue/clear")
                    .then()
                    .statusCode(204);

            // Read and make sure the queue was cleared
            response = readMessage();
            assertNotNull(response);
            assertEquals(0, response.size());

            // adding new message
            addMessage(message);

            // peek latest message
            response = readMessage();
            id = response.get(0).get("id");
            popReceipt = response.get(0).get("popReceipt");

            // delete message by id
            RestAssured.delete("/azure-storage-queue/queue/delete/" + id + "/" + popReceipt)
                    .then()
                    .statusCode(204);

            // consumer test

            // start consumer Route
            given()
                    .post("/azure-storage-queue/queue/consumer/start")
                    .then()
                    .statusCode(204);

            // add message
            addMessage("Testing consumer");

            // verify message is consumed
            Awaitility.await().pollInterval(1, TimeUnit.SECONDS).atMost(10, TimeUnit.SECONDS).until(() -> {
                final String body = RestAssured.given()
                        .get("/azure-storage-queue/queue/consumer")
                        .then()
                        .extract().body().asString();
                return body != null && body.contains("Testing consumer");
            });

            // stop consumer Route
            given()
                    .post("/azure-storage-queue/queue/consumer/stop")
                    .then()
                    .statusCode(204);

        } finally {
            // Delete
            RestAssured.delete("/azure-storage-queue/queue/delete")
                    .then()
                    .statusCode(204);
        }
    }

    private void addMessage(String message) {
        given()
                .contentType(ContentType.TEXT)
                .body(message)
                .post("/azure-storage-queue/queue/message")
                .then()
                .statusCode(201);
    }

    @SuppressWarnings("unchecked")
    private List<LinkedHashMap<String, String>> readMessage() {
        return (List<LinkedHashMap<String, String>>) given()
                .contentType(ContentType.JSON)
                .when()
                .get("/azure-storage-queue/queue/read")
                .as(List.class);
    }

}
