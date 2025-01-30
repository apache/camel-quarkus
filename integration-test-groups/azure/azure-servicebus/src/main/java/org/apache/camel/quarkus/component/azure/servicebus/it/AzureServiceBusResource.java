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
package org.apache.camel.quarkus.component.azure.servicebus.it;

import java.net.URI;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.azure.core.util.BinaryData;
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
import org.apache.camel.ConsumerTemplate;
import org.apache.camel.Exchange;
import org.apache.camel.FluentProducerTemplate;
import org.apache.camel.Message;
import org.apache.camel.component.azure.servicebus.ServiceBusConstants;
import org.apache.camel.component.mock.MockEndpoint;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@Path("/azure-servicebus")
@ApplicationScoped
public class AzureServiceBusResource {
    private static final Logger LOG = Logger.getLogger(AzureServiceBusResource.class);

    @ConfigProperty(name = "azure.servicebus.connection.string")
    Optional<String> connectionString;

    @Inject
    FluentProducerTemplate fluentProducerTemplate;
    @Inject
    ConsumerTemplate consumerTemplate;
    @Inject
    CamelContext context;

    @Path("/send/message/{destination}")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public Response sendMessageFromStringPayload(
            @PathParam("destination") String destination,
            @QueryParam("transportType") String transportType,
            @QueryParam("serviceBusType") String serviceBusType,
            @QueryParam("payloadType") String payloadType,
            @QueryParam("directEndpointUri") String directEndpointUri,
            @QueryParam("scheduledEnqueueTime") Long scheduledEnqueueTime,
            String message) throws Exception {
        return sendMessage(destination, transportType, serviceBusType, payloadType, directEndpointUri, scheduledEnqueueTime,
                message);
    }

    @Path("/send/message/{destination}")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response sendMessage(
            @PathParam("destination") String destination,
            @QueryParam("transportType") String transportType,
            @QueryParam("serviceBusType") String serviceBusType,
            @QueryParam("payloadType") String payloadType,
            @QueryParam("directEndpointUri") String directEndpointUri,
            @QueryParam("scheduledEnqueueTime") Long scheduledEnqueueTime,
            Object message) throws Exception {

        if (directEndpointUri == null) {
            directEndpointUri = "direct:send-message";
        }

        Object payload;
        if (payloadType.equals(String.class.getSimpleName())) {
            payload = message;
        } else if (payloadType.equals(byte[].class.getSimpleName())) {
            payload = ((String) message).getBytes();
        } else if (payloadType.equals(BinaryData.class.getSimpleName())) {
            payload = BinaryData.fromString((String) message);
        } else if (payloadType.equals(List.class.getSimpleName())) {
            payload = message;
        } else {
            throw new IllegalArgumentException("Unsupported payload type: " + payloadType);
        }

        OffsetDateTime offsetDateTime = null;
        if (scheduledEnqueueTime != null) {
            offsetDateTime = OffsetDateTime.ofInstant(Instant.ofEpochMilli(scheduledEnqueueTime), ZoneId.systemDefault());
        }

        fluentProducerTemplate.to(directEndpointUri)
                .withHeader("serviceBusType", serviceBusType)
                .withHeader("destination", destination)
                .withHeader("transportType", transportType)
                .withHeader("payloadType", payloadType)
                .withHeader(ServiceBusConstants.SCHEDULED_ENQUEUE_TIME, offsetDateTime)
                .withBody(payload)
                .send();

        return Response.created(new URI("https://camel.apache.org/")).build();
    }

    @Path("/receive/messages")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<Map<String, Object>> receiveMessages(@QueryParam("endpointUri") String endpointUri) {
        final MockEndpoint mockEndpoint = context.getEndpoint(endpointUri, MockEndpoint.class);
        List<Exchange> receivedExchanges = mockEndpoint.getReceivedExchanges();

        if (receivedExchanges.isEmpty()) {
            return Collections.emptyList();
        }

        List<Map<String, Object>> results = new ArrayList<>();
        receivedExchanges.forEach(exchange -> {
            Message message = exchange.getMessage();
            results.add(Map.of(
                    "body", message.getBody(String.class),
                    "sequenceNumber", message.getHeader(ServiceBusConstants.SEQUENCE_NUMBER)));
        });

        return results;
    }

    @Path("/receive/message/{destination}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String receiveMessage(@PathParam("destination") String destination) {
        return consumerTemplate.receiveBody(
                "azure-servicebus:%s?connectionString=RAW(%s)".formatted(destination, connectionString.get()), 10000,
                String.class);
    }

    @Path("/route/{routeId}/start")
    @POST
    public void startRoute(@PathParam("routeId") String routeId) throws Exception {
        LOG.infof("Starting route: %s", routeId);
        context.getRouteController().startRoute(routeId);
    }

    @Path("/route/{routeId}/stop")
    @POST
    public void stopRoute(@PathParam("routeId") String routeId) throws Exception {
        LOG.infof("Stopping route: %s", routeId);
        context.getRouteController().stopRoute(routeId);
    }
}
