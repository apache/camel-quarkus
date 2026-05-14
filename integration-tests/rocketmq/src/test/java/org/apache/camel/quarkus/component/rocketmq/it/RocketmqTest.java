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
package org.apache.camel.quarkus.component.rocketmq.it;

import java.util.concurrent.TimeUnit;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.http.Header;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTestResource(RocketmqTestResource.class)
@QuarkusTest
class RocketmqTest {

    @Test
    void produceConsumeRoundTripShouldSucceed() {
        String message = "Hello Camel Quarkus RocketMQ";

        Header header = new Header("sendToEndpointUri",
                "rocketmq:camel-test?producerGroup=camel-test-producer-group");
        given().when().header(header).contentType(ContentType.TEXT).body(message).post("/rocketmq/send").then()
                .statusCode(204);

        await().atMost(30L, TimeUnit.SECONDS).until(() -> {
            return given().get("/rocketmq/messages/rocketmq-test").path("size()").equals(1);
        });

        String[] messages = given().get("/rocketmq/messages/rocketmq-test").then().statusCode(200).extract()
                .as(String[].class);
        assertEquals(1, messages.length);
        assertEquals(message, messages[0]);
    }
}
