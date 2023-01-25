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
package org.apache.camel.quarkus.component.lra.it;

import java.net.URI;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.camel.FluentProducerTemplate;
import org.apache.camel.quarkus.component.lra.it.service.CreditService;
import org.apache.camel.quarkus.component.lra.it.service.OrderManagerService;

@Path("/lra")
@ApplicationScoped
public class LraResource {

    @Inject
    FluentProducerTemplate producerTemplate;

    @Inject
    CreditService creditService;

    @Inject
    OrderManagerService orderManagerService;

    @Path("/order")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public Response createOrder(@QueryParam("amount") int amount, @QueryParam("fail") boolean fail) throws Exception {
        try {
            producerTemplate.to("direct:saga")
                    .withHeader("amount", amount)
                    .withHeader("fail", fail)
                    .request();
        } catch (Exception e) {
            return Response.serverError().build();
        }

        return Response.created(new URI("https://camel.apache.org")).build();
    }

    @Path("/order/count")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public int getOrderCount() {
        return orderManagerService.getOrders().size();
    }

    @Path("/credit/available")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public int getAvailableCredit() {
        return creditService.getCredit();
    }
}
