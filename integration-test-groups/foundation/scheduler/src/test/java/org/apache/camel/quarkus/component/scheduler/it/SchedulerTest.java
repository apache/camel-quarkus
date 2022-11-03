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
package org.apache.camel.quarkus.component.scheduler.it;

import java.util.concurrent.TimeUnit;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;

import static org.awaitility.Awaitility.await;

@QuarkusTest
class SchedulerTest {

    @Test
    public void testInitialDelay() throws Exception {
        try {
            RestAssured.given()
                    .post("/scheduler/route/withInitialDelay/true")
                    .then()
                    .statusCode(204);
            await().atMost(5, TimeUnit.SECONDS).until(() -> {
                String body = RestAssured.get("/scheduler/get").then().statusCode(200).extract().body().asString();
                return !body.equals("0");
            });
        } finally {
            RestAssured.given()
                    .post("/scheduler/route/withInitialDelay/false")
                    .then()
                    .statusCode(204);
        }
    }

    @Test
    public void testDelay() throws Exception {
        try {
            RestAssured.given()
                    .post("/scheduler/route/withDelay/true")
                    .then()
                    .statusCode(204);
            await().atMost(10, TimeUnit.SECONDS).until(() -> {
                String body = RestAssured.get("/scheduler/get-delay-count").then().statusCode(200).extract().body()
                        .asString();
                return Integer.parseInt(body) > 2;
            });
        } finally {
            RestAssured.given()
                    .post("/scheduler/route/withDelay/false")
                    .then()
                    .statusCode(204);
        }

    }

    @Test
    public void testFixedDelay() throws Exception {
        try {
            RestAssured.given()
                    .post("/scheduler/route/useFixedDelay/true")
                    .then()
                    .statusCode(204);
            await().atMost(2, TimeUnit.SECONDS).until(() -> {
                String body = RestAssured.get("/scheduler/get-fixed-delay-count").then().statusCode(200).extract()
                        .body()
                        .asString();
                return Integer.parseInt(body) > 2;
            });
        } finally {
            RestAssured.given()
                    .post("/scheduler/route/useFixedDelay/flase")
                    .then()
                    .statusCode(204);
        }
    }

    @Test
    public void testDelayWithRepeat() throws Exception {
        try {
            RestAssured.given()
                    .post("/scheduler/route/withDelayRepeat/true")
                    .then()
                    .statusCode(204);
            await().atMost(4, TimeUnit.SECONDS).until(() -> {
                String body = RestAssured.get("/scheduler/get-repeat-count").then().statusCode(200).extract().body()
                        .asString();
                return Integer.parseInt(body) >= 4;
            });
        } finally {
            RestAssured.given()
                    .post("/scheduler/route/withDelayRepeat/false")
                    .then()
                    .statusCode(204);
        }
    }

    @Test
    public void testGreedyScheduler() throws Exception {
        try {
            RestAssured.given()
                    .post("/scheduler/route/greedy/true")
                    .then()
                    .statusCode(204);
            await().atMost(2, TimeUnit.SECONDS).until(() -> {
                String body = RestAssured.get("/scheduler/get-greedy-count").then().statusCode(200).extract().body()
                        .asString();
                return Integer.parseInt(body) > 10;
            });
        } finally {
            RestAssured.given()
                    .post("/scheduler/route/greedy/false")
                    .then()
                    .statusCode(204);
        }

    }
}
