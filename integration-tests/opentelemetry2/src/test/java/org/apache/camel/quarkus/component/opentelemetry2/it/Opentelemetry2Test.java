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
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static io.opentelemetry.semconv.incubating.CodeIncubatingAttributes.CODE_FUNCTION_NAME;
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

    @Disabled("https://github.com/apache/camel-quarkus/issues/7813")
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

    @Disabled("https://github.com/apache/camel-quarkus/issues/7813")
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

    @Test
    public void testTracedBean() {
        String name = "Camel Quarkus OpenTelemetry";
        RestAssured.get("/opentelemetry2/greet/" + name)
                .then()
                .statusCode(200)
                .body(equalTo("Hello " + name));

        // Verify the span hierarchy is JAX-RS Service -> Direct Endpoint -> Bean Method
        await().atMost(30, TimeUnit.SECONDS).pollDelay(50, TimeUnit.MILLISECONDS).until(() -> getSpans().size() == 5);
        List<Map<String, String>> spans = getSpans();
        assertEquals(5, spans.size());
        assertEquals(spans.get(0).get("parentId"), spans.get(1).get("spanId"));
        assertEquals(spans.get(1).get("parentId"), spans.get(2).get("spanId"));
        assertEquals(spans.get(2).get("parentId"), spans.get(3).get("spanId"));
        assertEquals(SpanKind.INTERNAL.name(), spans.get(3).get("kind"));
        assertEquals(SpanKind.SERVER.name(), spans.get(4).get("kind"));
    }

    @Test
    public void testTracedJdbcQuery() {
        String timestamp = RestAssured.get("/opentelemetry2/jdbc/query")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .asString();

        assertTrue(Long.parseLong(timestamp) > 0);

        // Verify the span hierarchy is JAX-RS Service -> Direct Endpoint -> Bean Endpoint -> Bean method -> JDBC query
        await().atMost(30, TimeUnit.SECONDS).pollDelay(50, TimeUnit.MILLISECONDS).until(() -> getSpans().size() == 6);
        List<Map<String, String>> spans = getSpans();
        assertEquals(6, spans.size());
        assertEquals(spans.get(0).get("parentId"), spans.get(1).get("parentId"));
        assertEquals("getConnection", spans.get(0).get("code.function"));

        assertEquals(spans.get(1).get("parentId"), spans.get(2).get("spanId"));
        assertEquals("SELECT", spans.get(1).get("db.operation"));

        assertEquals(spans.get(2).get("parentId"), spans.get(3).get("spanId"));
        assertEquals("bean://jdbcQueryBean", spans.get(2).get("camel.uri"));

        assertEquals(spans.get(3).get("parentId"), spans.get(4).get("spanId"));
        assertEquals("direct://jdbcQuery", spans.get(3).get("camel.uri"));
        assertEquals("EVENT_RECEIVED", spans.get(3).get("op"));

        // TODO: Restore this assertion - https://github.com/apache/camel-quarkus/issues/7813
        // assertEquals(spans.get(4).get("parentId"), spans.get(5).get("spanId"));

        // TODO: See above - remove this assertion. For now we expect the the JAX-RS service and the direct:jdbcQuery spans to be disconnected
        assertEquals(spans.get(4).get("parentId"), "0000000000000000");
        assertEquals("direct://jdbcQuery", spans.get(4).get("camel.uri"));
        assertEquals("EVENT_SENT", spans.get(4).get("op"));

        assertEquals(spans.get(5).get("parentId"), "0000000000000000");
        assertEquals("org.apache.camel.quarkus.component.opentelemetry2.it.OpenTelemetry2Resource.jdbcQuery",
                spans.get(5).get(CODE_FUNCTION_NAME.getKey()));
    }
}
