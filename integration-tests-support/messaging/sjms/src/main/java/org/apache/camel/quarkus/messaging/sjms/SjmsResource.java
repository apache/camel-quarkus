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
package org.apache.camel.quarkus.messaging.sjms;

import java.net.URI;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;
import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.quarkus.component.messaging.it.util.scheme.ComponentScheme;

@Path("/messaging/sjms")
public class SjmsResource {

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    CamelContext context;

    @Inject
    ComponentScheme componentScheme;

    @Path("/selector")
    @GET
    public void jmsSelector() throws InterruptedException {
        MockEndpoint mockEndpoint = context.getEndpoint("mock:selectorResult", MockEndpoint.class);
        mockEndpoint.expectedBodiesReceived("Camel SJMS Selector Match");

        String uri = componentScheme + ":queue:selectorA";
        producerTemplate.sendBodyAndHeader(uri, "Camel SJMS Selector Not Matched", "foo", "baz");
        producerTemplate.sendBodyAndHeader(uri, "Camel SJMS Selector Match", "foo", "bar");

        mockEndpoint.assertIsSatisfied(5000L);
    }

    @Path("/custom/destination/{destinationName}")
    @POST
    public Response produceMessageWithCustomDestination(
            @PathParam("destinationName") String destinationName,
            String message) throws Exception {
        producerTemplate.sendBodyAndHeader("direct:computedDestination", message, "DestinationName", destinationName);
        return Response.created(new URI("https://camel.apache.org/")).build();
    }
}
