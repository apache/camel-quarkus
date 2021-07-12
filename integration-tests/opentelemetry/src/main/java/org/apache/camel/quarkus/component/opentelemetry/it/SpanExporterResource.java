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

import java.util.Map;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.data.SpanData;

@Path("/opentelemetry/exporter")
public class SpanExporterResource {

    @Inject
    InMemorySpanExporter exporter;

    @Path("/spans")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public JsonArray getSpans() {
        JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();

        for (SpanData span : exporter.getFinishedSpanItems()) {
            if (span.getName().contains("exporter")) {
                // Ignore any trace events on this resource
                continue;
            }

            Map<AttributeKey<?>, Object> attributes = span.getAttributes().asMap();
            JsonObjectBuilder objectBuilder = Json.createObjectBuilder();
            objectBuilder.add("spanId", span.getSpanId());
            objectBuilder.add("traceId", span.getTraceId());
            objectBuilder.add("parentId", span.getParentSpanId());

            attributes.forEach((k, v) -> objectBuilder.add(String.valueOf(k), v.toString()));

            arrayBuilder.add(objectBuilder.build());
        }

        return arrayBuilder.build();
    }

    @POST
    @Path("/spans/reset")
    public void resetSpanExporter() {
        exporter.reset();
    }
}
