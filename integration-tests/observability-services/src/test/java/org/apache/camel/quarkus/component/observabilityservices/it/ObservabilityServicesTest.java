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
package org.apache.camel.quarkus.component.observabilityservices.it;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
class ObservabilityServicesTest {

    @Test
    void testHealthUpStatus() {
        RestAssured.when().get("/observe/health").then()
                .contentType(ContentType.JSON)
                .header("Content-Type", containsString("charset=UTF-8"))
                .body("status", is("UP"),
                        "checks.status.findAll().unique()", contains("UP"),
                        "checks.find { it.name == 'camel-routes' }", notNullValue(),
                        "checks.find { it.name == 'camel-consumers' }", notNullValue(),
                        "checks.find { it.name == 'context' }", notNullValue(),
                        "checks.find { it.name == 'context' }.data.'context.name'", notNullValue());
    }

    @Test
    void testLivenessUpStatus() {
        RestAssured.when().get("/observe/health/live").then()
                .contentType(ContentType.JSON)
                .header("Content-Type", containsString("charset=UTF-8"))
                .body("status", is("UP"),
                        "checks.status.findAll().unique()", contains("UP"));
    }

    @Test
    void testReadinessUpStatus() {
        RestAssured.when().get("/observe/health/ready").then()
                .contentType(ContentType.JSON)
                .header("Content-Type", containsString("charset=UTF-8"))
                .body("status", is("UP"),
                        "checks.status.findAll().unique()", contains("UP"));
    }

    @Test
    void testMetricsStatus() {
        RestAssured.when().get("/observe/metrics").then()
                .header("Content-Type", containsString("application/openmetrics-text"))
                .statusCode(HttpStatus.SC_OK);
    }

    @Test
    void metricsRegistry() {
        RestAssured.get("/observability-services/registry")
                .then()
                .statusCode(200)
                .body(is(PrometheusMeterRegistry.class.getName()));
    }

    @Test
    void traceRoute() {
        try {
            String message = UUID.randomUUID().toString();
            RestAssured.given()
                    .body(message)
                    .post("/observability-services/trace")
                    .then()
                    .statusCode(200)
                    .body(is("modified " + message));

            await().atMost(30, TimeUnit.SECONDS).pollDelay(50, TimeUnit.MILLISECONDS).until(() -> getSpans().size() == 5);
            List<Map<String, String>> spans = getSpans();

            assertEquals(spans.get(0).get("parentId"), spans.get(1).get("spanId"));
            assertEquals("seda://next", spans.get(0).get("camel.uri"));
            assertEquals("INTERNAL", spans.get(0).get("kind"));

            assertEquals(spans.get(1).get("parentId"), spans.get(2).get("spanId"));
            assertEquals("seda://next", spans.get(1).get("camel.uri"));
            assertEquals("INTERNAL", spans.get(1).get("kind"));

            assertEquals(spans.get(2).get("parentId"), spans.get(3).get("spanId"));
            assertEquals("direct://start", spans.get(2).get("camel.uri"));
            assertEquals("INTERNAL", spans.get(2).get("kind"));

            assertEquals("0000000000000000", spans.get(3).get("parentId"));
            assertEquals("direct://start", spans.get(3).get("camel.uri"));
            assertEquals("INTERNAL", spans.get(3).get("kind"));
        } finally {
            RestAssured.given()
                    .post("/spans/reset")
                    .then()
                    .statusCode(204);
        }
    }

    @Test
    void resolveMBeanAttribute() {
        String name = "org.apache.camel:type=context,*";
        RestAssured.given()
                .queryParam("name", name)
                .queryParam("attribute", "CamelId")
                .get("/observability-services/jmx/attribute")
                .then()
                .statusCode(200)
                .body(is("observability-services-context"));
    }

    static List<Map<String, String>> getSpans() {
        return RestAssured.given()
                .get("/spans/export")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .jsonPath()
                .get();
    }
}
