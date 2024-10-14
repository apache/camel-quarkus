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
package org.apache.camel.quarkus.component.azure.eventhubs.it;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnabledIfEnvironmentVariable(named = "AZURE_STORAGE_ACCOUNT_NAME", matches = ".+")
@EnabledIfEnvironmentVariable(named = "AZURE_STORAGE_ACCOUNT_KEY", matches = ".+")
@EnabledIfEnvironmentVariable(named = "AZURE_EVENT_HUBS_BLOB_CONTAINER_NAME", matches = ".+")
@EnabledIfEnvironmentVariable(named = "AZURE_EVENT_HUBS_CONNECTION_STRING", matches = ".+")
@QuarkusTest
class AzureEventhubsTest {
    // NOTE: Consumer endpoints are started / stopped manually to prevent them from inferring with each other

    @Test
    void produceConsumeEvents() {
        try {
            RestAssured.given()
                    .post("/azure-eventhubs/route/eventhubs-consumer/start")
                    .then()
                    .statusCode(204);

            final String messageBody = UUID.randomUUID().toString();

            RestAssured.given()
                    .contentType(ContentType.TEXT)
                    .body(messageBody)
                    .post("/azure-eventhubs/send-event/0")
                    .then()
                    .statusCode(201);

            Awaitility.await().pollInterval(1, TimeUnit.SECONDS).atMost(1, TimeUnit.MINUTES).untilAsserted(() -> {
                ValidatableResponse response = RestAssured.given()
                        .queryParam("endpointUri", "mock:partition-0-results")
                        .body(messageBody)
                        .get("/azure-eventhubs/receive-event")
                        .then()
                        .statusCode(200)
                        .body(
                                "body", is(messageBody),
                                "headers.CamelAzureEventHubsEnqueuedTime", notNullValue(),
                                "headers.CamelAzureEventHubsPartitionId", is("0"),
                                "headers.CamelAzureEventHubsSequenceNumber", greaterThanOrEqualTo(0));
                assertThat(((Number) (response.extract().path("headers.CamelAzureEventHubsOffset"))).longValue())
                        .isGreaterThanOrEqualTo(0);
            });
        } finally {
            RestAssured.given()
                    .post("/azure-eventhubs/route/eventhubs-consumer/stop")
                    .then()
                    .statusCode(204);
        }
    }

    @Test
    void produceMultipleMessages() {
        try {
            RestAssured.given()
                    .post("/azure-eventhubs/route/eventhubs-consumer/start")
                    .then()
                    .statusCode(204);

            List<String> messages = new ArrayList<>(3);
            for (int i = 0; i < 3; i++) {
                messages.add(UUID.randomUUID().toString());
            }

            RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body(messages)
                    .post("/azure-eventhubs/send-events/1")
                    .then()
                    .statusCode(201);

            Awaitility.await().pollInterval(1, TimeUnit.SECONDS).atMost(1, TimeUnit.MINUTES).untilAsserted(() -> {
                ValidatableResponse response = RestAssured.given()
                        .contentType(ContentType.JSON)
                        .queryParam("endpointUri", "mock:partition-1-results")
                        .body(messages)
                        .get("/azure-eventhubs/receive-events")
                        .then()
                        .statusCode(200)
                        .body(
                                "size()", is(3),
                                "[0].body", is(messages.get(0)),
                                "[0].headers.CamelAzureEventHubsEnqueuedTime", notNullValue(),
                                "[0].headers.CamelAzureEventHubsPartitionId", is("1"),
                                "[0].headers.CamelAzureEventHubsSequenceNumber", greaterThanOrEqualTo(0),
                                "[1].body", is(messages.get(1)),
                                "[1].headers.CamelAzureEventHubsEnqueuedTime", notNullValue(),
                                "[1].headers.CamelAzureEventHubsPartitionId", is("1"),
                                "[1].headers.CamelAzureEventHubsSequenceNumber", greaterThanOrEqualTo(0),
                                "[2].body", is(messages.get(2)),
                                "[2].headers.CamelAzureEventHubsEnqueuedTime", notNullValue(),
                                "[2].headers.CamelAzureEventHubsPartitionId", is("1"),
                                "[2].headers.CamelAzureEventHubsSequenceNumber", greaterThanOrEqualTo(0));

                assertThat(((Number) (response.extract().path("[0].headers.CamelAzureEventHubsOffset"))).longValue())
                        .isGreaterThanOrEqualTo(0);
                assertThat(((Number) (response.extract().path("[1].headers.CamelAzureEventHubsOffset"))).longValue())
                        .isGreaterThanOrEqualTo(0);
                assertThat(((Number) (response.extract().path("[2].headers.CamelAzureEventHubsOffset"))).longValue())
                        .isGreaterThanOrEqualTo(0);
            });
        } finally {
            RestAssured.given()
                    .post("/azure-eventhubs/route/eventhubs-consumer/stop")
                    .then()
                    .statusCode(204);
        }
    }

    @Test
    void produceConsumeEventsWithCustomClient() {
        try {
            RestAssured.given()
                    .post("/azure-eventhubs/route/eventhubs-consumer-for-custom-client/start")
                    .then()
                    .statusCode(204);

            final String messageBody = UUID.randomUUID().toString();

            RestAssured.given()
                    .contentType(ContentType.TEXT)
                    .queryParam("endpointUri", "direct:sendEventUsingCustomClient")
                    .body(messageBody)
                    .post("/azure-eventhubs/send-event/0")
                    .then()
                    .statusCode(201);

            Awaitility.await().pollInterval(1, TimeUnit.SECONDS).atMost(1, TimeUnit.MINUTES).untilAsserted(() -> {
                ValidatableResponse response = RestAssured.given()
                        .queryParam("endpointUri", "mock:partition-0-custom-client-results")
                        .body(messageBody)
                        .get("/azure-eventhubs/receive-event")
                        .then()
                        .statusCode(200)
                        .body(
                                "body", is(messageBody),
                                "headers.CamelAzureEventHubsEnqueuedTime", notNullValue(),
                                "headers.CamelAzureEventHubsPartitionId", is("0"),
                                "headers.CamelAzureEventHubsSequenceNumber", greaterThanOrEqualTo(0));
                assertThat(((Number) (response.extract().path("headers.CamelAzureEventHubsOffset"))).longValue())
                        .isGreaterThanOrEqualTo(0);
            });
        } finally {
            RestAssured.given()
                    .post("/azure-eventhubs/route/eventhubs-consumer-for-custom-client/stop")
                    .then()
                    .statusCode(204);
        }
    }

    @Test
    void customEventPosition() {
        try {
            RestAssured.given()
                    .post("/azure-eventhubs/route/eventhubs-consumer-custom-checkpoint-store/start")
                    .then()
                    .statusCode(204);

            // Send some messages to partition 2
            List<String> messages = new ArrayList<>(3);
            for (int i = 0; i < 3; i++) {
                messages.add(UUID.randomUUID().toString());
            }

            RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body(messages)
                    .post("/azure-eventhubs/send-events/2")
                    .then()
                    .statusCode(201);

            Awaitility.await().pollInterval(1, TimeUnit.SECONDS).atMost(1, TimeUnit.MINUTES).untilAsserted(() -> {
                ValidatableResponse response = RestAssured.given()
                        .queryParam("endpointUri", "mock:partition-2-initial-results")
                        .contentType(ContentType.JSON)
                        .body(messages)
                        .get("/azure-eventhubs/receive-events")
                        .then()
                        .statusCode(200)
                        .body(
                                "size()", is(3),
                                "[0].body", is(messages.get(0)),
                                "[0].headers.CamelAzureEventHubsEnqueuedTime", notNullValue(),
                                "[0].headers.CamelAzureEventHubsPartitionId", is("2"),
                                "[0].headers.CamelAzureEventHubsSequenceNumber", greaterThanOrEqualTo(0),
                                "[1].body", is(messages.get(1)),
                                "[1].headers.CamelAzureEventHubsEnqueuedTime", notNullValue(),
                                "[1].headers.CamelAzureEventHubsPartitionId", is("2"),
                                "[1].headers.CamelAzureEventHubsSequenceNumber", greaterThanOrEqualTo(0),
                                "[2].body", is(messages.get(2)),
                                "[2].headers.CamelAzureEventHubsEnqueuedTime", notNullValue(),
                                "[2].headers.CamelAzureEventHubsPartitionId", is("2"),
                                "[2].headers.CamelAzureEventHubsSequenceNumber", greaterThanOrEqualTo(0));
                assertThat(((Number) (response.extract().path("[0].headers.CamelAzureEventHubsOffset"))).longValue())
                        .isGreaterThanOrEqualTo(0);
                assertThat(((Number) (response.extract().path("[1].headers.CamelAzureEventHubsOffset"))).longValue())
                        .isGreaterThanOrEqualTo(0);
                assertThat(((Number) (response.extract().path("[2].headers.CamelAzureEventHubsOffset"))).longValue())
                        .isGreaterThanOrEqualTo(0);
            });

            RestAssured.given()
                    .post("/azure-eventhubs/route/eventhubs-consumer-custom-checkpoint-store/stop")
                    .then()
                    .statusCode(204);

            // Start another consumer configured to read partition 2 from the earliest offset
            RestAssured.given()
                    .post("/azure-eventhubs/route/eventhubs-consumer-with-event-position/start")
                    .then()
                    .statusCode(204);

            Awaitility.await().pollInterval(1, TimeUnit.SECONDS).atMost(1, TimeUnit.MINUTES).untilAsserted(() -> {
                Response response = RestAssured.given()
                        .queryParam("endpointUri", "mock:partition-2-event-position-results")
                        .contentType(ContentType.JSON)
                        .body(messages)
                        .get("/azure-eventhubs/receive-events")
                        .then()
                        .statusCode(200)
                        .extract()
                        .response();

                // Based on the data retention period configured on the EventHub, we can't make assumptions about what data is in the partition
                // Therefore, we assume the last 3 events will be the ones produced earlier in the test
                List<Map<String, Object>> results = response.jsonPath().getList("$.");
                int size = results.size();
                assertTrue(size >= 3);
                assertEquals(messages.get(0), results.get(size - 3).get("body"));
                assertEquals(messages.get(1), results.get(size - 2).get("body"));
                assertEquals(messages.get(2), results.get(size - 1).get("body"));
            });
        } finally {
            RestAssured.given()
                    .post("/azure-eventhubs/route/eventhubs-consumer-with-event-position/stop")
                    .then()
                    .statusCode(204);
        }
    }

    @Test
    void tokenCredentials() {
        try {
            RestAssured.given()
                    .post("/azure-eventhubs/route/eventhubs-consumer-custom-token-credential/start")
                    .then()
                    .statusCode(204);

            final String messageBody = UUID.randomUUID().toString();

            RestAssured.given()
                    .contentType(ContentType.TEXT)
                    .queryParam("endpointUri", "direct:sendEventUsingTokenCredential")
                    .body(messageBody)
                    .post("/azure-eventhubs/send-event/3")
                    .then()
                    .statusCode(201);

            Awaitility.await().pollInterval(1, TimeUnit.SECONDS).atMost(1, TimeUnit.MINUTES).untilAsserted(() -> {
                ValidatableResponse response = RestAssured.given()
                        .queryParam("endpointUri", "mock:partition-3-results")
                        .body(messageBody)
                        .get("/azure-eventhubs/receive-event")
                        .then()
                        .statusCode(200)
                        .body(
                                "body", is(messageBody),
                                "headers.CamelAzureEventHubsEnqueuedTime", notNullValue(),
                                "headers.CamelAzureEventHubsPartitionId", is("3"),
                                "headers.CamelAzureEventHubsSequenceNumber", greaterThanOrEqualTo(0));
                assertThat(((Number) (response.extract().path("headers.CamelAzureEventHubsOffset"))).longValue())
                        .isGreaterThanOrEqualTo(0);
            });
        } finally {
            RestAssured.given()
                    .post("/azure-eventhubs/route/eventhubs-consumer-custom-token-credential/stop")
                    .then()
                    .statusCode(204);
        }
    }

    @Test
    void azureIdentityCredentials() {
        Assumptions.assumeTrue(AzureCredentialsHelper.isAzureIdentityCredentialsAvailable());

        try {
            RestAssured.given()
                    .post("/azure-eventhubs/route/eventhubs-consumer-azure-identity-credential/start")
                    .then()
                    .statusCode(204);

            final String messageBody = UUID.randomUUID().toString();

            RestAssured.given()
                    .contentType(ContentType.TEXT)
                    .queryParam("endpointUri", "direct:sendEventUsingAzureIdentity")
                    .body(messageBody)
                    .post("/azure-eventhubs/send-event/4")
                    .then()
                    .statusCode(201);

            Awaitility.await().pollInterval(1, TimeUnit.SECONDS).atMost(1, TimeUnit.MINUTES).untilAsserted(() -> {
                ValidatableResponse response = RestAssured.given()
                        .queryParam("endpointUri", "mock:partition-4-results")
                        .body(messageBody)
                        .get("/azure-eventhubs/receive-event")
                        .then()
                        .statusCode(200)
                        .body(
                                "body", is(messageBody),
                                "headers.CamelAzureEventHubsEnqueuedTime", notNullValue(),
                                "headers.CamelAzureEventHubsPartitionId", is("4"),
                                "headers.CamelAzureEventHubsSequenceNumber", greaterThanOrEqualTo(0));
                assertThat(((Number) (response.extract().path("headers.CamelAzureEventHubsOffset"))).longValue())
                        .isGreaterThanOrEqualTo(0);
            });
        } finally {
            RestAssured.given()
                    .post("/azure-eventhubs/route/eventhubs-consumer-azure-identity-credential/stop")
                    .then()
                    .statusCode(204);
        }
    }

    @Test
    void amqpWebSocketsTransport() {
        try {
            RestAssured.given()
                    .post("/azure-eventhubs/route/eventhubs-consumer-with-amqp-ws-transport/start")
                    .then()
                    .statusCode(204);

            final String messageBody = UUID.randomUUID().toString();

            RestAssured.given()
                    .contentType(ContentType.TEXT)
                    .queryParam("endpointUri", "direct:sendEventUsingAmqpWebSockets")
                    .body(messageBody)
                    .post("/azure-eventhubs/send-event/4")
                    .then()
                    .statusCode(201);

            Awaitility.await().pollInterval(1, TimeUnit.SECONDS).atMost(1, TimeUnit.MINUTES).untilAsserted(() -> {
                ValidatableResponse response = RestAssured.given()
                        .queryParam("endpointUri", "mock:partition-4-ws-transport-results")
                        .body(messageBody)
                        .get("/azure-eventhubs/receive-event")
                        .then()
                        .statusCode(200)
                        .body(
                                "body", is(messageBody),
                                "headers.CamelAzureEventHubsEnqueuedTime", notNullValue(),
                                "headers.CamelAzureEventHubsPartitionId", is("4"),
                                "headers.CamelAzureEventHubsSequenceNumber", greaterThanOrEqualTo(0));
                assertThat(((Number) (response.extract().path("headers.CamelAzureEventHubsOffset"))).longValue())
                        .isGreaterThanOrEqualTo(0);
            });
        } finally {
            RestAssured.given()
                    .post("/azure-eventhubs/route/eventhubs-consumer-with-amqp-ws-transport/stop")
                    .then()
                    .statusCode(204);
        }
    }

    @Test
    void generatedConnectionString() {
        try {
            RestAssured.given()
                    .post("/azure-eventhubs/route/eventhubs-consumer-generated-connection-string/start")
                    .then()
                    .statusCode(204);

            final String messageBody = UUID.randomUUID().toString();

            RestAssured.given()
                    .contentType(ContentType.TEXT)
                    .queryParam("endpointUri", "direct:sendEventWithGeneratedConnectionString")
                    .body(messageBody)
                    .post("/azure-eventhubs/send-event/0")
                    .then()
                    .statusCode(201);

            Awaitility.await().pollInterval(1, TimeUnit.SECONDS).atMost(1, TimeUnit.MINUTES).untilAsserted(() -> {
                ValidatableResponse response = RestAssured.given()
                        .queryParam("endpointUri", "mock:partition-0-generated-connection-string-results")
                        .body(messageBody)
                        .get("/azure-eventhubs/receive-event")
                        .then()
                        .statusCode(200)
                        .body(
                                "body", is(messageBody),
                                "headers.CamelAzureEventHubsEnqueuedTime", notNullValue(),
                                "headers.CamelAzureEventHubsPartitionId", is("0"),
                                "headers.CamelAzureEventHubsSequenceNumber", greaterThanOrEqualTo(0));
                assertThat(((Number) (response.extract().path("headers.CamelAzureEventHubsOffset"))).longValue())
                        .isGreaterThanOrEqualTo(0);
            });
        } finally {
            RestAssured.given()
                    .post("/azure-eventhubs/route/eventhubs-consumer-generated-connection-string/stop")
                    .then()
                    .statusCode(204);
        }
    }
}
