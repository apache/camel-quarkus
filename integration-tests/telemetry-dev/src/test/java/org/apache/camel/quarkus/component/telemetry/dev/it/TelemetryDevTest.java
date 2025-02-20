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
package org.apache.camel.quarkus.component.telemetry.dev.it;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.apache.camel.telemetrydev.DevSpanAdapter;
import org.apache.camel.telemetrydev.DevTrace;
import org.junit.jupiter.api.Test;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@QuarkusTest
class TelemetryDevTest {

    private final ObjectMapper mapper = new ObjectMapper();

    protected Map<String, DevTrace> tracesFromLog() throws IOException {
        Map<String, DevTrace> answer = new HashMap<>();
        Path path = Paths.get("target/traces.log");
        List<String> allTraces = Files.readAllLines(path);
        for (String trace : allTraces) {
            DevTrace st = mapper.readValue(trace, DevTrace.class);
            if (answer.get(st.getTraceId()) != null) {
                // Multiple traces exists for this traceId: this may happen
                // when we deal with async events (like wiretap and the like)
                DevTrace existing = answer.get(st.getTraceId());
                List<DevSpanAdapter> mergedSpans = st.getSpans();
                mergedSpans.addAll(existing.getSpans());
                st = new DevTrace(st.getTraceId(), mergedSpans);
            }
            answer.put(st.getTraceId(), st);
        }

        return answer;
    }

    @Test
    public void testTracedCamelRoute() throws IOException {
        RestAssured.get("/telemetrydev/trace")
                .then()
                .statusCode(200)
                .body(equalTo("Traced direct:start"));

        await().atMost(30, TimeUnit.SECONDS).pollDelay(50, TimeUnit.MILLISECONDS).until(() -> tracesFromLog().size() == 1);
        DevTrace trace = tracesFromLog().values().iterator().next();
        assertNotNull(trace.getSpans());
        assertEquals(2, trace.getSpans().size());
        assertNull(trace.getSpans().get(0).getTag("parentSpan"));
        assertEquals(trace.getSpans().get(0).getTag("spanid"), trace.getSpans().get(1).getTag("parentSpan"));
    }

}
