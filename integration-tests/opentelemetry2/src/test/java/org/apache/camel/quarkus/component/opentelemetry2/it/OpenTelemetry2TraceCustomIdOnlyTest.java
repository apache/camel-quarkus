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
import io.quarkus.test.junit.TestProfile;
import io.restassured.RestAssured;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.apache.camel.quarkus.component.opentelemetry2.it.OpenTelemetry2TestHelper.getSpans;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestProfile(TraceCustomIdOnlyTestProfile.class)
@QuarkusTest
class OpenTelemetry2TraceCustomIdOnlyTest {
    @AfterEach
    public void afterEach() {
        RestAssured.post("/opentelemetry2/exporter/spans/reset")
                .then()
                .statusCode(204);
    }

    @Test
    void customIdRouteAndNodeAreTraced() {
        RestAssured.get("/opentelemetry2/trace/customIdOnly")
                .then()
                .statusCode(200)
                .body(equalTo("Traced direct:customIdOnly"));

        // Verify the span hierarchy is JAX-RS Service -> Direct Endpoint -> SetBody
        await().atMost(30, TimeUnit.SECONDS).pollDelay(50, TimeUnit.MILLISECONDS).until(() -> getSpans().size() == 4);

        List<Map<String, String>> spans = getSpans();
        assertEquals(4, spans.size());
        assertEquals("EVENT_PROCESS", spans.get(0).get("op"));
        assertEquals("customIdOnlySetBody-setBody", spans.get(0).get("component"));
        assertEquals(spans.get(1).get("spanId"), spans.get(0).get("parentId"));
        assertEquals("EVENT_RECEIVED", spans.get(1).get("op"));
        assertEquals("customIdOnlyRoute", spans.get(1).get("camel.route.id"));
        assertEquals(spans.get(2).get("spanId"), spans.get(1).get("parentId"));
        assertEquals("EVENT_SENT", spans.get(2).get("op"));
        assertEquals(spans.get(3).get("spanId"), spans.get(2).get("parentId"));
        assertEquals(SpanKind.SERVER.name(), spans.get(3).get("kind"));
    }

    @Test
    void nonCustomIdNodeIsNotTraced() {
        RestAssured.get("/opentelemetry2/trace")
                .then()
                .statusCode(200)
                .body(equalTo("Traced direct:start"));

        // The route has a custom id, so it is traced, but its setBody node does not, so no processor span is created
        await().atMost(30, TimeUnit.SECONDS).pollDelay(50, TimeUnit.MILLISECONDS).until(() -> getSpans().size() == 3);

        List<Map<String, String>> spans = getSpans();
        assertEquals(3, spans.size());
        assertEquals("EVENT_RECEIVED", spans.get(0).get("op"));
        assertEquals("tracedRoute", spans.get(0).get("camel.route.id"));
        assertEquals("EVENT_SENT", spans.get(1).get("op"));
        assertEquals(SpanKind.SERVER.name(), spans.get(2).get("kind"));
        assertTrue(spans.stream().noneMatch(span -> "EVENT_PROCESS".equals(span.get("op"))));
    }

    @Test
    void nonCustomIdRouteIsNotTraced() {
        String name = "Camel Quarkus OpenTelemetry";
        RestAssured.get("/opentelemetry2/greetBean/" + name)
                .then()
                .statusCode(200)
                .body(equalTo("Hello " + name));

        // The route has no custom id, so no Camel spans are created for it
        await().atMost(30, TimeUnit.SECONDS).pollDelay(50, TimeUnit.MILLISECONDS).until(() -> getSpans().size() == 2);

        List<Map<String, String>> spans = getSpans();
        assertEquals(2, spans.size());
        assertTrue(spans.stream().noneMatch(span -> span.containsKey("camel.route.id")));
        assertTrue(spans.stream().noneMatch(span -> span.containsKey("op")));
        assertEquals(SpanKind.SERVER.name(), spans.get(1).get("kind"));
    }
}
