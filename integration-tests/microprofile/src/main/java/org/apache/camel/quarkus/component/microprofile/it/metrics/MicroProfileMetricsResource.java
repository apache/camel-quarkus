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
package org.apache.camel.quarkus.component.microprofile.it.metrics;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWithRouteBuilder;

@Path("/microprofile-metrics")
@ApplicationScoped
public class MicroProfileMetricsResource {

    @Inject
    ProducerTemplate template;

    @Inject
    CamelContext context;

    @Path("/counter")
    @GET
    public Response counterIncrement() throws Exception {
        template.sendBody("direct:counter", null);
        return Response.ok().build();
    }

    @Path("/gauge/concurrent/increment")
    @GET
    public Response gaugeIncrement() throws Exception {
        template.sendBody("direct:concurrentGaugeIncrement", null);
        return Response.ok().build();
    }

    @Path("/gauge/concurrent/decrement")
    @GET
    public Response gaugeDecrement() throws Exception {
        template.sendBody("direct:concurrentGaugeDecrement", null);
        return Response.ok().build();
    }

    @Path("/gauge")
    @GET
    public Response gaugeSetValue(@QueryParam("value") int value) throws Exception {
        template.sendBody("direct:gauge", value);
        return Response.ok().build();
    }

    @Path("/histogram")
    @GET
    public Response histogramSetValue(@QueryParam("value") int value) throws Exception {
        template.sendBody("direct:histogram", value);
        return Response.ok().build();
    }

    @Path("/meter")
    @GET
    public Response meterSetMark(@QueryParam("mark") int mark) throws Exception {
        template.sendBody("direct:meter", mark);
        return Response.ok().build();
    }

    @Path("/timer")
    @GET
    public Response timerStartStop() throws Exception {
        template.sendBody("direct:timer", null);
        return Response.ok().build();
    }

    @Path("/log")
    @GET
    public Response logMessage() throws Exception {
        template.sendBody("log:message", "Test log message");
        return Response.ok().build();
    }

    @Path("/advicewith")
    @GET
    public Response adviceWith() throws Exception {
        AdviceWithRouteBuilder.adviceWith(context, "log", advisor -> {
            advisor.replaceFromWith("direct:replaced");
        });
        return Response.ok().build();
    }
}
