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
package org.apache.camel.quarkus.component.aws2.sns.it;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.apache.camel.quarkus.test.support.aws2.Aws2LocalStack;
import org.apache.camel.quarkus.test.support.aws2.Aws2TestResource;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.core.Is.is;

@QuarkusTest
@QuarkusTestResource(Aws2TestResource.class)
class Aws2SqsSnsTest {

    private String getPredefinedQueueName() {
        return ConfigProvider.getConfig().getValue("aws-sqs.queue-name", String.class);
    }

    @Aws2LocalStack
    private boolean localStack;

    @AfterEach
    public void purgeQueueAndWait() {
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
        RestAssured.delete("/aws2-sqs-sns/sqs/purge/queue/" + queueName)
                .then()
                .statusCode(200);

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
