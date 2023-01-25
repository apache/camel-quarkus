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
package org.apache.camel.quarkus.component.zendesk.it;

import java.net.URI;

import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import org.apache.camel.ProducerTemplate;
import org.zendesk.client.v2.model.Ticket;

@Path("/zendesk")
public class ZendeskResource {

    @Inject
    ProducerTemplate producerTemplate;

    @Path("/ticket")
    @GET
    public Response getTicket(@QueryParam("ticketId") long ticketId) {
        Ticket result = producerTemplate.requestBody("zendesk:default/getTicket?inBody=id", ticketId, Ticket.class);
        if (result != null) {
            return Response.ok(result.getDescription()).build();
        } else {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    @Path("/ticket")
    @POST
    public Response createTicket(String description) throws Exception {
        Ticket input = new Ticket();
        input.setSubject("Camel Quarkus Test Subject");
        input.setDescription(description);

        Ticket created = producerTemplate.requestBody("zendesk:default/createTicket?inBody=ticket", input, Ticket.class);
        return Response
                .created(new URI("https://camel.apache.org/"))
                .entity(created.getId())
                .build();
    }

    @Path("/ticket")
    @DELETE
    public Response deleteTicket(@QueryParam("ticketId") long ticketId) {
        producerTemplate.requestBody("zendesk:default/deleteTicket?inBody=id", ticketId, Ticket.class);
        return Response.status(Response.Status.NO_CONTENT).build();
    }
}
