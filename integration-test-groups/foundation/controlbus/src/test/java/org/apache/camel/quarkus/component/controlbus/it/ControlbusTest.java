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
package org.apache.camel.quarkus.component.controlbus.it;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.equalTo;

@QuarkusTest
class ControlbusTest {

    @BeforeEach
    public void startRoute() {
        String status = RestAssured.get("/controlbus/status").asString();
        if ("Stopped".equals(status)) {
            RestAssured.get("/controlbus/start");
        }
    }

    @Test
    public void testStopStart() {
        RestAssured.given()
                .contentType(ContentType.TEXT).get("/controlbus/status")
                .then().body(equalTo("Started"));

        RestAssured.given()
                .contentType(ContentType.TEXT).get("/controlbus/stop")
                .then().statusCode(200);

        RestAssured.given()
                .contentType(ContentType.TEXT).get("/controlbus/status")
                .then().body(equalTo("Stopped"));

        RestAssured.given()
                .contentType(ContentType.TEXT).get("/controlbus/start")
                .then().statusCode(200);

        RestAssured.given()
                .contentType(ContentType.TEXT).get("/controlbus/status")
                .then().body(equalTo("Started"));
    }

    @Test
    public void testSuspendResume() {
        RestAssured.given()
                .contentType(ContentType.TEXT).get("/controlbus/status")
                .then().body(equalTo("Started"));

        RestAssured.given()
                .contentType(ContentType.TEXT).get("/controlbus/suspend")
                .then().statusCode(200);

        RestAssured.given()
                .contentType(ContentType.TEXT).get("/controlbus/status")
                .then().body(equalTo("Suspended"));

        RestAssured.given()
                .contentType(ContentType.TEXT).get("/controlbus/resume")
                .then().statusCode(200);

        RestAssured.given()
                .contentType(ContentType.TEXT).get("/controlbus/status")
                .then().body(equalTo("Started"));
    }

    @Test
    public void testFail() {
        RestAssured.given()
                .contentType(ContentType.TEXT).get("/controlbus/status")
                .then().body(equalTo("Started"));

        RestAssured.given()
                .contentType(ContentType.TEXT).get("/controlbus/fail")
                .then().statusCode(200);

        RestAssured.given()
                .contentType(ContentType.TEXT).get("/controlbus/status")
                .then().body(equalTo("Stopped"));
    }

    @Test
    public void testRestart() {
        RestAssured.given()
                .contentType(ContentType.TEXT).get("/controlbus/status")
                .then().body(equalTo("Started"));

        RestAssured.given()
                .get("/controlbus/restart")
                .then().body("startCount", equalTo(1))
                .body("stopCount", equalTo(1));

        RestAssured.given()
                .contentType(ContentType.TEXT).get("/controlbus/status")
                .then().body(equalTo("Started"));
    }
}
