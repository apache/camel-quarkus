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
package org.apache.camel.quarkus.component.micrometer.it;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.search.Search;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.camel.ProducerTemplate;

@Path("/micrometer")
public class MicrometerResource {

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    MeterRegistry meterRegistry;

    @Inject
    TestMetric counter;

    @Path("/metric/{type}/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @GET
    public Response getMetricValue(@PathParam("type") String type, @PathParam("name") String name,
            @QueryParam("tags") String tagValues) {
        List<Tag> tags = new ArrayList<>();

        if (tagValues.length() > 0) {
            String[] tagElements = tagValues.split(",");
            for (String element : tagElements) {
                String[] tagParts = element.split("=");
                tags.add(Tag.of(tagParts[0], tagParts[1]));
            }
        }

        Search search = meterRegistry.find(name).tags(tags);
        if (search == null) {
            return Response.status(404).build();
        }

        try {
            Response.ResponseBuilder response = Response.ok();
            if (type.equals("counter")) {
                response.entity(search.counter().count());
            } else if (type.equals("gauge")) {
                response.entity(search.gauge().value());
            } else if (type.equals("summary")) {
                response.entity(search.summary().max());
            } else if (type.equals("timer")) {
                response.entity(search.timer().totalTime(TimeUnit.MILLISECONDS));
            } else {
                throw new IllegalArgumentException("Unknown metric type: " + type);
            }

            return response.build();
        } catch (NullPointerException e) {
            //metric does not exist
            return Response.status(500).entity("Metric does not exist").build();
        }
    }

    @Path("/counter")
    @GET
    public Response counter() {
        producerTemplate.sendBody("direct:counter", null);
        return Response.ok().build();
    }

    @Path("/summary")
    @GET
    public Response summarySetValue(@QueryParam("value") int value) {
        producerTemplate.sendBody("direct:summary", value);
        return Response.ok().build();
    }

    @Path("/timer")
    @GET
    public Response timerStartStop() {
        producerTemplate.sendBody("direct:timer", null);
        return Response.ok().build();
    }

    @Path("/log")
    @GET
    public Response logMessage() {
        producerTemplate.requestBody("direct:log", (Object) null);
        return Response.ok().build();
    }

    @Path("/annotations/call/{number}")
    @GET
    public Response annotationsCall(@PathParam("number") int number) {
        producerTemplate.requestBodyAndHeader("direct:annotatedBean", (Object) null, "number", number);
        return Response.ok().build();
    }
}
