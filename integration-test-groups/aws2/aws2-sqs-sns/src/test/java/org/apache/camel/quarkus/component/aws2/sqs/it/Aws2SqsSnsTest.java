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

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.apache.camel.quarkus.test.support.aws2.Aws2TestResource;
import org.awaitility.Awaitility;
import org.eclipse.microprofile.config.ConfigProvider;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.core.Is.is;

@QuarkusTest
@QuarkusTestResource(Aws2TestResource.class)
class Aws2SqsSnsTest {

    @Test
    public void sqs() {
        final String queueName = getPredefinedQueueName();

        final String[] queues = listQueues();
        Assertions.assertTrue(Stream.of(queues).anyMatch(url -> url.contains(queueName)));

        final String msg = sendSingleMessageToQueue(queueName);
        awaitMessageWithExpectedContentFromQueue(msg, queueName);
    }

    private String[] listQueues() {
        return RestAssured.get("/aws2-sqs-sns/sqs/queues")
                .then()
                .statusCode(200)
                .extract()
                .body().as(String[].class);
    }

    @Test
    public void sqsDeleteMessage() {
        final String qName = getPredefinedQueueName();
        final String msg = sendSingleMessageToQueue(qName);
        final String receipt = receiveReceiptOfMessageFromQueue(qName);
        deleteMessageFromQueue(qName, receipt);
        Assertions.assertNotEquals(receiveMessageFromQueue(qName), msg);
    }

    private String getPredefinedQueueName() {
        return ConfigProvider.getConfig().getValue("aws-sqs.queue-name", String.class);
    }

    private String sendSingleMessageToQueue(String queueName) {
        final String msg = "sqs" + UUID.randomUUID().toString().replace("-", "");
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(msg)
                .post("/aws2-sqs-sns/sqs/send/" + queueName)
                .then()
                .statusCode(201);
        return msg;
    }

    private String receiveReceiptOfMessageFromQueue(String queueName) {
        return RestAssured.get("/aws2-sqs-sns/sqs/receive/receipt/" + queueName)
                .then()
                .statusCode(200)
                .extract()
                .body()
                .asString();
    }

    private void deleteMessageFromQueue(String queueName, String receipt) {
        RestAssured.delete("/aws2-sqs-sns/sqs/delete/message/" + queueName + "/" + receipt)
                .then()
                .statusCode(200);
    }

    @Test
    @Disabled("https://github.com/apache/camel-quarkus/issues/3097")
    public void sqsAutoCreateDelayedQueue() {
        final String qName = "delayQueue";
        final int delay = 10;
        createDelayQueueAndVerifyExistence("delayQueue", delay);
        final String msgSent = sendSingleMessageToQueue(qName);
        Instant start = Instant.now();
        awaitMessageWithExpectedContentFromQueue(msgSent, qName);
        Assertions.assertTrue(Duration.between(start, Instant.now()).getSeconds() >= delay);
        deleteQueue(qName);
    }

    private void createDelayQueueAndVerifyExistence(String queueName, int delay) {
        RestAssured.post("/aws2-sqs-sns/sqs/queue/autocreate/delayed/" + queueName + "/" + delay)
                .then()
                .statusCode(200);
        Assertions.assertTrue(Stream.of(listQueues()).anyMatch(url -> url.contains(queueName)));
    }

    private void awaitMessageWithExpectedContentFromQueue(String expectedContent, String queueName) {
        Awaitility.await().pollInterval(1, TimeUnit.SECONDS).atMost(120, TimeUnit.SECONDS).until(
                () -> receiveMessageFromQueue(queueName),
                Matchers.is(expectedContent));
    }

    private void deleteQueue(String queueName) {
        RestAssured.get("/aws2-sqs-sns/sqs/delete/queue/" + queueName)
                .then()
                .statusCode(200);
        awaitQueueDeleted(queueName);
    }

    private void awaitQueueDeleted(String queueName) {
        Awaitility.await().pollInterval(1, TimeUnit.SECONDS).atMost(120, TimeUnit.SECONDS).until(
                () -> Stream.of(listQueues()).peek(System.out::println).noneMatch(url -> url.contains(queueName)));
    }

    private String receiveMessageFromQueue(String queueName) {
        return receiveMessageFromQueue(queueName, true);
    }

    private String receiveMessageFromQueue(String queueName, boolean deleteMessage) {
        return RestAssured.get("/aws2-sqs-sns/sqs/receive/" + queueName + "/" + deleteMessage)
                .then()
                .statusCode(anyOf(is(200), is(204)))
                .extract()
                .body()
                .asString();
    }

    @Test
    public void sqsSendBatchMessage() {
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
                .post("/aws2-sqs-sns/sqs/batch")
                .then()
                .statusCode(201)
                .extract()
                .body()
                .asString());
    }

    @Test
    public void sqsPurgeQueue() {
        final String qName = getPredefinedQueueName();
        sendSingleMessageToQueue(qName);
        purgeQueue(qName);
        awaitAllMessagesDeletedFromQueue(qName);
    }

    private void purgeQueue(String queueName) {
        RestAssured.delete("/aws2-sqs-sns/sqs/purge/queue/" + queueName)
                .then()
                .statusCode(200);
    }

    private void awaitAllMessagesDeletedFromQueue(String queueName) {
        // it can take up to 60 seconds to purge all messages in queue as stated in documentation
        Awaitility.await().pollInterval(1, TimeUnit.SECONDS).atMost(60, TimeUnit.SECONDS).until(
                () -> receiveMessageFromQueue(queueName, false),
                Matchers.emptyOrNullString());
    }

    @Test
    void sns() {
        final String snsMsg = "sns" + UUID.randomUUID().toString().replace("-", "");
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(snsMsg)
                .post("/aws2-sqs-sns/sns/send")
                .then()
                .statusCode(201);

        RestAssured
                .get("/aws2-sqs-sns/sns/receiveViaSqs")
                .then()
                .statusCode(200)
                .body("Message", is(snsMsg));

    }

    @Test
    void snsFifo() {
        final String snsMsg = "snsFifo" + UUID.randomUUID().toString().replace("-", "");
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .queryParam("fifo", true)
                .body(snsMsg)
                .post("/aws2-sqs-sns/sns/send")
                .then()
                .statusCode(201);

        RestAssured
                .get("/aws2-sqs-sns/snsFifo/receiveViaSqs")
                .then()
                .statusCode(200)
                .body("Message", is(snsMsg));
    }

}
