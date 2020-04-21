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
package org.apache.camel.quarkus.component.http.it;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.apache.camel.Exchange;
import org.apache.camel.FluentProducerTemplate;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.EndpointConsumerBuilder;
import org.apache.camel.builder.EndpointProducerBuilder;
import org.apache.camel.builder.endpoint.EndpointRouteBuilder;

@Path("/test")
@ApplicationScoped
public class SendDynamicResource {
    @Inject
    FluentProducerTemplate producerTemplate;

    @Path("/send-dynamic")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String get(@QueryParam("test-port") int port) {
        return producerTemplate
                .withHeader("SendDynamicHttpEndpointPort", port)
                .to("direct:send-dynamic")
                .request(String.class);
    }

    @Path("/service")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject get(@QueryParam("q") String q, @QueryParam("fq") String fq) {
        return Json.createObjectBuilder()
                .add("q", q)
                .add("fq", fq)
                .build();
    }

    @javax.enterprise.inject.Produces
    RoutesBuilder myRoute() {
        return new EndpointRouteBuilder() {
            @Override
            public void configure() throws Exception {
                final EndpointConsumerBuilder trigger = direct(
                        "send-dynamic");
                final EndpointProducerBuilder service = http(
                        "localhost:${header.SendDynamicHttpEndpointPort}/test/service?q=*&fq=publication_date:%5B${date:now-72h:yyyy-MM-dd}T00:00:00Z%20TO%20${date:now-24h:yyyy-MM-dd}T23:59:59Z%5D&wt=xml&indent=false&start=0&rows=100");

                from(trigger)
                        .setHeader(Exchange.HTTP_METHOD).constant("GET")
                        .toD(service)
                        .convertBodyTo(String.class);
            }
        };
    }
}
