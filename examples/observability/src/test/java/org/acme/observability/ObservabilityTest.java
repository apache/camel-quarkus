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
package org.acme.observability;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsInAnyOrder;

@QuarkusTest
public class ObservabilityTest {

    @Test
    public void metrics() {
        // Verify a expected Camel metric is available
        given()
                .when().accept(ContentType.JSON)
                .get("/metrics/application")
                .then()
                .statusCode(200)
                .body(
                        "'camel.context.status;camelContext=camel-quarkus-observability'", is(1));
    }

    @Test
    public void health() {
        // Verify liveness
        given()
                .when().accept(ContentType.JSON)
                .get("/health/live")
                .then()
                .statusCode(200)
                .body("status", Matchers.is("UP"),
                        "checks.name", containsInAnyOrder("camel-liveness-checks", "camel"),
                        "checks.data.custom-liveness-check", containsInAnyOrder(null, "UP"));

        // Verify readiness
        given()
                .when().accept(ContentType.JSON)
                .get("/health/ready")
                .then()
                .statusCode(200)
                .body("status", Matchers.is("UP"),
                        "checks.name", containsInAnyOrder("camel-readiness-checks", "camel", "Uptime readiness check"),
                        "checks.data.custom-readiness-check", containsInAnyOrder(null, "UP"));
    }
}
