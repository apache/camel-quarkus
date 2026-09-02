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
 * Integration test for CAMEL-24511: verifies that a paho-mqtt5 consumer route recovers after a resubscribe failure on
 * automatic reconnect, instead of silently becoming a zombie.
 *
 * Uses {@link FaultyMqtt5Broker} (a Java TCP server) to simulate a broker that accepts the initial connection normally
 * but fails to send SUBACK on connections 2 and 3 (auto-reconnect and first restart attempt), triggering keep-alive
 * timeouts. The fix in {@code PahoMqtt5Consumer.restartRouteAsync()} detects this and restarts the route. The first
 * restart fails (connection 3), but {@code SupervisingRouteController} retries with exponential backoff, and the route
 * recovers on connection 4 when the broker starts responding normally again.
 */
@QuarkusTest
@TestProfile(ResubscribeFailureProfile.class)
class PahoMqtt5ResubscribeRecoveryTest {

    @InjectFaultyBroker
    FaultyMqtt5Broker broker;

    @Test
    void resubscribeFailureShouldTriggerRouteRestartAndRecovery() {
        // 1. Wait for the route to be Started (connection 1 to faulty broker succeeds)
        await().atMost(30, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .untilAsserted(() -> RestAssured.get("/paho-mqtt5/routeStatus/" + PahoMqtt5Route.TESTING_ROUTE_ID)
                        .then()
                        .statusCode(200)
                        .body(is("Started")));

        // 2. The faulty broker closes connection 1 after ~2s (simulating connection loss),
        //    Paho auto-reconnects (connection 2),
        //    but the broker ignores SUBSCRIBE — keep-alive timeout fires (~7.5s with keepAlive=5),
        //    our fix calls restartRouteAsync(), route stops. The restart attempt (connection 3)
        //    also fails (broker still ignores SUBSCRIBE). SupervisingRouteController retries with
        //    backoff, and connection 4 succeeds (broker behaves normally from connection 4 onward).
        //    Wait for at least 4 connections — proves the SupervisingRouteController retry happened.
        await().atMost(90, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .until(() -> broker.getConnectionCount() >= 4);

        // 3. After SupervisingRouteController retries, the route should recover to Started
        await().atMost(30, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .untilAsserted(() -> RestAssured.get("/paho-mqtt5/routeStatus/" + PahoMqtt5Route.TESTING_ROUTE_ID)
                        .then()
                        .statusCode(200)
                        .body(is("Started")));
    }
}
