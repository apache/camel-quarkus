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
package org.apache.camel.quarkus.messaging.jms;

import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.camel.CamelContext;
import org.apache.camel.ConsumerTemplate;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.quarkus.component.messaging.it.util.scheme.ComponentScheme;

@Path("/messaging/jms")
public class JmsResource {

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    ConsumerTemplate consumerTemplate;

    @Inject
    CamelContext context;

    @Inject
    ComponentScheme componentScheme;

    @Path("/custom/message/listener/factory")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String customMessageListenerContainerFactory(String message) {
        producerTemplate.sendBody(
                componentScheme
                        + ":queue:listener?messageListenerContainerFactory=#customMessageListener",
                message);
        return consumerTemplate.receiveBody(componentScheme + ":queue:listener", 5000,
                String.class);
    }

    @Path("/custom/destination/resolver")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String customDestinationResolver(String message) {
        producerTemplate.sendBody(
                componentScheme
                        + ":queue:ignored?destinationResolver=#customDestinationResolver",
                message);

        // The custom DestinationResolver should have overridden the original queue name to 'destinationOverride'
        return consumerTemplate.receiveBody(componentScheme + ":queue:destinationOverride",
                5000, String.class);
    }

    @Path("/custom/message/converter")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String customMessageConverter(String message) {
        producerTemplate.sendBody(
                componentScheme
                        + ":queue:converter?messageConverter=#customMessageConverter",
                message);
        return consumerTemplate.receiveBody(
                componentScheme
                        + ":queue:converter?messageConverter=#customMessageConverter",
                5000,
                String.class);
    }

    @Path("/transfer/exchange")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    @POST
    public Response testTransferExchange(String message) throws InterruptedException {
        MockEndpoint mockEndpoint = context.getEndpoint("mock:transferExchangeResult", MockEndpoint.class);
        mockEndpoint.expectedMessageCount(1);

        producerTemplate.sendBody(
                componentScheme + ":queue:transferExchange?transferExchange=true", message);

        mockEndpoint.assertIsSatisfied();

        List<Exchange> exchanges = mockEndpoint.getExchanges();
        Exchange exchange = exchanges.get(0);
        String result = exchange.getMessage().getBody(String.class);

        return Response.ok().entity(result).build();
    }

    @Path("/transfer/exception")
    @Produces(MediaType.TEXT_PLAIN)
    @GET
    public Response testTransferException() {
        try {
            producerTemplate.requestBody(
                    componentScheme + ":queue:transferException?transferException=true",
                    "bad payload");
        } catch (RuntimeCamelException e) {
            Class<? extends Throwable> exception = e.getCause().getClass();
            return Response.ok().entity(exception.getName()).build();
        }
        return Response.serverError().build();
    }
}
