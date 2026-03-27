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
package org.apache.camel.quarkus.component.aws2.mq.it;

import java.util.concurrent.TimeUnit;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static org.hamcrest.Matchers.hasItem;

@QuarkusTest
@QuarkusTestResource(Aws2MqTestResource.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class Aws2MqTest {

    static final String BROKER_NAME = "cq-test-broker";
    static final String BROKER_POJO_NAME = "cq-pojo-test-broker";

    static String brokerId;
    static String brokerPojoId;

    @Test
    @Order(1)
    public void testCreateBrokers() {
        brokerId = RestAssured.given()
                .post("/aws2-mq/broker/" + BROKER_NAME)
                .then()
                .statusCode(201)
                .extract().body().asString();

        brokerPojoId = RestAssured.given()
                .post("/aws2-mq/broker/" + BROKER_POJO_NAME + "/pojo")
                .then()
                .statusCode(201)
                .extract().body().asString();

        // Wait for both brokers to become RUNNING before running further tests
        Awaitility.await()
                .atMost(30, TimeUnit.MINUTES)
                .pollDelay(0, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.MINUTES)
                .until(() -> "RUNNING".equals(brokerState(brokerId))
                        && "RUNNING".equals(brokerState(brokerPojoId)));
    }

    @Test
    @Order(2)
    public void testListBrokers() {
        // List all brokers
        RestAssured.given()
                .get("/aws2-mq/brokers")
                .then()
                .statusCode(200)
                .body("$", hasItem(BROKER_NAME));

        // List all brokers via pojo
        RestAssured.given()
                .get("/aws2-mq/brokers")
                .then()
                .statusCode(200)
                .body("$", hasItem(BROKER_POJO_NAME));
    }

    @Test
    @Order(4)
    public void testUpdateBrokers() {
        // Fetch current configuration ID
        String configId = RestAssured.given()
                .queryParam("brokerId", brokerId)
                .get("/aws2-mq/broker/configurationId")
                .then()
                .statusCode(200)
                .extract().body().asString();

        // Update via headers with explicit configuration ID
        RestAssured.given()
                .queryParam("brokerId", brokerId)
                .queryParam("configurationId", configId)
                .put("/aws2-mq/broker")
                .then()
                .statusCode(200);

        // Update via pojo
        RestAssured.given()
                .queryParam("brokerId", brokerPojoId)
                .put("/aws2-mq/broker/pojo")
                .then()
                .statusCode(200);
    }

    @Test
    @Order(5)
    public void testRebootBrokers() {
        // Reboot via headers
        RestAssured.given()
                .post("/aws2-mq/broker/" + brokerId + "/reboot")
                .then()
                .statusCode(204);

        // Reboot via pojo
        RestAssured.given()
                .post("/aws2-mq/broker/" + brokerPojoId + "/reboot/pojo")
                .then()
                .statusCode(204);

        // Wait for both brokers to become RUNNING again after reboot
        Awaitility.await()
                .atMost(15, TimeUnit.MINUTES)
                .pollDelay(0, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.MINUTES)
                .until(() -> "RUNNING".equals(brokerState(brokerId))
                        && "RUNNING".equals(brokerState(brokerPojoId)));
    }

    @Test
    @Order(6)
    public void testDeleteBrokers() {
        RestAssured.given()
                .queryParam("brokerId", brokerId)
                .delete("/aws2-mq/broker")
                .then()
                .statusCode(204);

        RestAssured.given()
                .queryParam("brokerId", brokerPojoId)
                .delete("/aws2-mq/broker/pojo")
                .then()
                .statusCode(204);
    }

    @Test
    @Order(7)
    public void cleanUpVerification() {
        // Wait until both brokers are no longer listed as deletion is asynchronous in AWS
        Awaitility.await()
                .atMost(30, TimeUnit.MINUTES)
                .pollDelay(0, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.MINUTES)
                .until(() -> {
                    java.util.List<?> brokers = RestAssured.given()
                            .get("/aws2-mq/brokers")
                            .then()
                            .statusCode(200)
                            .extract().jsonPath().getList("$");
                    return !brokers.contains(BROKER_NAME) && !brokers.contains(BROKER_POJO_NAME);
                });
    }

    private static String brokerState(String id) {
        return RestAssured.given()
                .queryParam("brokerId", id)
                .get("/aws2-mq/broker/describe")
                .then()
                .statusCode(200)
                .extract().body().asString();
    }
}
