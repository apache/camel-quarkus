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
package org.apache.camel.quarkus.component.smooks.it;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.apache.camel.ConsumerTemplate;
import org.apache.camel.FluentProducerTemplate;
import org.apache.camel.quarkus.component.smooks.it.model.Customer;

@Path("/smooks")
public class SmooksResource {
    @Inject
    FluentProducerTemplate fluentProducerTemplate;

    @Inject
    ConsumerTemplate consumerTemplate;

    @Path("/bean/routing")
    @POST
    @Consumes(MediaType.APPLICATION_XML)
    public void beanRouting(String customerXml) {
        fluentProducerTemplate.to("direct:beanRouting")
                .withBody(customerXml)
                .send();
    }

    @Path("/dataformat/marshal")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_XML)
    public String dataFormatMarshal(Customer customer) {
        return fluentProducerTemplate.to("direct:smooksDataFormatMarshal")
                .withBody(customer)
                .request(String.class)
                // Simplify XMl root tag for RestAssured assertions
                .replace(Customer.class.getName(), Customer.class.getSimpleName());
    }

    @Path("/dataformat/unmarshal")
    @POST
    @Consumes(MediaType.APPLICATION_XML)
    @Produces(MediaType.APPLICATION_JSON)
    public Customer dataFormatUnmarshal(String customerXml) {
        return fluentProducerTemplate.to("direct:smooksDataFormatUnmarshal")
                .withBody(customerXml)
                .request(Customer.class);
    }

    @Path("/bean/routing/endpoint/{endpointUri}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Object assertBeanRoutingEndpoint(@PathParam("endpointUri") String endpointUri) {
        return consumerTemplate.receiveBody(endpointUri, 10000L, Customer.class);
    }
}
