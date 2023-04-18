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
package org.apache.camel.quarkus.component.opentelemetry.it;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTestResource(OpenTelemetryTestResource.class)
@QuarkusTest
class OpenTelemetryTest {

    @AfterEach
    public void afterEach() {
        RestAssured.post("/opentelemetry/exporter/spans/reset")
                .then()
                .statusCode(204);
    }

    @Test
    public void testTraceRoute() {
        // Generate messages
        for (int i = 0; i < 5; i++) {
            RestAssured.get("/opentelemetry/test/trace/")
                    .then()
                    .statusCode(200);

            // No spans should be recorded for this route as they are excluded by camel.opentelemetry.exclude-patterns in
            // application.properties
            // TODO: Reinstate this when platform-http route excludes are fixed. For now, a timer endpoint stands in for filter tests
            // https://github.com/apache/camel-quarkus/issues/2897
            // RestAssured.get("/opentelemetry/test/trace/filtered")
            //        .then()
            //        .statusCode(200);
        }

        // Retrieve recorded spans
        List<Map<String, String>> spans = getSpans();
        assertEquals(5, spans.size());

        for (Map<String, String> span : spans) {
            assertEquals("camel-platform-http", span.get("component"));
            assertEquals("200", span.get("http.status_code"));
            assertEquals("GET", span.get("http.method"));
            assertEquals("platform-http:///opentelemetry/test/trace?httpMethodRestrict=GET", span.get("camel.uri"));
            assertTrue(span.get("http.url").endsWith("/opentelemetry/test/trace/"));
        }
    }

    @Test
    public void testTracedCamelRouteInvokedFromJaxRsService() {
        RestAssured.get("/opentelemetry/trace")
                .then()
                .statusCode(200)
                .body(equalTo("Traced direct:start"));

        // Verify the span hierarchy is JAX-RS Service -> Direct Endpoint
        List<Map<String, String>> spans = getSpans();
        assertEquals(2, spans.size());
        assertEquals(spans.get(1).get("spanId"), spans.get(0).get("parentId"));
    }

    @Test
    public void testTracedBean() {
        String name = "Camel Quarkus OpenTelemetry";
        RestAssured.get("/opentelemetry/greet/" + name)
                .then()
                .statusCode(200)
                .body(equalTo("Hello " + name));

        // Verify the span hierarchy is JAX-RS Service -> Direct Endpoint -> Bean Method
        List<Map<String, String>> spans = getSpans();
        assertEquals(3, spans.size());
        assertEquals(spans.get(1).get("parentId"), spans.get(0).get("parentId"));
        assertEquals(spans.get(2).get("spanId"), spans.get(1).get("parentId"));
    }

    @Test
    public void testTracedJdbcQuery() {
        String timestamp = RestAssured.get("/opentelemetry/jdbc/query")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .asString();

        assertTrue(Long.parseLong(timestamp) > 0);

        // Verify the span hierarchy is JAX-RS Service -> Direct Endpoint -> Bean Endpoint -> Bean method -> JDBC query
        await().atMost(30, TimeUnit.SECONDS).pollDelay(50, TimeUnit.MILLISECONDS).until(() -> getSpans().size() >= 4);
        List<Map<String, String>> spans = getSpans();

        boolean asyncProcessingStartedEventPresent = false;
        try {
            Thread.currentThread().getContextClassLoader()
                    .loadClass("org.apache.camel.impl.event.ExchangeAsyncProcessingStartedEvent");
            asyncProcessingStartedEventPresent = true;
        } catch (ClassNotFoundException e) {
            // Ignore
        }

        String expectedDbQuerySpanParent = asyncProcessingStartedEventPresent ? spans.get(0).get("parentId")
                : spans.get(3).get("spanId");
        assertEquals(expectedDbQuerySpanParent, spans.get(0).get("parentId"));
        assertEquals("SELECT", spans.get(0).get("db.operation"));

        assertEquals(spans.get(2).get("spanId"), spans.get(1).get("parentId"));
        assertEquals("bean://jdbcQueryBean", spans.get(1).get("camel.uri"));

        assertEquals(spans.get(3).get("spanId"), spans.get(2).get("parentId"));
        assertEquals("direct://jdbcQuery", spans.get(2).get("camel.uri"));

        assertEquals("0000000000000000", spans.get(3).get("parentId"));
        assertEquals("/opentelemetry/jdbc/query", spans.get(3).get("http.target"));
    }

    private List<Map<String, String>> getSpans() {
        return RestAssured.given()
                .get("/opentelemetry/exporter/spans")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .jsonPath()
                .get();
    }
}
