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

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        await().atMost(30, TimeUnit.SECONDS).pollDelay(50, TimeUnit.MILLISECONDS).until(() -> getSpans().size() == 5);
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
        await().atMost(30, TimeUnit.SECONDS).pollDelay(50, TimeUnit.MILLISECONDS).until(() -> getSpans().size() == 2);
        List<Map<String, String>> spans = getSpans();
        assertEquals(2, spans.size());
        assertEquals(spans.get(0).get("parentId"), spans.get(1).get("spanId"));
    }

    @Test
    public void testTracedBean() {
        String name = "Camel Quarkus OpenTelemetry";
        RestAssured.get("/opentelemetry/greet/" + name)
                .then()
                .statusCode(200)
                .body(equalTo("Hello " + name));

        // Verify the span hierarchy is JAX-RS Service -> Direct Endpoint -> Bean Method
        await().atMost(30, TimeUnit.SECONDS).pollDelay(50, TimeUnit.MILLISECONDS).until(() -> getSpans().size() == 3);
        List<Map<String, String>> spans = getSpans();
        assertEquals(3, spans.size());
        assertEquals(spans.get(0).get("parentId"), spans.get(1).get("parentId"));
        assertEquals(spans.get(1).get("parentId"), spans.get(2).get("spanId"));
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
        await().atMost(30, TimeUnit.SECONDS).pollDelay(50, TimeUnit.MILLISECONDS).until(() -> getSpans().size() == 5);
        List<Map<String, String>> spans = getSpans();
        assertEquals(5, spans.size());
        assertEquals(spans.get(0).get("parentId"), spans.get(1).get("parentId"));
        assertEquals(spans.get(0).get("code.function"), "getConnection");

        assertEquals(spans.get(1).get("parentId"), spans.get(2).get("spanId"));
        assertEquals(spans.get(1).get("db.operation"), "SELECT");

        assertEquals(spans.get(2).get("parentId"), spans.get(3).get("spanId"));
        assertEquals(spans.get(2).get("camel.uri"), "bean://jdbcQueryBean");

        assertEquals(spans.get(3).get("parentId"), spans.get(4).get("spanId"));
        assertEquals(spans.get(3).get("camel.uri"), "direct://jdbcQuery");

        assertEquals(spans.get(4).get("parentId"), "0000000000000000");
        assertEquals(spans.get(4).get("code.function"), "jdbcQuery");
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
