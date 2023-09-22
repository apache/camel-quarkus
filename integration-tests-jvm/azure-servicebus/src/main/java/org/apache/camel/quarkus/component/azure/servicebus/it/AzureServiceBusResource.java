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

import java.util.List;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.mock.MockEndpoint;

@Path("/azure-servicebus")
@ApplicationScoped
public class AzureServiceBusResource {
    @Inject
    ProducerTemplate producerTemplate;
    @Inject
    CamelContext context;

    @Path("/producer")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<String> getBasicProducer() throws Exception {
        return producerTemplate.requestBody("direct:producer-test", null, List.class);
    }

    @Path("/consumer")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<String> getBasicConsumer() throws Exception {
        final MockEndpoint mockEndpoint = context.getEndpoint("mock:azure-servicebus-consumed", MockEndpoint.class);
        return mockEndpoint.getReceivedExchanges().stream()
                .map(Exchange::getMessage)
                .map(m -> m.getBody(String.class))
                .collect(Collectors.toList());
    }
}
