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

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.ResourceArg;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.apache.camel.quarkus.test.support.activemq.ActiveMQTestResource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@QuarkusTestResource(initArgs = {
        @ResourceArg(name = "modules", value = "quarkus.artemis,camel.component.paho,broker-url.ws")
}, value = ActiveMQTestResource.class)
class JmsTest {

    //@ParameterizedTest
    @ValueSource(strings = { "jms", "paho", "paho-ws", "sjms" })
    public void testJmsComponent(String component) {
        String message = "Hello Camel Quarkus " + component;

        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(message)
                .post("/messaging/" + component + "/{queueName}", component + "-test-queue")
                .then()
                .statusCode(201);

        RestAssured.given()
                .contentType(ContentType.TEXT)
                .get("/messaging/" + component + "/{queueName}", component + "-test-queue")
                .then()
                .statusCode(200)
                .body(is(message));
    }

    //@ParameterizedTest
    @ValueSource(strings = { "bytes", "file", "node", "string" })
    public void testJmsMessageType(String type) {
        String message = "Message type " + type;
        String expected = message;
        if (type.equals("node")) {
            expected = "<test>" + message + "</test>";
        }

        RestAssured.given()
                .body(message)
                .post("/messaging/jms/type/" + type)
                .then()
                .statusCode(200)
                .body(is(expected));
    }

    //@Test
    public void testJmsMapMessage() {
        Map<String, String> message = new HashMap<>();
        message.put("foo", "bar");
        message.put("cheese", "wine");

        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(message)
                .post("/messaging/jms/map")
                .then()
                .statusCode(200)
                .body(is("{\"foo\":\"bar\",\"cheese\":\"wine\"}"));
    }

    //@Test
    public void testJmsMessageListenerContainerFactory() {
        String message = "Camel JMS With Custom MessageListenerContainerFactory";
        RestAssured.given()
                .body(message)
                .post("/messaging/jms/custom/message/listener/factory")
                .then()
                .statusCode(200)
                .body(is(message));
    }

    //@Test
    public void testJmsDestinationResolver() {
        String message = "Camel JMS With Custom DestinationResolver";
        RestAssured.given()
                .body(message)
                .post("/messaging/jms/custom/destination/resolver")
                .then()
                .statusCode(200)
                .body(is(message));
    }

    //@Test
    public void testJmsMessageConverter() {
        String result = RestAssured.given()
                .body("a test message")
                .post("/messaging/jms/custom/message/converter")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .asString();

        assertTrue(result.startsWith("converter prefix"));
        assertTrue(result.endsWith("converter suffix"));
    }

    //@Test
    public void testJmsTopic() {
        String message = "Camel JMS Topic Message";
        RestAssured.given()
                .body(message)
                .post("/messaging/jms/topic")
                .then()
                .statusCode(204);
    }

    //@Test
    public void testJmsSelector() {
        RestAssured.given()
                .get("/messaging/jms/selector/foo='bar'")
                .then()
                .statusCode(200)
                .body(is("Camel JMS Selector Match"));
    }

    //@Test
    public void testJmsObject() {
        String message = "Mr Test Person";
        RestAssured.given()
                .body(message)
                .post("/messaging/jms/object")
                .then()
                .statusCode(200)
                .body(is(message));
    }

    //@Test
    public void testJmsTransferExchange() {
        String message = "Test transfer message";
        RestAssured.given()
                .body(message)
                .post("/messaging/jms/transfer/exchange")
                .then()
                .statusCode(200)
                .body(is(message));
    }

    //@Test
    public void testJmsTransaction() {
        RestAssured.given()
                .get("/messaging/jms/transaction")
                .then()
                .statusCode(200)
                .body(is("JMS Transaction Success"));
    }

    //@Test
    public void testResequence() {
        final List<String> messages = Arrays.asList("a", "b", "c", "c", "d");
        for (String msg : messages) {
            RestAssured.given()
                    .body(msg)
                    .post("/messaging/jms/resequence")
                    .then()
                    .statusCode(201);
        }
        Collections.reverse(messages);
        final List<String> actual = RestAssured.given()
                .get("/messaging/jms/mock/resequence/5/10000")
                .then()
                .statusCode(200)
                .extract().body().jsonPath().getList(".", String.class);
        Assertions.assertEquals(messages, actual);
    }

}
