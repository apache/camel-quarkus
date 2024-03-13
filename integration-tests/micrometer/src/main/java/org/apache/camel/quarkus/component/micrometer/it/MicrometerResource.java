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
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.micrometer.core.instrument.search.Search;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.micrometer.MicrometerComponent;
import org.apache.camel.component.micrometer.MicrometerConstants;
import org.apache.camel.component.micrometer.eventnotifier.MicrometerEventNotifierService;
import org.apache.camel.component.micrometer.messagehistory.MicrometerMessageHistoryService;
import org.jboss.logging.Logger;

@Path("/micrometer")
public class MicrometerResource {
    private static final Logger LOG = Logger.getLogger(MicrometerResource.class);

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    MeterRegistry meterRegistry;

    @Inject
    @Named("micrometerCustom")
    MicrometerComponent micrometerCustomComponent;

    @Inject
    PrometheusMeterRegistry prometheusMeterRegistry;

    @Inject
    CamelContext camelContext;

    private LinkedList<Integer> list = new LinkedList<>();

    public MicrometerResource(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        meterRegistry.gaugeCollectionSize("example.list.size", Tags.empty(), list);
    }

    // registry values are: custom, standard
    @Path("/metric/{type}/{name}/{registry}")
    @Produces(MediaType.APPLICATION_JSON)
    @GET
    public Response getMetricValue(@PathParam("type") String type, @PathParam("name") String name,
            @PathParam("registry") String registry,
            @QueryParam("tags") String tagValues) {
        List<Tag> tags = new ArrayList<>();
        if (tagValues.length() > 0) {
            String[] tagElements = tagValues.split(",");
            for (String element : tagElements) {
                String[] tagParts = element.split("=");
                tags.add(Tag.of(tagParts[0], tagParts[1]));
            }
        }
        //search only in prometheus registry (not in jmx one), counter test covers among others the unexpected behavior of prometheus
        Search search = ("custom".equals(registry) ? micrometerCustomComponent.getMetricsRegistry() : prometheusMeterRegistry)
                .find(name).tags(tags);
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

    @Path("/counter/{inc}")
    @GET
    public Response counter(@PathParam("inc") int increment) {
        return counter(increment, "counter");
    }

    @Path("/counterCustom/{inc}")
    @GET
    public Response counterCustom(@PathParam("inc") int increment) {
        return counter(increment, "counterCustom");
    }

    @Path("/counterComposite/{inc}")
    @GET
    public Response counterComposite(@PathParam("inc") int increment) {
        return counter(increment, "counterComposite");
    }

    /**
     * If inc is > 0, MicrometerConstants.HEADER_COUNTER_INCREMENT is used
     * If inc is < 0, MicrometerConstants.HEADER_COUNTER_DECREMENT is used (with positive value)
     * If inc == 0, no header is added.
     */
    Response counter(int increment, String route) {
        String path = "direct:" + route;
        if (increment > 0) {
            producerTemplate.sendBodyAndHeader(path, null, MicrometerConstants.HEADER_COUNTER_INCREMENT, increment);
        } else if (increment < 0) {
            producerTemplate.sendBodyAndHeader(path, null, MicrometerConstants.HEADER_COUNTER_DECREMENT,
                    -increment);
        } else {
            producerTemplate.sendBody(path, null);
        }
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

    @Path("/statistics")
    @GET
    public Response statistics() {
        MicrometerEventNotifierService service = camelContext.hasService(MicrometerEventNotifierService.class);
        String json = service.dumpStatisticsAsJson();

        //todo debug logging
        LOG.info("json is " + json);
        LOG.info("Service.started(): " + service.isStarted());
        LOG.info("meter registry is " + service.getMeterRegistry());
        if (service.getMeterRegistry() instanceof CompositeMeterRegistry) {
            LOG.info("composite registry from " + ((CompositeMeterRegistry) service.getMeterRegistry()).getRegistries());
        }
        Optional<Meter> om = service.getMeterRegistry().getMeters().stream()
                .filter(m -> m.getId().getName().contains("camel.routes.added")).findFirst();
        LOG.info("meter `camel.routes.added` " + om.get());
        if (om.isPresent()) {
            LOG.info("value is " + om.get().measure().iterator().next().getValue());
        }
        return Response.ok().entity(json).build();
    }

    @Path("/history")
    @GET
    public Response history() {
        MicrometerMessageHistoryService service = camelContext.hasService(MicrometerMessageHistoryService.class);
        if (service == null) {
            return Response.status(500).entity("History is null").build();
        }
        String json = service.dumpStatisticsAsJson();
        return Response.ok().entity(json).build();
    }

    @Path("/annotations/call/{number}")
    @GET
    public Response annotationsCall(@PathParam("number") int number) {
        producerTemplate.requestBodyAndHeader("direct:annotatedBean", (Object) null, "number", number);
        return Response.ok().build();
    }

    @Path("/getContextManagementName")
    @GET
    public Response getContextManagemetName() throws Exception {
        return Response.ok().entity(camelContext.getManagementName()).build();
    }

    @Path("/sendJmxHistory")
    @GET
    public Response annotationsCall() {
        producerTemplate.sendBody("direct:jmxHistory", "hello");
        return Response.ok().build();
    }

    @Path("/gauge/{number}")
    @GET
    public Response gauge(@PathParam("number") int number) {
        if (number == 2 || number % 2 == 0) {
            // add even numbers to the list
            list.add(number);
        } else {
            // remove items from the list for odd numbers
            try {
                number = list.removeFirst();
            } catch (NoSuchElementException nse) {
                number = 0;
            }
        }
        return Response.ok().build();
    }
}
