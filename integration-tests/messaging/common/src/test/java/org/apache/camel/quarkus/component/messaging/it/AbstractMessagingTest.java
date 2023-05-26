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
package org.apache.camel.quarkus.component.messaging.it;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import jakarta.json.bind.JsonbBuilder;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hamcrest.core.Is.is;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class AbstractMessagingTest {
    protected String queue;
    protected String queue2;
    protected String topic;

    @BeforeAll
    public void startRoutes(TestInfo info) {
        // At this point RestAssured is not configured to use the test port
        // see https://github.com/quarkusio/quarkus/issues/7690#issuecomment-596543310
        // The comment states it does not work in native, however it seems to work fine
        RestAssured.given()
                .port(ConfigProvider.getConfig().getValue("quarkus.http.test-port", Integer.class))
                .get("/messaging/routes/start");
    }

    @BeforeEach
    public void setupDestinations(TestInfo test) {
        final String testMethod = test.getTestMethod().get().getName();
        queue = testMethod;
        queue2 = testMethod + "2";
        topic = testMethod;
    }

    @Test
    public void testQueueProduceConsume() {
        String message = "Hello Camel Quarkus JMS";

        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(message)
                .post("/messaging/{queueName}", queue)
                .then()
                .statusCode(201);

        RestAssured.given()
                .contentType(ContentType.TEXT)
                .get("/messaging/{queueName}", queue)
                .then()
                .statusCode(200)
                .body(is(message));
    }

    @ParameterizedTest
    @ValueSource(strings = { "bytes", "file", "node", "string" })
    public void testJmsMessageType(String type) {
        String message = "Message type " + type;
        String expected = message;
        if ("node".equals(type)) {
            expected = "<test>" + message + "</test>";
        }

        RestAssured.given()
                .body(message)
                .post("/messaging/{queueName}/type/{type}", queue, type)
                .then()
                .statusCode(200)
                .body(is(expected));
    }

    @Test
    public void testJmsMapMessage() {
        Map<String, String> message = new HashMap<>();
        message.put("foo", "bar");
        message.put("cheese", "wine");

        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(JsonbBuilder.create().toJson(message))
                .post("/messaging/{queueName}/map", queue)
                .then()
                .statusCode(200)
                .body(is("{\"foo\":\"bar\",\"cheese\":\"wine\"}"));
    }

    @Test
    public void testJmsTopic() {
        String message = "Camel JMS Topic Message";
        RestAssured.given()
                .body(message)
                .post("/messaging/topic/{topicName}", topic)
                .then()
                .statusCode(204);
    }

    @Test
    public void testJmsSelector() {
        RestAssured.given()
                .get("/messaging/{queueName}/selector/foo='bar'", queue)
                .then()
                .statusCode(200)
                .body(is("Camel JMS Selector Match"));
    }

    @Test
    public void testJmsObject() {
        String message = "Mr Test Person";
        RestAssured.given()
                .body(message)
                .post("/messaging/{queueName}/object", queue)
                .then()
                .statusCode(200)
                .body(is(message));
    }

    @Test
    public void testJmsTransaction() {
        RestAssured.given()
                .get("/messaging/{queueName}/transaction", queue)
                .then()
                .statusCode(200)
                .body(is("JMS Transaction Success"));
    }

    @DisabledIfEnvironmentVariable(named = "CI", matches = "true", disabledReason = "https://github.com/apache/camel-quarkus/issues/2957")
    @Test
    public void testResequence() {
        final List<String> messages = Arrays.asList("a", "b", "c", "c", "d");
        for (String msg : messages) {
            RestAssured.given()
                    .body(msg)
                    .post("/messaging/{queueName}", queue)
                    .then()
                    .statusCode(201);
        }
        Collections.reverse(messages);
        final List<String> actual = RestAssured.given()
                .get("/messaging/mock/resequence/5/10000")
                .then()
                .statusCode(200)
                .extract().body().jsonPath().getList(".", String.class);
        Assertions.assertEquals(messages, actual);
    }

    @Test
    public void testJmsReplyTo() {
        String message = "JMS Reply To Message";
        RestAssured.given()
                .body(message)
                .post("/messaging/reply/to")
                .then()
                .statusCode(204);
    }

    @Test
    public void testJmsPojoConsumer() {
        String message = "Camel Quarkus JMS POJO Consumer";

        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(message)
                .post("/messaging/{queueName}", queue)
                .then()
                .statusCode(201);

        RestAssured.given()
                .get("/messaging/pojo/consumer")
                .then()
                .statusCode(200)
                .body(is("Hello " + message));
    }
}
