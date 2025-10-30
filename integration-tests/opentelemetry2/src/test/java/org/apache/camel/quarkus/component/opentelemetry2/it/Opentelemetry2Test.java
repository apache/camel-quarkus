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
package org.apache.camel.quarkus.component.opentelemetry2.it;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.opentelemetry.api.trace.SpanKind;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.apache.camel.quarkus.component.opentelemetry2.it.OpenTelemetry2TestHelper.getSpans;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class Opentelemetry2Test {

    @AfterEach
    public void afterEach() {
        RestAssured.post("/opentelemetry2/exporter/spans/reset")
                .then()
                .statusCode(204);
    }

    @Test
    public void testTraceRoute() {
        // Generate messages
        for (int i = 0; i < 5; i++) {
            RestAssured.get("/opentelemetry2/test/trace/")
                    .then()
                    .statusCode(200);

            // No spans should be recorded for this route as they are excluded by camel.opentelemetry2.exclude-patterns in
            // application.properties
            // TODO: Reinstate this when platform-http route excludes are fixed. For now, a timer endpoint stands in for filter tests
            // https://github.com/apache/camel-quarkus/issues/2897
            // RestAssured.get("/opentelemetry2/test/trace/filtered")
            //        .then()
            //        .statusCode(200);
        }

        // Retrieve recorded spans
        await().atMost(30, TimeUnit.SECONDS).pollDelay(50, TimeUnit.MILLISECONDS).until(() -> getSpans().size() == 5);
        List<Map<String, String>> spans = getSpans();
        assertEquals(5, spans.size());

        for (Map<String, String> span : spans) {
            assertEquals("camel-platform-http", span.get("component"));
            assertEquals("200", span.get("http.status_code"));
            assertEquals("GET", span.get("http.method"));
            assertEquals("platform-http:///opentelemetry2/test/trace?httpMethodRestrict=GET", span.get("camel.uri"));
            assertTrue(span.get("http.url").endsWith("/opentelemetry2/test/trace/"));
        }
    }

    @Test
    public void testTracedCamelRouteInvokedFromJaxRsService() {
        RestAssured.get("/opentelemetry2/trace")
                .then()
                .statusCode(200)
                .body(equalTo("Traced direct:start"));

        // Verify the span hierarchy is JAX-RS Service -> Direct Endpoint
        await().atMost(30, TimeUnit.SECONDS).pollDelay(50, TimeUnit.MILLISECONDS).until(() -> getSpans().size() == 3);
        List<Map<String, String>> spans = getSpans();
        assertEquals(3, spans.size());
        assertEquals(spans.get(0).get("parentId"), spans.get(1).get("spanId"));
        assertEquals(SpanKind.CLIENT.name(), spans.get(1).get("kind"));
        assertEquals(SpanKind.SERVER.name(), spans.get(2).get("kind"));
    }

}
