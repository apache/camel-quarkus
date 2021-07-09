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

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

import org.apache.camel.CamelContext;
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
    public String route(String statement, @PathParam("route") String route, @Context UriInfo uriInfo) {
        final Map<String, Object> headers = uriInfo.getQueryParameters().entrySet().stream()
                .map(e -> new AbstractMap.SimpleImmutableEntry<String, Object>(e.getKey(), e.getValue().get(0)))
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
        return producerTemplate.requestBodyAndHeaders("direct:" + route, statement, headers, String.class);
    }

    @Path("/mock/{name}/{count}/{timeout}/{part}")
    @Produces(MediaType.TEXT_PLAIN)
    @GET
    public String mockHeader(@PathParam("name") String name, @PathParam("count") int count, @PathParam("timeout") int timeout,
            @PathParam("part") String part) {
        MockEndpoint mock = context.getEndpoint("mock:" + name, MockEndpoint.class);
        mock.setExpectedMessageCount(count);
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
