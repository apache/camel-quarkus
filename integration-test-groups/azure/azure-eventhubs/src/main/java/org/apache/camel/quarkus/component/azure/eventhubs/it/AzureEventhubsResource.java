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
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.quarkus.scheduler.Scheduled;
import org.apache.camel.CamelContext;
import org.apache.camel.ConsumerTemplate;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.mock.MockEndpoint;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Path("/azure-eventhubs")
@ApplicationScoped
public class AzureEventhubsResource {

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    ConsumerTemplate consumerTemplate;

    @Inject
    CamelContext context;

    @ConfigProperty(name = "azure.event.hubs.connection.string")
    Optional<String> connectionString;

    private volatile String message;
    private int counter = 0;

    /**
     * For some reason if we send just a single message, it is not always received by the consumer.
     * Sending multiple messages seems to be more reliable.
     */
    @Scheduled(every = "1s")
    void schedule() {
        if (message != null) {
            final String endpointUri = "azure-eventhubs:?connectionString=RAW(" + connectionString.get() + ")";
            producerTemplate.sendBody(endpointUri, message + (counter++));
        }
    }

    @Path("/receive-events")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<String> receiveEvents() throws Exception {

        final MockEndpoint mockEndpoint = context.getEndpoint("mock:azure-consumed", MockEndpoint.class);
        return mockEndpoint.getReceivedExchanges().stream()
                .map(Exchange::getMessage)
                .map(m -> m.getBody(String.class))
                .collect(Collectors.toList());
    }

    @Path("/send-events")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    @Consumes(MediaType.TEXT_PLAIN)
    public Response sendEvents(String body) throws Exception {
        this.message = body; // start sending the messages via schedule()
        return Response.created(new URI("https://camel.apache.org/")).build();
    }

}
