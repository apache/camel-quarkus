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
package org.apache.camel.quarkus.component.disruptor.it;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.camel.CamelContext;
import org.apache.camel.ConsumerTemplate;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.disruptor.DisruptorEndpoint;
import org.apache.camel.component.disruptor.DisruptorNotStartedException;

@Path("/disruptor")
@ApplicationScoped
public class DisruptorResource {
    public static final String DISRUPTOR = "disruptor";
    public static final String DISRUPTOR_VM = "disruptor-vm";

    @Inject
    CamelContext context;
    @Inject
    ProducerTemplate producerTemplate;
    @Inject
    ConsumerTemplate consumerTemplate;

    @Path("/component/{componentName}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response loadComponent(@PathParam("componentName") String componentName) {
        return context.getComponent(componentName) != null
                ? Response.ok().build()
                : Response.status(404, componentName + " could not be loaded from the Camel context").build();
    }

    @Path("/buffer/{name}")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    public void request(@PathParam("name") String name, String value) {
        producerTemplate.sendBody(DISRUPTOR + ":" + name, value);
    }

    @Path("/buffer/{name}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String request(@PathParam("name") String name) {
        return consumerTemplate.receiveBody(DISRUPTOR + ":" + name, String.class);
    }

    @Path("/buffer/{name}/inspect")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject size(@PathParam("name") String name) throws DisruptorNotStartedException {
        DisruptorEndpoint endpoint = context.getEndpoint(DISRUPTOR + ":" + name, DisruptorEndpoint.class);

        return Json.createObjectBuilder()
                .add("pendingExchangeCount", endpoint.getPendingExchangeCount())
                .add("size", endpoint.getSize())
                .build();
    }
}
