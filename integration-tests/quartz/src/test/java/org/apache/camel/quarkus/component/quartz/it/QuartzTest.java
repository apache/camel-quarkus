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
package org.apache.camel.quarkus.component.quartz.it;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hamcrest.Matchers.is;

@QuarkusTest
class QuartzTest {

    @ParameterizedTest()
    @ValueSource(strings = { "cron", "quartz" })
    public void testSchedulerComponent(String component) {
        RestAssured.given()
                .queryParam("fromEndpoint", component)
                .get("/quartz/get")
                .then()
                .statusCode(200)
                .body(is("Hello Camel Quarkus " + component));
    }

    @Test
    public void testProperties() {
        RestAssured.given()
                .queryParam("fromEndpoint", "quartz-properties")
                .queryParam("componentName", "quartzFromProperties")
                .get("/quartz/getNameAndResult")
                .then()
                .statusCode(200)
                .body("name", is("MyScheduler-"),
                        "result", is("Hello Camel Quarkus Quartz Properties"));
    }

    @Test
    public void testCronTrigger() {
        RestAssured.given()
                .queryParam("fromEndpoint", "quartz-cron-trigger")
                .get("/quartz/get")
                .then()
                .statusCode(200)
                .body(is("Hello Camel Quarkus Quartz From Cron Trigger"));

    }

    @Test
    public void testHeaders() {
        RestAssured.given()
                .queryParam("fromEndpoint", "quartz")
                .get("/quartz/getHeaders")
                .then()
                .statusCode(200)
                .body("triggerName", is("1 * * * * "));
    }

    @Test
    public void testMisfire() {
        RestAssured.given()
                .queryParam("fromEndpoint", "quartz-cron-misfire")
                .get("/quartz/getMisfire")
                .then()
                .statusCode(200)
                .body("timezone", is("Europe/Stockholm"),
                        "misfire", is("2"));
    }
}
