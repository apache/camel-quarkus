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
package org.apache.camel.quarkus.component.azure.servicebus.it;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import com.azure.core.amqp.AmqpTransportType;
import com.azure.core.util.BinaryData;
import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusProcessorClient;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.awaitility.Awaitility;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;

@EnabledIfEnvironmentVariable(named = "AZURE_SERVICEBUS_CONNECTION_STRING", matches = ".+")
@EnabledIfEnvironmentVariable(named = "AZURE_SERVICEBUS_QUEUE_NAME", matches = ".+")
@QuarkusTest
class AzureServiceBusTest {
    // NOTE: Consumer endpoints are started / stopped manually to prevent them from inferring with each other

    private static final Logger LOG = Logger.getLogger(AzureServiceBusTest.class);

    @BeforeAll
    public static void beforeAll() {
        // Drain the test queue in case there are messages lingering from previous failed runs
        ServiceBusProcessorClient client = new ServiceBusClientBuilder()
                .connectionString(AzureServiceBusHelper.getConnectionString())
                .processor()
                .processMessage(messageContext -> {
                    LOG.infof("Purged old message: %s", messageContext.getMessage().getMessageId());
                    messageContext.complete();
                })
                .processError(errorContext -> LOG.errorf(errorContext.getException(),
                        "Error draining queue %s" + errorContext.getEntityPath()))
                .queueName(AzureServiceBusHelper.getDestination("queue"))
                .buildProcessorClient();

        client.start();
        try {
            // We don't know how many messages there may be to drain so just sleep for enough time
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            client.close();
        }
    }

    @ParameterizedTest
    @MethodSource("produceConsumeOptions")
    void produceConsumeMessage(
            String destinationType,
            AmqpTransportType transportType,
            String payloadType) {

        final String consumerRouteId = "servicebus-%s-consumer-%s".formatted(destinationType, transportType);
        final String destination = AzureServiceBusHelper.getDestination(destinationType);
        final String mockEndpointUri = "mock:%s-%s-%s-%s-results".formatted(destinationType, destination, transportType.name(),
                payloadType);
        final String messageBody = UUID.randomUUID().toString();

        try {
            RestAssured.given()
                    .post("/azure-servicebus/route/" + consumerRouteId + "/start")
                    .then()
                    .statusCode(204);

            RestAssured.given()
                    .contentType(ContentType.TEXT)
                    .queryParam("serviceBusType", destinationType)
                    .queryParam("payloadType", payloadType)
                    .queryParam("transportType", transportType.name())
                    .body(messageBody)
                    .post("/azure-servicebus/send/message/" + destination)
                    .then()
                    .statusCode(201);

            Awaitility.await().pollInterval(1, TimeUnit.SECONDS).atMost(1, TimeUnit.MINUTES).untilAsserted(() -> {
                RestAssured.given()
                        .queryParam("endpointUri", mockEndpointUri)
                        .get("/azure-servicebus/receive/messages")
                        .then()
                        .statusCode(200)
                        .body(
                                "[0].body", is(messageBody),
                                "[0].sequenceNumber", greaterThanOrEqualTo(1));
            });
        } finally {
            RestAssured.given()
                    .post("/azure-servicebus/route/" + consumerRouteId + "/stop")
                    .then()
                    .statusCode(204);
        }
    }

    @Test
    void multipleMessages() {
        final String consumerRouteId = "servicebus-queue-consumer-" + AmqpTransportType.AMQP;
        final String destination = AzureServiceBusHelper.getDestination("queue");
        final String mockEndpointUri = "mock:%s-%s-%s-%s-results".formatted("queue", destination, AmqpTransportType.AMQP.name(),
                List.class.getSimpleName());
        final List<String> messages = new ArrayList<>();

        for (int i = 0; i < 3; i++) {
            messages.add(UUID.randomUUID().toString());
        }

        try {
            RestAssured.given()
                    .post("/azure-servicebus/route/" + consumerRouteId + "/start")
                    .then()
                    .statusCode(204);

            RestAssured.given()
                    .contentType(ContentType.JSON)
                    .queryParam("serviceBusType", "queue")
                    .queryParam("payloadType", List.class.getSimpleName())
                    .queryParam("transportType", AmqpTransportType.AMQP.name())
                    .body(messages)
                    .post("/azure-servicebus/send/message/" + destination)
                    .then()
                    .statusCode(201);

            Awaitility.await().pollInterval(1, TimeUnit.SECONDS).atMost(1, TimeUnit.MINUTES).untilAsserted(() -> {
                RestAssured.given()
                        .queryParam("endpointUri", mockEndpointUri)
                        .get("/azure-servicebus/receive/messages")
                        .then()
                        .statusCode(200)
                        .body(
                                "[0].body", is(messages.get(0)),
                                "[0].sequenceNumber", greaterThanOrEqualTo(1),
                                "[1].body", is(messages.get(1)),
                                "[1].sequenceNumber", greaterThanOrEqualTo(1),
                                "[2].body", is(messages.get(2)),
                                "[2].sequenceNumber", greaterThanOrEqualTo(1));
            });
        } finally {
            RestAssured.given()
                    .post("/azure-servicebus/route/" + consumerRouteId + "/stop")
                    .then()
                    .statusCode(204);
        }
    }

    @Disabled("https://github.com/apache/camel-quarkus/issues/6950")
    @Test
    void produceConsumeWithCustomClients() {
        final String messageBody = UUID.randomUUID().toString();
        try {
            RestAssured.given()
                    .post("/azure-servicebus/route/servicebus-queue-consumer-custom-processor/start")
                    .then()
                    .statusCode(204);

            RestAssured.given()
                    .contentType(ContentType.TEXT)
                    .queryParam("directEndpointUri", "direct:send-message-custom-client")
                    .queryParam("payloadType", String.class.getSimpleName())
                    .body(messageBody)
                    .post("/azure-servicebus/send/message/" + AzureServiceBusHelper.getDestination("queue"))
                    .then()
                    .statusCode(201);

            Awaitility.await().pollInterval(1, TimeUnit.SECONDS).atMost(1, TimeUnit.MINUTES).untilAsserted(() -> {
                RestAssured.given()
                        .queryParam("endpointUri", AzureServiceBusProducers.MOCK_ENDPOINT_URI)
                        .get("/azure-servicebus/receive/messages")
                        .then()
                        .statusCode(200)
                        .body(
                                "[0].body", is(messageBody),
                                "[0].sequenceNumber", greaterThanOrEqualTo(1));
            });
        } finally {
            RestAssured.given()
                    .post("/azure-servicebus/route/servicebus-queue-consumer-custom-processor/stop")
                    .then()
                    .statusCode(204);
        }
    }

    @Test
    void tokenCredentialAuthentication() {
        final String messageBody = UUID.randomUUID().toString();
        try {
            RestAssured.given()
                    .post("/azure-servicebus/route/servicebus-queue-consumer-token-credential/start")
                    .then()
                    .statusCode(204);

            RestAssured.given()
                    .contentType(ContentType.TEXT)
                    .queryParam("directEndpointUri", "direct:token-credential")
                    .queryParam("payloadType", String.class.getSimpleName())
                    .body(messageBody)
                    .post("/azure-servicebus/send/message/" + AzureServiceBusHelper.getDestination("queue"))
                    .then()
                    .statusCode(201);

            Awaitility.await().pollInterval(1, TimeUnit.SECONDS).atMost(1, TimeUnit.MINUTES).untilAsserted(() -> {
                RestAssured.given()
                        .queryParam("endpointUri", "mock:servicebus-token-credential-results")
                        .get("/azure-servicebus/receive/messages")
                        .then()
                        .statusCode(200)
                        .body(
                                "[0].body", is(messageBody),
                                "[0].sequenceNumber", greaterThanOrEqualTo(1));
            });
        } finally {
            RestAssured.given()
                    .post("/azure-servicebus/route/servicebus-queue-consumer-token-credential/stop")
                    .then()
                    .statusCode(204);
        }
    }

    @Test
    void scheduled() {
        final String messageBody = UUID.randomUUID().toString();
        try {
            RestAssured.given()
                    .post("/azure-servicebus/route/servicebus-queue-scheduled-consumer/start")
                    .then()
                    .statusCode(204);

            // Schedule message for 10 seconds in the future
            long scheduledEnqueueTime = Instant.now()
                    .plus(Duration.of(10, ChronoUnit.SECONDS))
                    .toEpochMilli();

            RestAssured.given()
                    .contentType(ContentType.TEXT)
                    .queryParam("directEndpointUri", "direct:scheduled")
                    .queryParam("payloadType", String.class.getSimpleName())
                    .queryParam("scheduledEnqueueTime", scheduledEnqueueTime)
                    .body(messageBody)
                    .post("/azure-servicebus/send/message/" + AzureServiceBusHelper.getDestination("queue"))
                    .then()
                    .statusCode(201);

            while (Instant.now().toEpochMilli() < scheduledEnqueueTime) {
                // No message should be received before the scheduled time
                RestAssured.given()
                        .queryParam("endpointUri", "mock:servicebus-queue-scheduled-consumer-results")
                        .get("/azure-servicebus/receive/messages")
                        .then()
                        .statusCode(200)
                        .body("size()", is(0));
                try {
                    Thread.sleep(250);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            // Message should be enqueued and eventually consumed
            Awaitility.await().pollInterval(1, TimeUnit.SECONDS).atMost(1, TimeUnit.MINUTES).untilAsserted(() -> {
                RestAssured.given()
                        .queryParam("endpointUri", "mock:servicebus-queue-scheduled-consumer-results")
                        .get("/azure-servicebus/receive/messages")
                        .then()
                        .statusCode(200)
                        .body(
                                "[0].body", is(messageBody),
                                "[0].sequenceNumber", greaterThanOrEqualTo(1));
            });
        } finally {
            RestAssured.given()
                    .post("/azure-servicebus/route/servicebus-queue-scheduled-consumer/stop")
                    .then()
                    .statusCode(204);
        }
    }

    @Test
    void azureIdentityCredentials() {
        Assumptions.assumeTrue(AzureServiceBusHelper.isAzureIdentityCredentialsAvailable());

        final String messageBody = UUID.randomUUID().toString();
        try {
            RestAssured.given()
                    .post("/azure-servicebus/route/servicebus-queue-consumer-azure-identity/start")
                    .then()
                    .statusCode(204);

            RestAssured.given()
                    .contentType(ContentType.TEXT)
                    .queryParam("directEndpointUri", "direct:azure-identity")
                    .queryParam("payloadType", String.class.getSimpleName())
                    .body(messageBody)
                    .post("/azure-servicebus/send/message/" + AzureServiceBusHelper.getDestination("queue"))
                    .then()
                    .statusCode(201);

            Awaitility.await().pollInterval(1, TimeUnit.SECONDS).atMost(1, TimeUnit.MINUTES).untilAsserted(() -> {
                RestAssured.given()
                        .queryParam("endpointUri", "mock:servicebus-azure-identity-results")
                        .get("/azure-servicebus/receive/messages")
                        .then()
                        .statusCode(200)
                        .body(
                                "[0].body", is(messageBody),
                                "[0].sequenceNumber", greaterThanOrEqualTo(1));
            });
        } finally {
            RestAssured.given()
                    .post("/azure-servicebus/route/servicebus-queue-consumer-azure-identity/stop")
                    .then()
                    .statusCode(204);
        }
    }

    static Stream<Arguments> produceConsumeOptions() {
        String destinationTypes = "queue";

        // Topics aren't available on the Azure basic pricing tier
        if (AzureServiceBusHelper.isAzureServiceBusTopicConfigPresent()) {
            destinationTypes += ",topic";
        } else {
            LOG.warnf(
                    "Configuration options azure.servicebus.topic.name & azure.servicebus.topic.subscription.name are not present. Azure Service Bus topic testing will be disabled");
        }

        String[] payloadTypes = { String.class.getSimpleName(), byte[].class.getSimpleName(),
                BinaryData.class.getSimpleName() };
        AmqpTransportType[] transportTypes = AmqpTransportType.values();
        return Stream.of(destinationTypes.split(","))
                .flatMap(s1 -> Stream.of(transportTypes)
                        .flatMap(s2 -> Stream.of(payloadTypes)
                                .map(s3 -> Arguments.of(s1, s2, s3))));
    }
}
