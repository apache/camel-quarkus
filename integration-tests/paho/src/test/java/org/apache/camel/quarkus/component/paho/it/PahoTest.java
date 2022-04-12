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
package org.apache.camel.quarkus.component.paho.it;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.StringRegularExpression.matchesRegex;

@QuarkusTest
@QuarkusTestResource(PahoTestResource.class)
class PahoTest {

    @ParameterizedTest
    @ValueSource(strings = { "tcp", "ssl", "ws" })
    public void sendReceive(String protocol) {
        String message = "Hello Camel Quarkus " + protocol;

        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(message)
                .post("/paho/{protocol}/{queueName}", protocol, protocol + "-test-queue")
                .then()
                .statusCode(201);

        RestAssured.given()
                .contentType(ContentType.TEXT)
                .get("/paho/{protocol}/{queueName}", protocol, protocol + "-test-queue")
                .then()
                .statusCode(200)
                .body(is(message));
    }

    @Test
    public void overrideTopic() {
        String message = "Hello Camel Quarkus Override Topic";
        String queue = "myoverride";

        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(message)
                .post("/paho/override/" + queue)
                .then()
                .statusCode(201);

        RestAssured.given()
                .contentType(ContentType.TEXT)
                .get("/paho/tcp/" + queue)
                .then()
                .statusCode(200)
                .body(is(message));
    }

    @Test
    public void mqttExceptionDuringReconnectShouldSucceed() {
        RestAssured.get("/paho/mqttExceptionDuringReconnectShouldSucceed")
                .then()
                .statusCode(200)
                .body(matchesRegex(".+"));
    }

    @Test
    public void readThenWriteWithFilePersistenceShouldSucceed() {
        String message = "readThenWriteWithFilePersistenceShouldSucceed message content: 762e6af1-3ec7-40e0-9271-0c98a1001728";
        RestAssured.given()
                .queryParam("message", message)
                .get("/paho/readThenWriteWithFilePersistenceShouldSucceed")
                .then()
                .statusCode(200)
                .body(is(message));
    }
}
