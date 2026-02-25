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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import io.restassured.RestAssured;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;

class OpenTelemetry2TestHelper {
    private static final Logger LOG = Logger.getLogger(OpenTelemetry2TestHelper.class);
    private static final boolean DEBUG_SPAN_HIERARCHY = ConfigProvider.getConfig().getValue("debug.span.hierarchy.enabled",
            boolean.class);

    private OpenTelemetry2TestHelper() {
        // Utility class
    }

    static List<Map<String, String>> getSpans() {
        List<Map<String, String>> spans = RestAssured.given()
                .get("/opentelemetry2/exporter/spans")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .jsonPath()
                .get();

        if (DEBUG_SPAN_HIERARCHY) {
            printSpanHierarchy(spans);
        }

        return spans;
    }

    public static void printSpanHierarchy(List<Map<String, String>> spans) {
        if (spans.isEmpty()) {
            return;
        }

        Map<String, List<Map<String, String>>> spansByTraceId = spans.stream()
                .collect(Collectors.groupingBy(span -> span.get("traceId")));

        for (Map.Entry<String, List<Map<String, String>>> entry : spansByTraceId.entrySet()) {
            LOG.infof("Trace ID: %s", entry.getKey());
            printTraceHierarchy(entry.getValue());
        }
    }

    private static void printTraceHierarchy(List<Map<String, String>> spans) {
        Map<String, List<Map<String, String>>> childrenByParentId = new HashMap<>();
        Set<String> spanIds = spans.stream().map(span -> span.get("spanId")).collect(Collectors.toSet());

        for (Map<String, String> span : spans) {
            String parentId = span.get("parentId");
            childrenByParentId.computeIfAbsent(parentId, k -> new ArrayList<>()).add(span);
        }

        List<Map<String, String>> rootSpans = spans.stream()
                .filter(span -> !spanIds.contains(span.get("parentId")))
                .toList();

        for (Map<String, String> rootSpan : rootSpans) {
            printSpan(rootSpan, childrenByParentId, "");
        }
    }

    private static void printSpan(Map<String, String> span, Map<String, List<Map<String, String>>> childrenByParentId,
            String indent) {
        LOG.infof("%s- %s", indent, spanToString(span));

        List<Map<String, String>> children = childrenByParentId.get(span.get("spanId"));
        if (children != null) {
            for (Map<String, String> child : children) {
                printSpan(child, childrenByParentId, indent + "  ");
            }
        }
    }

    private static String spanToString(Map<String, String> span) {
        StringBuilder sb = new StringBuilder();
        sb.append("spanId=").append(span.get("spanId"));
        sb.append(", parentId=").append(span.get("parentId"));
        sb.append(", kind=").append(span.get("kind"));

        span.entrySet().stream()
                .filter(entry -> !Set.of("traceId", "spanId", "parentId", "kind").contains(entry.getKey()))
                .forEach(entry -> sb.append(", ").append(entry.getKey()).append("=").append(entry.getValue()));

        return sb.toString();
    }
}
