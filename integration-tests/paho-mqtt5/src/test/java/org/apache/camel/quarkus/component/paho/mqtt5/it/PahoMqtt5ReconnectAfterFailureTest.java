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

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;

import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.is;

@QuarkusTest
@TestProfile(ReconnectProfile.class)
public class PahoMqtt5ReconnectAfterFailureTest {

    @InjectPahoContainer
    GenericContainer container;

    @Test
    public void test() throws Exception {
        String msg = "msg";

        RestAssured.given()
                .contentType(ContentType.TEXT)
                .get("/paho-mqtt5/routeStatus/" + PahoMqtt5Route.TESTING_ROUTE_ID)
                .then()
                .statusCode(200)
                .body(is("Stopped"));

        container.start();

        RestAssured.given()
                .contentType(ContentType.TEXT)
                .get("/paho-mqtt5/routeStatus/" + PahoMqtt5Route.TESTING_ROUTE_ID + "?waitForContainerStarted=true")
                .then()
                .statusCode(200)
                .body(anyOf(is("Started"), is("Starting")));

        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(msg)
                .post("/paho-mqtt5/send")
                .then()
                .statusCode(201);

        RestAssured.given()
                .body(msg)
                .post("/paho-mqtt5/mock")
                .then()
                .statusCode(200)
                .body(is("OK"));
    }
}
