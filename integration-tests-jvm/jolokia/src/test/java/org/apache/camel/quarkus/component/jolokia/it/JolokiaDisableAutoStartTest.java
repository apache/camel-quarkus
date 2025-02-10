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
package org.apache.camel.quarkus.component.jolokia.it;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.Map;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.restassured.RestAssured;
import io.restassured.config.HttpClientConfig;
import org.awaitility.Awaitility;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

@TestProfile(JolokiaDisableAutoStartTest.JolokiaAdditionalPropertiesProfile.class)
@QuarkusTest
class JolokiaDisableAutoStartTest {
    @Test
    void autoStartupDisabled() {
        // Connecting to Jolokia should not be possible
        RestAssured.config = RestAssured.config().httpClient(
                HttpClientConfig.httpClientConfig()
                        .setParam("http.connection.timeout", 1000)
                        .setParam("http.socket.timeout", 1000));

        RestAssured.port = 8778;
        assertThrows(SocketTimeoutException.class, () -> {
            RestAssured.get("/jolokia/")
                    .then()
                    .statusCode(204);
        });

        // Manually start Jolokia
        RestAssured.port = ConfigProvider.getConfig().getValue("quarkus.http.test-port", Integer.class);
        RestAssured.post("/jolokia/start")
                .then()
                .statusCode(204);

        // Verify a basic request is successful
        RestAssured.port = 8778;
        Awaitility.await().atMost(Duration.ofSeconds(5)).pollInterval(Duration.ofMillis(100)).untilAsserted(() -> {
            RestAssured.given()
                    .get("/jolokia/")
                    .then()
                    .statusCode(200)
                    .body("status", equalTo(200));
        });

        // Verify stop. We don't bother putting this in a finally block since a shutdown hook will take care of stopping Jolokia in case of test failure
        RestAssured.port = ConfigProvider.getConfig().getValue("quarkus.http.test-port", Integer.class);
        RestAssured.post("/jolokia/stop")
                .then()
                .statusCode(204);

        // Connecting to Jolokia should not be possible
        RestAssured.port = 8778;
        assertThrows(ConnectException.class, () -> {
            RestAssured.get("/jolokia/")
                    .then()
                    .statusCode(204);
        });
    }

    public static final class JolokiaAdditionalPropertiesProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("quarkus.camel.jolokia.server.auto-start", "false");
        }
    }
}
