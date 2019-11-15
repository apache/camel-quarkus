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
package org.apache.camel.quarkus.component.opentracing.it;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.opentracing.Tracer;
import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;

@Path("/opentracing")
@ApplicationScoped
public class OpenTracingResource {

    @Inject
    Tracer tracer;

    @Path("/spans")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public JsonArray getSpans() {
        JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();

        MockTracer mockTracer = (MockTracer) tracer;
        for (MockSpan span : mockTracer.finishedSpans()) {
            MockSpan.MockContext context = span.context();

            JsonObjectBuilder objectBuilder = Json.createObjectBuilder();
            objectBuilder.add("spanId", context.spanId());
            objectBuilder.add("traceId", context.traceId());

            span.tags().forEach((k, v) -> objectBuilder.add(k, v.toString()));

            arrayBuilder.add(objectBuilder.build());
        }

        return arrayBuilder.build();
    }
}
