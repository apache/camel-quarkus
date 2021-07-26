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
package org.apache.camel.quarkus.component.paho.mqtt5.it;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hamcrest.core.Is.is;

@QuarkusTest
@QuarkusTestResource(PahoMqtt5TestResource.class)
class PahoMqtt5Test {

    //@ParameterizedTest
    @ValueSource(strings = { "tcp", "ws" })
    public void sendReceive(String protocol) {
        String message = "Hello Camel Quarkus " + protocol;

        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(message)
                .post("/paho-mqtt5/tcp/{queueName}", protocol + "-test-queue")
                .then()
                .statusCode(201);

        RestAssured.given()
                .contentType(ContentType.TEXT)
                .get("/paho-mqtt5/tcp/{queueName}", protocol + "-test-queue")
                .then()
                .statusCode(200)
                .body(is(message));
    }

}
