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

import java.util.Map;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.camel.CamelContext;
import org.apache.camel.micrometer.observability.MicrometerObservabilityTracer;

@Path("/micrometer-observability/exporter")
public class SpanExporterResource {

    @Inject
    InMemorySpanExporter exporter;

    @Inject
    CamelContext camelContext;

    @Path("/spans")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public JsonArray getSpans() {
        JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
        exporter.getFinishedSpanItems().stream()
                .filter(span -> !span.getName().contains("/micrometer-observability/"))
                .forEach(span -> {
                    Map<AttributeKey<?>, Object> attributes = span.getAttributes().asMap();
                    JsonObjectBuilder objectBuilder = Json.createObjectBuilder();
                    objectBuilder.add("spanId", span.getSpanId());
                    objectBuilder.add("traceId", span.getTraceId());
                    objectBuilder.add("parentSpanId", span.getParentSpanId());
                    objectBuilder.add("name", span.getName());
                    attributes.forEach((k, v) -> objectBuilder.add(String.valueOf(k), v.toString()));
                    arrayBuilder.add(objectBuilder.build());
                });
        return arrayBuilder.build();
    }

    @Path("/spans/reset")
    @POST
    public Response resetSpans() {
        exporter.reset();
        return Response.noContent().build();
    }

    /**
     * Returns the live configuration values of the {@link MicrometerObservabilityTracer} registered
     * in the Camel registry. Used by {@code testConfigPropertiesAreWired} to verify that all five
     * {@code quarkus.camel.micrometer-observability.*} properties are correctly passed through
     * {@code CamelMicrometerObservabilityConfig} → {@code MicrometerObservabilityTracerProducer}
     * → the tracer instance.
     */
    @Path("/tracer-config")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject getTracerConfig() {
        MicrometerObservabilityTracer tracer = camelContext.getRegistry()
                .findSingleByType(MicrometerObservabilityTracer.class);
        JsonObjectBuilder builder = Json.createObjectBuilder();
        if (tracer != null) {
            builder.add("excludePatterns", tracer.getExcludePatterns() != null ? tracer.getExcludePatterns() : "");
            builder.add("includePatterns", tracer.getIncludePatterns() != null ? tracer.getIncludePatterns() : "");
            builder.add("traceProcessors", tracer.isTraceProcessors());
            builder.add("disableCoreProcessors", tracer.isDisableCoreProcessors());
            builder.add("traceHeadersInclusion", tracer.isTraceHeadersInclusion());
        }
        return builder.build();
    }
}
