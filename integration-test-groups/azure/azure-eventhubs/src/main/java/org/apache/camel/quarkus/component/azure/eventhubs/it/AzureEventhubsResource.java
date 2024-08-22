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
package org.apache.camel.quarkus.component.azure.eventhubs.it;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.azure.eventhubs.EventHubsConstants;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.util.ObjectHelper;
import org.jboss.logging.Logger;

@Path("/azure-eventhubs")
@ApplicationScoped
public class AzureEventhubsResource {
    private static final Logger LOG = Logger.getLogger(AzureEventhubsResource.class);

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    CamelContext context;

    @Path("/receive-event")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> receiveEvent(@QueryParam("endpointUri") String endpointUri, String match) {
        final MockEndpoint mockEndpoint = context.getEndpoint(endpointUri, MockEndpoint.class);
        List<Exchange> receivedExchanges = mockEndpoint.getReceivedExchanges();

        Optional<Exchange> optionalExchange = receivedExchanges.stream()
                .filter(exchange -> exchange.getMessage().getBody(String.class).equals(match))
                .findFirst();

        if (optionalExchange.isEmpty()) {
            return Collections.emptyMap();
        }

        Exchange exchange = optionalExchange.get();
        Message message = exchange.getMessage();
        return Map.of(
                "body", message.getBody(String.class),
                "headers", message.getHeaders());
    }

    @Path("/send-event/{partitionId}")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public Response sendEvent(
            @PathParam("partitionId") String partitionId,
            @QueryParam("endpointUri") String endpointUri,
            String message) throws Exception {

        if (ObjectHelper.isEmpty(endpointUri)) {
            endpointUri = "direct:sendEvent";
        }

        LOG.infof("Producing event to endpoint uri: %s", endpointUri);

        producerTemplate.sendBodyAndHeader(endpointUri, message, EventHubsConstants.PARTITION_ID, partitionId);
        return Response.created(new URI("https://camel.apache.org/")).build();
    }

    @Path("/receive-events")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<Map<String, Object>> receiveEvents(@QueryParam("endpointUri") String endpointUri, List<String> matches) {
        final MockEndpoint mockEndpoint = context.getEndpoint(endpointUri, MockEndpoint.class);
        List<Exchange> receivedExchanges = mockEndpoint.getReceivedExchanges();

        List<Exchange> exchanges = receivedExchanges.stream()
                .filter(exchange -> matches.contains(exchange.getMessage().getBody(String.class)))
                .toList();

        if (exchanges.isEmpty()) {
            return Collections.emptyList();
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Exchange exchange : exchanges) {
            Message message = exchange.getMessage();
            result.add(Map.of(
                    "body", message.getBody(String.class),
                    "headers", message.getHeaders()));
        }

        return result;
    }

    @Path("/send-events/{partitionId}")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response sendEvents(@PathParam("partitionId") String partitionId, List<String> messages) throws Exception {
        producerTemplate.sendBodyAndHeader("direct:sendEvent", messages, EventHubsConstants.PARTITION_ID, partitionId);
        return Response.created(new URI("https://camel.apache.org/")).build();
    }

    @Path("/route/{routeId}/start")
    @POST
    public void startRoute(@PathParam("routeId") String routeId) throws Exception {
        LOG.infof("Starting route: %s", routeId);
        context.getRouteController().startRoute(routeId);
        // A random jitter value is applied in the Event Hubs client before its message listener is active.
        // In addition, claiming ownership of partitions seems to take an indeterminate amount of time.
        // Therefore, we need to wait until it's safe to produce events
        Thread.sleep(5000);
    }

    @Path("/route/{routeId}/stop")
    @POST
    public void stopRoute(@PathParam("routeId") String routeId) throws Exception {
        context.getRouteController().stopRoute(routeId);
    }
}
