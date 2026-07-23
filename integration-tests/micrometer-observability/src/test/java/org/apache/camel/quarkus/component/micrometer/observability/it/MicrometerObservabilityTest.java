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
package org.apache.camel.quarkus.component.micrometer.observability.it;

import java.util.List;
import java.util.concurrent.TimeUnit;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class MicrometerObservabilityTest {

    @AfterEach
    public void afterEach() {
        RestAssured.post("/micrometer-observability/exporter/spans/reset")
                .then()
                .statusCode(204);
    }

    /**
     * Verifies that the Quarkus extension correctly wires the MicrometerObservabilityTracer
     * with the Quarkus-provided OpenTelemetry bean (via OtelTracer bridge) and that Camel routes
     * produce spans that reach the OTel SDK pipeline (InMemorySpanExporter).
     *
     * This validates the full chain: Camel → MicrometerObservabilityTracer → OtelTracer bridge → OTel SDK.
     */
    @Test
    void testSpansAreGenerated() {
        RestAssured.get("/micrometer-observability/trace")
                .then()
                .statusCode(200)
                .body(equalTo("traced-response"));

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            JsonPath spans = RestAssured.get("/micrometer-observability/exporter/spans")
                    .then().statusCode(200).extract().jsonPath();

            List<Object> spanList = spans.getList("$");
            assertFalse(spanList.isEmpty(),
                    "Expected at least one span from direct:traced route — OtelTracer bridge wiring may be broken");
        });
    }

    /**
     * Verifies that quarkus.camel.micrometer-observability.exclude-patterns is correctly applied.
     * The route direct:excluded is listed in exclude-patterns so it must produce no spans.
     * The route direct:traced (not excluded) must produce at least one span.
     */
    @Test
    void testExcludePatternsAreApplied() {
        RestAssured.get("/micrometer-observability/trace-excluded")
                .then()
                .statusCode(200)
                .body(equalTo("excluded-response"));

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            JsonPath spans = RestAssured.get("/micrometer-observability/exporter/spans")
                    .then().statusCode(200).extract().jsonPath();
            List<Object> spanList = spans.getList("$");
            assertTrue(spanList.isEmpty(),
                    "direct:excluded is in exclude-patterns — no spans expected, got: " + spanList);
        });

        RestAssured.post("/micrometer-observability/exporter/spans/reset").then().statusCode(204);
        RestAssured.get("/micrometer-observability/trace").then().statusCode(200);

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            JsonPath spans = RestAssured.get("/micrometer-observability/exporter/spans")
                    .then().statusCode(200).extract().jsonPath();
            List<Object> spanList = spans.getList("$");
            assertFalse(spanList.isEmpty(),
                    "direct:traced is not excluded — expected spans, got none");
        });
    }

    /**
     * Verifies that the CDI bean {@code CamelMicrometerObservabilityConfig} correctly picks up
     * all five {@code quarkus.camel.micrometer-observability.*} properties from
     * {@code application.properties} and that {@code MicrometerObservabilityTracerProducer}
     * passes them through to the live {@code MicrometerObservabilityTracer}.
     *
     * This is purely a Quarkus extension concern: camel upstream is responsible for what these
     * options do at runtime; we only verify the configuration wiring is not broken.
     */
    @Test
    void testConfigPropertiesAreWired() {
        JsonPath config = RestAssured.get("/micrometer-observability/exporter/tracer-config")
                .then().statusCode(200).extract().jsonPath();

        assertEquals("direct:excluded", config.getString("excludePatterns"),
                "exclude-patterns must be wired to the tracer");
        assertEquals("direct:traced", config.getString("includePatterns"),
                "include-patterns must be wired to the tracer");
        assertTrue(config.getBoolean("traceProcessors"),
                "trace-processors=true must be wired to the tracer");
        assertTrue(config.getBoolean("disableCoreProcessors"),
                "disable-core-processors=true must be wired to the tracer");
        assertTrue(config.getBoolean("traceHeadersInclusion"),
                "trace-headers-inclusion=true must be wired to the tracer");
    }

    /**
     * Verifies that W3C traceparent propagation works end-to-end.
     * A request carrying a {@code traceparent} header must cause all Camel spans to be
     * attached to the external trace (same traceId).
     */
    @Test
    void testUpstreamW3CTracePropagation() {
        String externalTraceId = "0af044aea5c127fd5ab5f839de2b8ae2";
        String externalSpanId = "d362a8a943c2b289";
        String traceparent = "00-" + externalTraceId + "-" + externalSpanId + "-01";

        RestAssured.given()
                .header("traceparent", traceparent)
                .get("/micrometer-observability/trace-upstream")
                .then()
                .statusCode(200);

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            JsonPath spans = RestAssured.get("/micrometer-observability/exporter/spans")
                    .then().statusCode(200).extract().jsonPath();

            List<String> traceIds = spans.getList("traceId");
            assertFalse(traceIds.isEmpty(), "Expected at least one span from the upstream-propagation request");

            // All Camel spans must share the external traceId
            for (String traceId : traceIds) {
                assertEquals(externalTraceId, traceId,
                        "Camel span traceId must match the upstream traceparent traceId");
            }
        });
    }
}
