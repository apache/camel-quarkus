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
package org.apache.camel.quarkus.component.nats.it;

import java.util.concurrent.TimeUnit;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.Header;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTestResource(NatsTestResource.class)
@QuarkusTest
class NatsTest {

    //@Test
    void basicAuthProduceConsumeRoundTripShouldSucceed() {
        Header header = new Header("sendToEndpointUri", "natsBasicAuth:test");
        given().when().header(header).body("basic-auth-msg").post("/nats/send").then().statusCode(204);

        await().atMost(10L, TimeUnit.SECONDS).until(() -> {
            return given().get("/nats/messages/basic-auth").path("size()").equals(1);
        });

        String[] messages = given().get("/nats/messages/basic-auth").then().statusCode(200).extract().as(String[].class);
        assertEquals(1, messages.length);
        assertEquals("basic-auth-msg", messages[0]);
    }

    //@Test
    void noAuthProduceConsumeRoundTripShouldSucceed() {
        Header header = new Header("sendToEndpointUri", "natsNoAuth:test");
        given().when().header(header).body("no-auth-msg").post("/nats/send").then().statusCode(204);

        await().atMost(10L, TimeUnit.SECONDS).until(() -> {
            return given().get("/nats/messages/no-auth").path("size()").equals(1);
        });

        String[] messages = given().get("/nats/messages/no-auth").then().statusCode(200).extract().as(String[].class);
        assertEquals(1, messages.length);
        assertEquals("no-auth-msg", messages[0]);
    }

    //@Test
    @EnabledIfEnvironmentVariable(named = "ENABLE_TLS_TESTS", matches = "true")
    void tlsAuthProduceConsumeRoundTripShouldSucceed() {
        Header header = new Header("sendToEndpointUri", "natsTlsAuth:test?sslContextParameters=#ssl&secure=true");
        given().when().header(header).body("tls-auth-msg").post("/nats/send").then().statusCode(204);

        await().atMost(10L, TimeUnit.SECONDS).until(() -> {
            return given().get("/nats/messages/tls-auth").path("size()").equals(1);
        });

        String[] messages = given().get("/nats/messages/tls-auth").then().statusCode(200).extract().as(String[].class);
        assertEquals(1, messages.length);
        assertEquals("tls-auth-msg", messages[0]);
    }

    //@Test
    void tokenAuthProduceConsumeRoundTripShouldSucceed() {
        Header header = new Header("sendToEndpointUri", "natsTokenAuth:test");
        given().when().header(header).body("token-auth-msg").post("/nats/send").then().statusCode(204);

        await().atMost(10L, TimeUnit.SECONDS).until(() -> {
            return given().get("/nats/messages/token-auth").path("size()").equals(1);
        });

        String[] messages = given().get("/nats/messages/token-auth").then().statusCode(200).extract().as(String[].class);
        assertEquals(1, messages.length);
        assertEquals("token-auth-msg", messages[0]);
    }

    //@Test
    void consumeMaxMessagesShouldRetainFirstTwoMessages() {
        Header header = new Header("sendToEndpointUri", "natsNoAuth:max");
        for (int msgNumber = 1; msgNumber <= 10; msgNumber++) {
            given().when().header(header).body("msg " + msgNumber).post("/nats/send").then().statusCode(204);
        }

        await().atMost(10L, TimeUnit.SECONDS).until(() -> {
            return given().get("/nats/messages/2-msg-max").path("size()").equals(2);
        });

        String[] messages = given().get("/nats/messages/2-msg-max").then().statusCode(200).extract().as(String[].class);
        assertEquals(2, messages.length);
        assertEquals("msg 1", messages[0]);
        assertEquals("msg 2", messages[1]);
    }

    //@Test
    void consumeMaxQueueMessagesShouldRetainRightNumberOfMessages() {
        Header header = new Header("sendToEndpointUri", "natsNoAuth:qmax");
        for (int msgNumber = 1; msgNumber <= 20; msgNumber++) {
            given().when().header(header).body("qmsg " + msgNumber).post("/nats/send").then().statusCode(204);
        }

        await().atMost(10L, TimeUnit.SECONDS).until(() -> {
            return given().get("/nats/messages/3-qmsg-max").path("size()").equals(3)
                    && given().get("/nats/messages/8-qmsg-max").path("size()").equals(8);
        });
    }

    //@Test
    void requestReplyShouldSucceed() {
        Header header = new Header("sendToEndpointUri", "natsNoAuth:request-reply?replySubject=reply");
        given().when().header(header).body("Request").post("/nats/send").then().statusCode(204);

        await().atMost(10L, TimeUnit.SECONDS).until(() -> {
            return given().get("/nats/messages/reply").path("size()").equals(1);
        });

        String[] messages = given().get("/nats/messages/reply").then().statusCode(200).extract().as(String[].class);
        assertEquals(1, messages.length);
        assertEquals("Request => Reply", messages[0]);
    }

}
