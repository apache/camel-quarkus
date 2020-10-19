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

import java.util.HashMap;
import java.util.Map;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hamcrest.core.Is.is;

@QuarkusTest
@QuarkusTestResource(ActiveMQTestResource.class)
class JmsTest {

    @ParameterizedTest
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

    @ParameterizedTest
    @ValueSource(strings = { "Text", "Bytes" })
    public void testJmsMessageType(String type) {
        RestAssured.given()
                .get("/messaging/jms/type/" + type)
                .then()
                .statusCode(200)
                .body(is("true"));
    }

    @Test
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

    @Test
    public void testJmsMessageListenerContainerFactory() {
        String message = "Camel JMS With Custom MessageListenerContainerFactory";
        RestAssured.given()
                .body(message)
                .post("/messaging/jms/custom/message/listener/factory")
                .then()
                .statusCode(200)
                .body(is(message));
    }

    @Test
    public void testJmsDestinationResolver() {
        String message = "Camel JMS With Custom DestinationResolver";
        RestAssured.given()
                .body(message)
                .post("/messaging/jms/custom/destination/resolver")
                .then()
                .statusCode(200)
                .body(is(message));
    }

    @Test
    public void testJmsTopic() {
        String message = "Camel JMS Topic Message";
        RestAssured.given()
                .body(message)
                .post("/messaging/jms/topic")
                .then()
                .statusCode(201);
    }

    @Test
    public void testJmsSelector() {
        RestAssured.given()
                .get("/messaging/jms/selector/foo='bar'")
                .then()
                .statusCode(200)
                .body(is("Camel JMS Selector Match"));
    }

    @Test
    public void testJmsTransaction() {
        RestAssured.given()
                .get("/messaging/jms/transaction")
                .then()
                .statusCode(200)
                .body(is("JMS Transaction Success"));
    }
}
