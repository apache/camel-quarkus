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
package org.apache.camel.quarkus.component.microprofile.metrics.it;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.apache.camel.ProducerTemplate;

@Path("/microprofile-metrics")
@ApplicationScoped
public class MicroProfileMetricsResource {

    @Inject
    ProducerTemplate template;

    @Path("/counter")
    @GET
    public Response counterIncrement() throws Exception {
        template.sendBody("direct:counter", null);
        return Response.ok().build();
    }

    @Path("/gauge/increment")
    @GET
    public Response gaugeIncrement() throws Exception {
        template.sendBody("direct:gaugeIncrement", null);
        return Response.ok().build();
    }

    @Path("/gauge/decrement")
    @GET
    public Response gaugeDecrement() throws Exception {
        template.sendBody("direct:gaugeDecrement", null);
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
}
