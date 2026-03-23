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
package org.apache.camel.quarkus.component.aws2.sqs.it;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.apache.camel.quarkus.test.support.aws2.Aws2LocalStack;
import org.apache.camel.quarkus.test.support.aws2.Aws2TestResource;
import org.apache.camel.quarkus.test.support.aws2.BaseAWs2TestSupport;
import org.apache.commons.lang3.RandomStringUtils;
import org.awaitility.Awaitility;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

@QuarkusTest
@QuarkusTestResource(Aws2TestResource.class)
class Aws2SqsTest extends BaseAWs2TestSupport {

    private static final Logger LOG = Logger.getLogger(Aws2SqsTest.class);

    @Aws2LocalStack
    private boolean localStack;

    public Aws2SqsTest() {
        super("/aws2-sqs");
    }

    private String getPredefinedQueueName() {
        return ConfigProvider.getConfig().getValue("aws-sqs.queue-name", String.class);
    }

    private String getPredefinedFailingQueueName() {
        return ConfigProvider.getConfig().getValue("aws-sqs.failing-name", String.class);
    }

    private String getPredefinedDeadletterQueueName() {
        return ConfigProvider.getConfig().getValue("aws-sqs.deadletter-name", String.class);
    }

    private String getDelayedQueueName() {
        return ConfigProvider.getConfig().getValue("aws-sqs.delayed-name", String.class);
    }

    private Integer getPollIntervalSendToDelayQueueInSecs() {
        return ConfigProvider.getConfig().getOptionalValue("aws-sqs.delayed-queue.poll-interval-secs", Integer.class)
                .orElse(10);
    }

    private Integer getTimeoutSendToDelayQueueInMins() {
        return ConfigProvider.getConfig().getOptionalValue("aws-sqs.delayed-queue.timeout-mins", Integer.class).orElse(5);
    }

    @AfterEach
    void purgeQueueAndWait() {
        String qName = getPredefinedQueueName();
        purgeQueue(qName);
        // purge takes up to 60 seconds
        // all messages delivered within those 60 seconds might get deleted
        try {
            if (!localStack) {
                TimeUnit.SECONDS.sleep(60);
            }
        } catch (InterruptedException ignored) {
        }
        Assertions.assertEquals(receiveMessageFromQueue(qName, false), "");
    }

    private void purgeQueue(String queueName) {
        RestAssured.delete("/aws2-sqs/purge/queue/" + queueName)
                .then()
                .statusCode(200);
    }

    @Test
    void sqs() {
        final String queueName = getPredefinedQueueName();

        final String[] queues = listQueues();
        Assertions.assertTrue(Stream.of(queues).anyMatch(url -> url.contains(queueName)));

        final String msg = sendSingleMessageToQueue(queueName);
        awaitMessageWithExpectedContentFromQueue(msg, queueName);
    }

    private String[] listQueues() {
        return RestAssured.get("/aws2-sqs/queues")
                .then()
                .statusCode(200)
                .extract()
                .body().as(String[].class);
    }

    @Test
    void deadletter() {
        final String failingQueueName = getPredefinedFailingQueueName();
        final String deadletterQueueName = getPredefinedDeadletterQueueName();

        final String[] queues = listQueues();
        Assertions.assertTrue(Stream.of(queues).anyMatch(url -> url.contains(failingQueueName)));

        final String msg = sendSingleMessageToQueue(failingQueueName);
        awaitMessageWithExpectedContentFromQueue(msg, deadletterQueueName);
    }

    @Test
    void sqsDeleteMessage() {
        final String qName = getPredefinedQueueName();
        sendSingleMessageToQueue(qName);
        final String receipt = receiveReceiptOfMessageFromQueue(qName);
        final String msg = sendSingleMessageToQueue(qName);
        deleteMessageFromQueue(qName, receipt);
        // assertion is here twice because in case delete wouldn't work in our queue would be two messages
        // it's possible that the first retrieval would retrieve the correct message and therefore the test
        // would incorrectly pass. By receiving message twice we check if the message was really deleted.
        awaitMessageWithExpectedContentFromQueue(msg, qName);
        awaitMessageWithExpectedContentFromQueue(msg, qName);
    }

    private String sendSingleMessageToQueue(String queueName) {
        final String msg = "sqs" + UUID.randomUUID().toString().replace("-", "");
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(msg)
                .post("/aws2-sqs/send/" + queueName)
                .then()
                .statusCode(201);
        return msg;
    }

    private String receiveReceiptOfMessageFromQueue(String queueName) {
        return RestAssured
                .get("/aws2-sqs/receive/receipt/" + queueName)
                .then()
                .statusCode(200)
                .extract()
                .body()
                .asString();
    }

    private void deleteMessageFromQueue(String queueName, String receipt) {
        RestAssured
                .delete("/aws2-sqs/delete/message/" + queueName + "/"
                        + URLEncoder.encode(receipt, StandardCharsets.UTF_8))
                .then()
                .statusCode(200);
    }

    @Test
    void sqsAutoCreateDelayedQueue() {
        final String qName = getDelayedQueueName();
        final int delay = 20;
        try {
            final String msg = "sqs" + UUID.randomUUID().toString().replace("-", "");
            Instant start = Instant.now();
            RestAssured.get("/aws2-sqs/queue/autocreate/delayed/" + qName + "/" + delay + "/" + msg)
                    .then()
                    .statusCode(200)
                    .body(equalTo(msg));
            awaitMessageWithExpectedContentFromQueue(msg, qName);
            Assertions.assertTrue(Duration.between(start, Instant.now()).getSeconds() >= delay);
        } catch (AssertionError e) {
            e.printStackTrace();
            Assertions.fail();
        }
    }

    private String createDelayQueueAndVerifyExistence(String queueName, int delay) {
        return RestAssured.get("/aws2-sqs/queue/autocreate/delayed/" + queueName + "/" + delay)
                .then()
                .statusCode(200)
                .extract()
                .body().asString();
    }

    private void awaitMessageWithExpectedContentFromQueue(String expectedContent, String queueName) {
        Awaitility.await().pollInterval(1, TimeUnit.SECONDS).atMost(120, TimeUnit.SECONDS).until(
                () -> {
                    ExtractableResponse<Response> resp = RestAssured.get("/aws2-sqs/receive/" + queueName + "/false")
                            .then().extract();
                    return resp.statusCode() == 200 && expectedContent.equals(resp.body().asString());
                });
    }

    private void deleteQueue(String queueName) {
        RestAssured.delete("/aws2-sqs/delete/queue/" + queueName)
                .then()
                .statusCode(200);
        awaitQueueDeleted(queueName);
    }

    private void awaitQueueDeleted(String queueName) {
        Awaitility.await().pollInterval(1, TimeUnit.SECONDS).atMost(120, TimeUnit.SECONDS).until(
                () -> Stream.of(listQueues()).noneMatch(url -> url.contains(queueName)));
    }

    private String receiveMessageFromQueue(String queueName, boolean deleteMessage) {
        return RestAssured.get("/aws2-sqs/receive/" + queueName + "/" + deleteMessage)
                .then()
                .statusCode(anyOf(is(200), is(204)))
                .extract()
                .body()
                .asString();
    }

    @Test
    void sqsSendBatchMessage() {
        final List<String> messages = new ArrayList<>(Arrays.asList(
                "Hello from camel-quarkus",
                "This is a batch message test",
                "Let's add few more messages",
                "Next message will be last",
                "Goodbye from camel-quarkus"));
        Assertions.assertEquals(messages.size(), sendMessageBatchAndRetrieveSuccessCount(messages));
    }

    private int sendMessageBatchAndRetrieveSuccessCount(List<String> batch) {
        return Integer.parseInt(RestAssured.given()
                .contentType(ContentType.JSON)
                .body(batch)
                .post("/aws2-sqs/batch")
                .then()
                .statusCode(201)
                .extract()
                .body()
                .asString());
    }

    @Test
    void sqsBatchConsumer() {
        // clean previously collected messages
        RestAssured.delete("/aws2-sqs/batch-consumer/messages").then().statusCode(200);

        final List<String> messages = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            String msg = "batch-consumer-" + UUID.randomUUID().toString().replace("-", "");
            messages.add(msg);
            RestAssured.given()
                    .contentType(ContentType.TEXT)
                    .body(msg)
                    .post("/aws2-sqs/batch-consumer/send")
                    .then()
                    .statusCode(200);
        }

        Awaitility.await().pollInterval(1, TimeUnit.SECONDS).atMost(120, TimeUnit.SECONDS).until(() -> {
            List<?> received = RestAssured.get("/aws2-sqs/batch-consumer/messages")
                    .then().statusCode(200).extract().body().as(List.class);
            return received.size() >= messages.size();
        });

        List<?> received = RestAssured.get("/aws2-sqs/batch-consumer/messages")
                .then().statusCode(200).extract().body().as(List.class);
        Assertions.assertEquals(messages.size(), received.size());
        Assertions.assertTrue(received.containsAll(messages));
    }

    @Test
    void sqsKmsEncryption() {
        Assumptions.assumeTrue(localStack, "KMS test only runs on LocalStack");

        final String kmsQueueName = "camel-quarkus-kms-"
                + RandomStringUtils.secure().nextAlphanumeric(10).toLowerCase(Locale.ROOT);
        final String msg = "kms-msg-" + UUID.randomUUID().toString().replace("-", "");

        try {
            RestAssured.given()
                    .contentType(ContentType.TEXT)
                    .body(msg)
                    .post("/aws2-sqs/kms/send/" + kmsQueueName)
                    .then()
                    .statusCode(200);

            Awaitility.await().pollInterval(1, TimeUnit.SECONDS).atMost(60, TimeUnit.SECONDS).until(() -> {
                ExtractableResponse<Response> resp = RestAssured.get("/aws2-sqs/kms/receive/" + kmsQueueName)
                        .then().extract();
                return resp.statusCode() == 200 && msg.equals(resp.body().asString());
            });
        } finally {
            deleteQueue(kmsQueueName);
        }
    }

    @Test
    void sqsJmsLikeSelector() {
        final String selectorQueueName = ConfigProvider.getConfig().getValue("aws-sqs.selector-name", String.class);

        // clean previously collected messages
        RestAssured.delete("/aws2-sqs/selector/messages").then().statusCode(200);
        purgeQueue(selectorQueueName);

        final String selectedMsg = "selected-" + UUID.randomUUID().toString().replace("-", "");
        final String rejectedMsg = "rejected-" + UUID.randomUUID().toString().replace("-", "");

        // send message that matches the filter (filter-type=selected)
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(selectedMsg)
                .queryParam("filterType", SelectorRouteBuilder.FILTER_ATTRIBUTE_SELECTED_VALUE)
                .post("/aws2-sqs/selector/send/" + selectorQueueName)
                .then()
                .statusCode(200);

        // send message that does not match the filter (filter-type=rejected)
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(rejectedMsg)
                .queryParam("filterType", "rejected")
                .post("/aws2-sqs/selector/send/" + selectorQueueName)
                .then()
                .statusCode(200);

        // wait for the selected message to be consumed
        Awaitility.await().pollInterval(1, TimeUnit.SECONDS).atMost(60, TimeUnit.SECONDS).until(() -> {
            List<?> collected = RestAssured.get("/aws2-sqs/selector/messages")
                    .then().statusCode(200).extract().body().as(List.class);
            return collected.contains(selectedMsg);
        });

        // verify only the selected message was collected
        List<?> collected = RestAssured.get("/aws2-sqs/selector/messages")
                .then().statusCode(200).extract().body().as(List.class);
        Assertions.assertTrue(collected.contains(selectedMsg), "Selected message should have been collected");
        Assertions.assertFalse(collected.contains(rejectedMsg), "Rejected message should not have been collected");

        // purge rejected messages remaining in queue
        purgeQueue(selectorQueueName);
    }

    @Override
    public void testMethodForDefaultCredentialsProvider() {
        listQueues();
    }
}
