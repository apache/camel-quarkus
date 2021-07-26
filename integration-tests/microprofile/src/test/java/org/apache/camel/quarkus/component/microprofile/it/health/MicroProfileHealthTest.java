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
package org.apache.camel.quarkus.component.microprofile.it.health;

import java.util.List;
import java.util.concurrent.TimeUnit;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import org.awaitility.Awaitility;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

@QuarkusTest
class MicroProfileHealthTest {

    //@Test
    public void testHealthUpStatus() {
        RestAssured.when().get("/q/health").then()
                .contentType(ContentType.JSON)
                .header("Content-Type", containsString("charset=UTF-8"))
                .body("status", is("UP"),
                        "checks.status", containsInAnyOrder("UP", "UP"),
                        "checks.name",
                        containsInAnyOrder("camel-readiness-checks", "camel-liveness-checks"),
                        "checks.data.context", containsInAnyOrder(null, "UP"),
                        "checks.data.'route:healthyRoute'", containsInAnyOrder(null, "UP"),
                        "checks.data.always-up", containsInAnyOrder("UP", "UP"));
    }

    //@Test
    public void testHealthDownStatus() {
        try {
            RestAssured.get("/microprofile-health/checks/failing/true")
                    .then()
                    .statusCode(204);

            RestAssured.when().get("/q/health").then()
                    .contentType(ContentType.JSON)
                    .header("Content-Type", containsString("charset=UTF-8"))
                    .body("status", is("DOWN"),
                            "checks.status", containsInAnyOrder("DOWN", "DOWN"),
                            "checks.name",
                            containsInAnyOrder("camel-readiness-checks", "camel-liveness-checks"),
                            "checks.data.context", containsInAnyOrder(null, "UP"));
        } finally {
            RestAssured.get("/microprofile-health/checks/failing/false")
                    .then()
                    .statusCode(204);
        }
    }

    //@Test
    public void testLivenessUpStatus() {
        RestAssured.when().get("/q/health/live").then()
                .contentType(ContentType.JSON)
                .header("Content-Type", containsString("charset=UTF-8"))
                .body("status", is("UP"),
                        "checks.status", containsInAnyOrder("UP"),
                        "checks.name", containsInAnyOrder("camel-liveness-checks"),
                        "checks.data.test", containsInAnyOrder("UP"),
                        "checks.data.test-liveness", containsInAnyOrder("UP"));
    }

    //@Test
    public void testLivenessDownStatus() {
        try {
            RestAssured.get("/microprofile-health/checks/failing/true")
                    .then()
                    .statusCode(204);

            RestAssured.when().get("/q/health/live").then()
                    .contentType(ContentType.JSON)
                    .header("Content-Type", containsString("charset=UTF-8"))
                    .body("status", is("DOWN"),
                            "checks.status", containsInAnyOrder("DOWN"),
                            "checks.name", containsInAnyOrder("camel-liveness-checks"),
                            "checks.data.test", containsInAnyOrder("UP"),
                            "checks.data.test-liveness", containsInAnyOrder("UP"),
                            "checks.data.failing-check", containsInAnyOrder("DOWN"));
        } finally {
            RestAssured.get("/microprofile-health/checks/failing/false")
                    .then()
                    .statusCode(204);
        }
    }

    //@Test
    public void testReadinessUpStatus() {
        RestAssured.when().get("/q/health/ready").then()
                .contentType(ContentType.JSON)
                .header("Content-Type", containsString("charset=UTF-8"))
                .body("status", is("UP"),
                        "checks.status", containsInAnyOrder("UP"),
                        "checks.name", containsInAnyOrder("camel-readiness-checks"),
                        "checks.data.context", containsInAnyOrder("UP"),
                        "checks.data.test-readiness", containsInAnyOrder("UP"));
    }

    //@Test
    public void testReadinessDownStatus() {
        try {
            RestAssured.get("/microprofile-health/checks/failing/true")
                    .then()
                    .statusCode(204);

            RestAssured.when().get("/q/health/ready").then()
                    .contentType(ContentType.JSON)
                    .header("Content-Type", containsString("charset=UTF-8"))
                    .body("status", is("DOWN"),
                            "checks.status", containsInAnyOrder("DOWN"),
                            "checks.name", containsInAnyOrder("camel-readiness-checks"),
                            "checks.data.context", containsInAnyOrder("UP"),
                            "checks.data.test-readiness", containsInAnyOrder("UP"));
        } finally {
            RestAssured.get("/microprofile-health/checks/failing/false")
                    .then()
                    .statusCode(204);
        }
    }

    //@Test
    public void testRouteStoppedDownStatus() {
        try {
            RestAssured.get("/microprofile-health/route/healthyRoute/stop")
                    .then()
                    .statusCode(204);

            RestAssured.when().get("/q/health").then()
                    .contentType(ContentType.JSON)
                    .header("Content-Type", containsString("charset=UTF-8"))
                    .body("status", is("DOWN"),
                            "checks.data.'route:healthyRoute'", containsInAnyOrder(null, "DOWN"));
        } finally {
            RestAssured.get("/microprofile-health/route/healthyRoute/start")
                    .then()
                    .statusCode(204);
        }
    }

    //@Test
    public void testFailureThreshold() {
        try {
            RestAssured.get("/microprofile-health/route/checkIntervalThreshold/stop")
                    .then()
                    .statusCode(204);

            // Configured failure threshold and interval should allow the initial health state be UP
            RestAssured.when().get("/q/health").then()
                    .contentType(ContentType.JSON)
                    .header("Content-Type", containsString("charset=UTF-8"))
                    .body("status", is("UP"),
                            "checks.data.'route:checkIntervalThreshold'", containsInAnyOrder(null, "UP"));

            // Poll the health endpoint until the threshold / interval is exceeded and the health state transitions to DOWN
            Awaitility.await().atMost(10, TimeUnit.SECONDS).pollDelay(50, TimeUnit.MILLISECONDS).until(() -> {
                JsonPath result = RestAssured.when().get("/q/health").then()
                        .contentType(ContentType.JSON)
                        .header("Content-Type", containsString("charset=UTF-8"))
                        .extract()
                        .jsonPath();

                String status = result.getString("status");
                List<String> routeStatus = result.getList("checks.data.'route:checkIntervalThreshold'");
                return status.equals("DOWN") && routeStatus.contains("DOWN");
            });
        } finally {
            RestAssured.get("/microprofile-health/route/checkIntervalThreshold/start")
                    .then()
                    .statusCode(204);

            // Wait for the threshold check to report status UP
            Awaitility.await().atMost(10, TimeUnit.SECONDS).pollDelay(50, TimeUnit.MILLISECONDS).until(() -> {
                JsonPath result = RestAssured.when().get("/q/health").then()
                        .contentType(ContentType.JSON)
                        .header("Content-Type", containsString("charset=UTF-8"))
                        .extract()
                        .jsonPath();

                String status = result.getString("status");
                List<String> routeStatus = result.getList("checks.data.'route:checkIntervalThreshold'");
                return status.equals("UP") && routeStatus.contains("UP");
            });
        }
    }
}
