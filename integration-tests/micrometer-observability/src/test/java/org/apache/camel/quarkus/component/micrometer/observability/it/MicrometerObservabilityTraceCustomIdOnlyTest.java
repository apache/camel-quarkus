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
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.RestAssured;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestProfile(TraceCustomIdOnlyTestProfile.class)
@QuarkusTest
class MicrometerObservabilityTraceCustomIdOnlyTest {

    @AfterEach
    public void afterEach() {
        RestAssured.post("/micrometer-observability/exporter/spans/reset")
                .then()
                .statusCode(204);
    }

    @Test
    void traceCustomIdOnlyIsWired() {
        RestAssured.get("/micrometer-observability/exporter/tracer-config")
                .then()
                .statusCode(200)
                .body("traceCustomIdOnly", equalTo(true));
    }

    @Test
    void customIdRouteAndNodeAreTraced() {
        RestAssured.get("/micrometer-observability/trace-custom-id")
                .then()
                .statusCode(200)
                .body(equalTo("custom-id-response"));

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            List<Map<String, String>> spans = getSpans();
            assertTrue(spans.stream().anyMatch(span -> "custom-id-route".equals(span.get("camel.route.id"))),
                    "Expected a route span for custom-id-route, got: " + spans);
            assertTrue(spans.stream().anyMatch(span -> "custom-id-set-body-setBody".equals(span.get("name"))),
                    "Expected a processor span for custom-id-set-body, got: " + spans);
        });
    }

    @Test
    void nonCustomIdNodeIsNotTraced() {
        RestAssured.get("/micrometer-observability/trace")
                .then()
                .statusCode(200)
                .body(equalTo("traced-response"));

        // The route has a custom id so it is traced, but its setBody node does not, so no processor span is created
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            List<Map<String, String>> spans = getSpans();
            assertTrue(spans.stream().anyMatch(span -> "traced-route".equals(span.get("camel.route.id"))),
                    "Expected a route span for traced-route, got: " + spans);
        });
        List<Map<String, String>> spans = getSpans();
        assertTrue(spans.stream().noneMatch(span -> "EVENT_PROCESS".equals(span.get("op"))),
                "Expected no processor spans, got: " + spans);
    }

    @Test
    void nonCustomIdRouteIsNotTraced() {
        RestAssured.get("/micrometer-observability/trace-no-custom-id")
                .then()
                .statusCode(200)
                .body(equalTo("no-custom-id-response"));

        // Invoke a traced route afterwards so that once its spans are exported, any spans for direct:noCustomId would be too
        RestAssured.get("/micrometer-observability/trace-custom-id")
                .then()
                .statusCode(200);

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            List<Map<String, String>> spans = getSpans();
            assertTrue(spans.stream().anyMatch(span -> "custom-id-route".equals(span.get("camel.route.id"))),
                    "Expected a route span for custom-id-route, got: " + spans);
        });

        // The route has no custom id, so no Camel spans are created for it
        List<Map<String, String>> spans = getSpans();
        assertTrue(spans.stream().noneMatch(span -> "direct://noCustomId".equals(span.get("camel.uri"))),
                "Expected no spans for direct:noCustomId, got: " + spans);
        assertTrue(spans.stream()
                .filter(span -> "EVENT_PROCESS".equals(span.get("op")))
                .allMatch(span -> "custom-id-set-body-setBody".equals(span.get("name"))),
                "Expected no processor spans for direct:noCustomId, got: " + spans);
    }

    private static List<Map<String, String>> getSpans() {
        return RestAssured.get("/micrometer-observability/exporter/spans")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getList("$");
    }
}
