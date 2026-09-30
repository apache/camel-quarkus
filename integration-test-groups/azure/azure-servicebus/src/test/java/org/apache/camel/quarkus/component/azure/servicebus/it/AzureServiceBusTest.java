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
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.apache.camel.quarkus.test.EnabledIf;
import org.apache.camel.quarkus.test.mock.backend.MockBackendDisabled;
import org.apache.camel.quarkus.test.support.azure.AzureServiceBusTestResource;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionTimeoutException;
import org.awaitility.core.ThrowingRunnable;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;

@QuarkusTest
@QuarkusTestResource(AzureServiceBusTestResource.class)
class AzureServiceBusTest {
    // NOTE: Consumer endpoints are started / stopped manually to prevent them from inferring with each other

    private static final Logger LOG = Logger.getLogger(AzureServiceBusTest.class);

    // Kept at 1 minute on purpose while #9257 is investigated, a longer window would hide late deliveries
    private static final Duration MESSAGE_RECEIVE_TIMEOUT = Duration.ofMinutes(1);

    @BeforeAll
    public static void beforeAll() {
        //this is not necessary for mocked testing
        if (AzureServiceBusHelper.isMockBackEnd()) {
            return;
        }

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
            final Instant routeStarted = startRoute(consumerRouteId);

            final Sent sent = send(RestAssured.given()
                    .contentType(ContentType.TEXT)
                    .queryParam("serviceBusType", destinationType)
                    .queryParam("payloadType", payloadType)
                    .queryParam("transportType", transportType.name())
                    .body(messageBody), destination);

            awaitReceived(routeStarted, sent, () -> {
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
            messages.add("cq-azure-servicebus-test-" + UUID.randomUUID().toString());
        }
        try {
            final Instant routeStarted = startRoute(consumerRouteId);

            final Sent sent = send(RestAssured.given()
                    .contentType(ContentType.JSON)
                    .queryParam("serviceBusType", "queue")
                    .queryParam("payloadType", List.class.getSimpleName())
                    .queryParam("transportType", AmqpTransportType.AMQP.name())
                    .body(messages), destination);

            awaitReceived(routeStarted, sent, () -> {
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

    @Test
    void produceConsumeWithCustomClients() {
        final String messageBody = UUID.randomUUID().toString();
        try {
            final Instant routeStarted = startRoute("servicebus-queue-consumer-custom-processor");

            final Sent sent = send(RestAssured.given()
                    .contentType(ContentType.TEXT)
                    .queryParam("directEndpointUri", "direct:send-message-custom-client")
                    .queryParam("payloadType", String.class.getSimpleName())
                    .body(messageBody), AzureServiceBusHelper.getDestination("queue"));

            awaitReceived(routeStarted, sent, () -> {
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
    @EnabledIf({ MockBackendDisabled.class }) //only connection string is enabled on emulator, see https://github.com/Azure/azure-service-bus-emulator-installer/issues/30#issuecomment-2500163047
    void tokenCredentialAuthentication() {
        final String messageBody = UUID.randomUUID().toString();
        try {
            final Instant routeStarted = startRoute("servicebus-queue-consumer-token-credential");

            final Sent sent = send(RestAssured.given()
                    .contentType(ContentType.TEXT)
                    .queryParam("directEndpointUri", "direct:token-credential")
                    .queryParam("payloadType", String.class.getSimpleName())
                    .body(messageBody), AzureServiceBusHelper.getDestination("queue"));

            awaitReceived(routeStarted, sent, () -> {
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
            final Instant routeStarted = startRoute("servicebus-queue-scheduled-consumer");

            // Schedule message for 15 seconds in the future
            long scheduledEnqueueTime = Instant.now()
                    .plus(Duration.of(15, ChronoUnit.SECONDS))
                    .toEpochMilli();

            final Sent sent = send(RestAssured.given()
                    .contentType(ContentType.TEXT)
                    .queryParam("directEndpointUri", "direct:scheduled")
                    .queryParam("payloadType", String.class.getSimpleName())
                    .queryParam("scheduledEnqueueTime", scheduledEnqueueTime)
                    .body(messageBody), AzureServiceBusHelper.getDestination("queue"));

            //we are checking that there is no message in the next 10 seconds.
            //we should rather avoid checking the message near the scheduled time, because process can be delayed and receives the message
            while (Instant.now().toEpochMilli() < (scheduledEnqueueTime - 5000)) {
                // No message should be received before the scheduled time
                RestAssured.given()
                        .queryParam("endpointUri", "mock:servicebus-queue-scheduled-consumer-results")
                        .get("/azure-servicebus/receive/messages")
                        .then()
                        .statusCode(200)
                        .body("size()", is(0));
                try {
                    //wait those 5 seconds, we were not checking
                    Thread.sleep(5250);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            // Message should be enqueued and eventually consumed
            awaitReceived(routeStarted, sent, () -> {
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

    @EnabledIf({ MockBackendDisabled.class }) //only connection string is enabled on emulator, see https://github.com/Azure/azure-service-bus-emulator-installer/issues/30#issuecomment-2500163047
    @Test
    void azureIdentityCredentials() {
        Assumptions.assumeTrue(AzureServiceBusHelper.isAzureIdentityCredentialsAvailable());

        final String messageBody = UUID.randomUUID().toString();
        try {
            final Instant routeStarted = startRoute("servicebus-queue-consumer-azure-identity");

            final Sent sent = send(RestAssured.given()
                    .contentType(ContentType.TEXT)
                    .queryParam("directEndpointUri", "direct:azure-identity")
                    .queryParam("payloadType", String.class.getSimpleName())
                    .body(messageBody), AzureServiceBusHelper.getDestination("queue"));

            awaitReceived(routeStarted, sent, () -> {
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

    @Test
    void dynamicExceptionInstantiation() {
        RestAssured.get("/azure-servicebus/exception/cache")
                .then()
                .statusCode(200)
                .body(is("true"));
    }

    private static Instant startRoute(String routeId) {
        RestAssured.given()
                .post("/azure-servicebus/route/" + routeId + "/start")
                .then()
                .statusCode(204);
        return Instant.now();
    }

    // Fails with the server side stack trace when the send did not succeed, so the cause reaches the CI console
    private static Sent send(RequestSpecification request, String destination) {
        final Instant start = Instant.now();
        final Response response = request.post("/azure-servicebus/send/message/" + destination);
        final long millis = Duration.between(start, Instant.now()).toMillis();
        Assertions.assertEquals(201, response.statusCode(),
                () -> "Sending the message failed after " + millis + " ms:\n" + response.asString());
        return new Sent(Instant.now(), millis);
    }

    // Puts the route start / send timeline into the timeout error, the only text that reaches a CI console when
    // test output is redirected to a file
    private static void awaitReceived(Instant routeStarted, Sent sent, ThrowingRunnable assertion) {
        try {
            Awaitility.await().pollInterval(1, TimeUnit.SECONDS).atMost(MESSAGE_RECEIVE_TIMEOUT).untilAsserted(assertion);
        } catch (ConditionTimeoutException e) {
            throw new AssertionError(
                    "No message received within %s. The consumer route was started %d ms before the send completed, the send took %d ms. %s"
                            .formatted(MESSAGE_RECEIVE_TIMEOUT, Duration.between(routeStarted, sent.at()).toMillis(),
                                    sent.millis(), e.getMessage()),
                    e);
        }
    }

    private record Sent(Instant at, long millis) {
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
        //in mocked backend, the  AMQP_WEB_SOCKET is not supported, see https://github.com/Azure/azure-service-bus-emulator-installer/issues/51
        AmqpTransportType[] transportTypes;
        if (AzureServiceBusHelper.isMockBackEnd()) {
            transportTypes = new AmqpTransportType[] { AmqpTransportType.AMQP };
        } else {
            transportTypes = AmqpTransportType.values();
        }
        return Stream.of(destinationTypes.split(","))
                .flatMap(s1 -> Stream.of(transportTypes)
                        .flatMap(s2 -> Stream.of(payloadTypes)
                                .map(s3 -> Arguments.of(s1, s2, s3))));
    }
}
