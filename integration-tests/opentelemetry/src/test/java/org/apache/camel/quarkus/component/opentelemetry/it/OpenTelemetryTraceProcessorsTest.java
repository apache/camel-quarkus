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

import io.opentelemetry.api.trace.SpanKind;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;

import static org.apache.camel.quarkus.component.opentelemetry.it.OpenTelemetryTestHelper.getSpans;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

@TestProfile(TraceProcessorsTestProfile.class)
@QuarkusTest
class OpenTelemetryTraceProcessorsTest {
    @Test
    void traceProcessors() {
        RestAssured.get("/opentelemetry/trace")
                .then()
                .statusCode(200)
                .body(equalTo("Traced direct:start"));

        // Verify the span hierarchy is JAX-RS Service -> Direct Endpoint -> SetBody
        await().atMost(30, TimeUnit.SECONDS).pollDelay(50, TimeUnit.MILLISECONDS).until(() -> getSpans().size() == 4);
        List<Map<String, String>> spans = getSpans();
        assertEquals(4, spans.size());
        assertEquals(SpanKind.INTERNAL.name(), spans.get(0).get("kind"));
        assertEquals("camel-setBody", spans.get(0).get("component"));
        assertEquals(spans.get(1).get("spanId"), spans.get(0).get("parentId"));
        assertEquals(SpanKind.INTERNAL.name(), spans.get(1).get("kind"));
        assertEquals(spans.get(2).get("spanId"), spans.get(1).get("parentId"));
        assertEquals(SpanKind.CLIENT.name(), spans.get(2).get("kind"));
        assertEquals(spans.get(3).get("spanId"), spans.get(2).get("parentId"));
        assertEquals(SpanKind.SERVER.name(), spans.get(3).get("kind"));
    }
}
