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

@TestProfile(TraceProcessorsTestProfile.class)
@QuarkusTest
class OpenTelemetry2TraceProcessorsTest {
    @AfterEach
    public void afterEach() {
        RestAssured.post("/opentelemetry2/exporter/spans/reset")
                .then()
                .statusCode(204);
    }

    @Test
    void traceProcessors() {
        RestAssured.get("/opentelemetry2/trace")
                .then()
                .statusCode(200)
                .body(equalTo("Traced direct:start"));

        // Verify the span hierarchy is JAX-RS Service -> Direct Endpoint -> SetBody
        await().atMost(30, TimeUnit.SECONDS).pollDelay(50, TimeUnit.MILLISECONDS).until(() -> {
            List<Map<String, String>> spans = getSpans();
            return spans.size() == 4;
        });

        List<Map<String, String>> spans = getSpans();
        assertEquals(4, spans.size());
        assertEquals(SpanKind.INTERNAL.name(), spans.get(0).get("kind"));
        assertEquals("setBody3-setBody", spans.get(0).get("component"));
        assertEquals(spans.get(1).get("spanId"), spans.get(0).get("parentId"));
        assertEquals(SpanKind.INTERNAL.name(), spans.get(1).get("kind"));
        assertEquals(spans.get(2).get("spanId"), spans.get(1).get("parentId"));
        assertEquals(SpanKind.INTERNAL.name(), spans.get(2).get("kind"));

        // TODO: Restore this assertion - https://github.com/apache/camel-quarkus/issues/7813
        // assertEquals(spans.get(2).get("parentId"), spans.get(3).get("spanId"));

        // TODO: See above - remove this assertion. For now we expect the the JAX-RS service span to be disconnected
        assertEquals(spans.get(3).get("parentId"), "0000000000000000");
        assertEquals(SpanKind.SERVER.name(), spans.get(3).get("kind"));
    }
}
