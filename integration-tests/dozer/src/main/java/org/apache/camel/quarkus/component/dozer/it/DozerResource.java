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
package org.apache.camel.quarkus.component.dozer.it;

import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.camel.ProducerTemplate;
import org.apache.camel.quarkus.component.dozer.it.model.CustomerA;
import org.apache.camel.quarkus.component.dozer.it.model.CustomerB;

@Path("/dozer")
@ApplicationScoped
public class DozerResource {

    @Inject
    ProducerTemplate producerTemplate;

    @Path("/map")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public CustomerB dozerMap() {
        return producerTemplate.requestBody("direct:mapWithEndpoint", createCustomerA(), CustomerB.class);
    }

    @Path("/map/using/converter")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public CustomerB dozerMapWithConverter() {
        return producerTemplate.requestBody("direct:mapWithConverter", createCustomerA(), CustomerB.class);
    }

    @Path("/map/using/variable")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public CustomerB dozerMapWithVariable() {
        return producerTemplate.requestBody("direct:mapWithVariable", createCustomerA(), CustomerB.class);
    }

    @Path("/map/using/expression")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public CustomerB dozerMapWithExpression() {
        Map<String, Object> headers = new HashMap<>();
        headers.put("firstName", "Camel");
        headers.put("lastName", "Quarkus");
        return producerTemplate.requestBodyAndHeaders("direct:mapWithExpression", createCustomerA(), headers, CustomerB.class);
    }

    private CustomerA createCustomerA() {
        return new CustomerA("Peter", "Post", "Camel Street", "12345");
    }
}
