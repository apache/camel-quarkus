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
package org.apache.camel.quarkus.eip.it;

import java.util.AbstractMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.apache.camel.CamelContext;
import org.apache.camel.CamelExecutionException;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.mock.MockEndpoint;

@Path("/eip")
@ApplicationScoped
public class EipResource {

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    CamelContext context;

    @Path("/route/{route}")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public Response route(String statement, @PathParam("route") String route, @Context UriInfo uriInfo) {
        final Map<String, Object> headers = uriInfo.getQueryParameters().entrySet().stream()
                .map(e -> new AbstractMap.SimpleImmutableEntry<String, Object>(e.getKey(), e.getValue().get(0)))
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
        try {
            String result = producerTemplate.requestBodyAndHeaders("direct:" + route, statement, headers, String.class);
            return Response.ok(result).build();
        } catch (CamelExecutionException e) {
            return Response.serverError().entity(e.getMessage()).build();
        }
    }

    @Path("/mock/{name}/{count}/{timeout}/{part}")
    @Produces(MediaType.TEXT_PLAIN)
    @GET
    public String mockHeader(@PathParam("name") String name, @PathParam("count") String count,
            @PathParam("timeout") int timeout,
            @PathParam("part") String part) {
        MockEndpoint mock = context.getEndpoint("mock:" + name, MockEndpoint.class);

        if (count.endsWith("+")) {
            mock.setMinimumExpectedMessageCount(Integer.valueOf(count.substring(0, count.length() - 1)));
        } else {
            mock.setExpectedMessageCount(Integer.valueOf(count));
        }
        try {
            mock.assertIsSatisfied(timeout);
        } catch (InterruptedException e1) {
            Thread.currentThread().interrupt();
        }
        switch (part) {
        case "body":
            return mock.getExchanges().stream().map(e -> e.getMessage().getBody(String.class)).collect(Collectors.joining(","));
        case "header":
            return mock.getExchanges().stream()
                    .flatMap(e -> e.getMessage().getHeaders().entrySet().stream()
                            .map(entry -> entry.getKey() + "=" + entry.getValue()))
                    .collect(Collectors.joining(","));
        case "property":
            return mock.getExchanges().stream()
                    .flatMap(e -> e.getProperties().entrySet().stream()
                            .map(entry -> entry.getKey() + "=" + entry.getValue()))
                    .collect(Collectors.joining(","));
        default:
            throw new IllegalStateException("Unexpected part " + part);
        }
    }

}
