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

import java.util.concurrent.TimeUnit;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.is;

/**
 * Integration test for CAMEL-24511: verifies that without {@code SupervisingRouteController}, a paho-mqtt5 consumer
 * route transitions to Stopped (not zombie Started) after a resubscribe failure on automatic reconnect.
 *
 * The broker never sends SUBACK after the initial connection, so the route cannot recover. Without
 * {@code SupervisingRouteController}, there is no retry mechanism — the route stays Stopped.
 */
@QuarkusTest
@TestProfile(ResubscribeNoRecoveryProfile.class)
class PahoMqtt5ResubscribeNoRecoveryTest {

    @InjectFaultyBroker
    FaultyMqtt5Broker broker;

    @Test
    void resubscribeFailureWithoutSupervisingControllerShouldStopRoute() {
        // 1. Wait for the route to be Started (connection 1 to faulty broker succeeds)
        await().atMost(30, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .untilAsserted(() -> RestAssured.get("/paho-mqtt5/routeStatus/" + PahoMqtt5Route.TESTING_ROUTE_ID)
                        .then()
                        .statusCode(200)
                        .body(is("Started")));

        // 2. The faulty broker closes connection 1 after ~2s (simulating connection loss),
        //    Paho auto-reconnects (connection 2),
        //    but the broker never sends SUBACK — keep-alive timeout fires (~7.5s with keepAlive=5),
        //    our fix calls restartRouteAsync(), route stops. Without SupervisingRouteController,
        //    startRoute() also fails (broker still not sending SUBACK) and the route stays Stopped.
        await().atMost(60, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .until(() -> broker.getConnectionCount() >= 2);

        // 3. The route should transition to Stopped — NOT remain as a zombie in Started state
        await().atMost(30, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .untilAsserted(() -> RestAssured.get("/paho-mqtt5/routeStatus/" + PahoMqtt5Route.TESTING_ROUTE_ID)
                        .then()
                        .statusCode(200)
                        .body(is("Stopped")));
    }
}
