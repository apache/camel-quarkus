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
package org.apache.camel.quarkus.component.knative.producer.it;

import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.cloudevents.CloudEvents;
import org.apache.camel.component.knative.KnativeComponent;
import org.apache.camel.component.mock.MockEndpoint;

@Path("/knative-producer")
@ApplicationScoped
public class KnativeProducerResource {
    @Inject
    CamelContext context;

    @Inject
    ProducerTemplate producerTemplate;

    @GET
    @Path("inspect")
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject inspect() {
        var component = context.getComponent("knative", KnativeComponent.class);
        var builder = Json.createObjectBuilder();

        if (component.getProducerFactory() != null) {
            builder.add("producer-factory", component.getProducerFactory().getClass().getName());
        }
        if (component.getConsumerFactory() != null) {
            builder.add("consumer-factory", component.getConsumerFactory().getClass().getName());
        }

        return builder.build();
    }

    @GET
    @Path("/send/{type}/{msg}")
    public Response sendMessageToChannel(@PathParam("type") String type, @PathParam("msg") String message) {
        producerTemplate.sendBody(String.format("direct:%s", type), message);
        return Response.ok().build();
    }

    @Path("/mock/{name}")
    @GET
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public boolean validProducedCloudEvent(@PathParam("name") String endpointName) {
        MockEndpoint mockEndpoint = context.getEndpoint(String.format("mock:%s", endpointName), MockEndpoint.class);
        Exchange result = mockEndpoint.getReceivedExchanges().stream()
                .findAny().get();

        // check there is a time
        String time = result.getMessage().getHeader("ce-time", String.class);
        if (time == null) {
            return false;
        }
        // check the time is valid
        try {
            DateTimeFormatter.ISO_INSTANT.parse(time);
        } catch (DateTimeParseException e) {
            // if no valid time format return false
            return false;
        }
        // check there is an id
        String id = result.getMessage().getHeader("ce-id", String.class);
        if (id == null) {
            return false;
        }
        // check there is a source
        String source = result.getMessage().getHeader("ce-source", String.class);
        if (source == null) {
            return false;
        }
        // check there is a type
        String type = result.getMessage().getHeader("ce-type", String.class);
        if (type == null) {
            return false;
        }
        // check there is a spec version
        String specVersion = result.getMessage().getHeader("ce-specversion", String.class);
        if (specVersion == null) {
            return false;
        }
        // check spec version is valid
        if (CloudEvents.v1_0.version().equals(specVersion) || CloudEvents.v1_0_1.version().equals(specVersion)) {
            return true;
        }

        // spec version invalid
        return false;

    }
}
