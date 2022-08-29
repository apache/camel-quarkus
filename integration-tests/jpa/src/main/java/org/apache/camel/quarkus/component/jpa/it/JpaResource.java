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
package org.apache.camel.quarkus.component.jpa.it;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.jpa.JpaConstants;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.quarkus.component.jpa.it.model.Fruit;

@Path("/jpa")
public class JpaResource {

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    CamelContext context;

    @Path("/fruit")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @SuppressWarnings("unchecked")
    public List<Fruit> getFruits() {
        return producerTemplate.requestBody("direct:findAll", null, List.class);
    }

    @Path("/fruit")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createFruit(Fruit fruit) throws URISyntaxException {
        producerTemplate.sendBody("direct:store", fruit);
        return Response.created(new URI("https://camel.apache.org/")).build();
    }

    @Path("/fruit/{id}")
    @DELETE
    public Response deleteFruit(@PathParam("id") int id) {
        Fruit fruit = new Fruit();
        fruit.setId(id);
        producerTemplate.sendBody("direct:remove", fruit);
        return Response.ok().build();
    }

    @Path("/fruit/{id}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Fruit getFruit(@PathParam("id") int id) {
        return producerTemplate.requestBody("direct:findById", id, Fruit.class);
    }

    @Path("/fruit/named/{name}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @SuppressWarnings("unchecked")
    public List<Fruit> getFruitByQuery(@PathParam("name") String name) {
        return producerTemplate.requestBody("direct:namedQuery", name, List.class);
    }

    @Path("/fruit/native/{id}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @SuppressWarnings("unchecked")
    public List<Fruit> getFruitByNativeQuery(@PathParam("id") int id) {
        Map<String, Object> params = Collections.singletonMap("id", id);
        return producerTemplate.requestBodyAndHeader("direct:nativeQuery", null, JpaConstants.JPA_PARAMETERS_HEADER, params,
                List.class);
    }

    @Path("/direct/{name}")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response triggerDirectEndpoint(
            @PathParam("name") String name, @HeaderParam("rollback") Boolean rollback,
            @HeaderParam("messageId") String messageId, Fruit fruit) {
        Map<String, Object> headers = new HashMap<>();
        headers.put("rollback", rollback);
        headers.put("messageId", messageId);
        Object response = producerTemplate.requestBodyAndHeaders("direct:" + name, fruit, headers);
        return Response.ok(response).build();
    }

    @Path("/mock/{name}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List getMockContent(@PathParam("name") String name) {
        MockEndpoint mock = context.getEndpoint("mock:" + name, MockEndpoint.class);
        return mock.getReceivedExchanges().stream()
                .map(Exchange::getMessage)
                .map(m -> m.getBody(String.class))
                .collect(Collectors.toList());
    }

    @Path("/mock/{name}")
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    public Response resetMock(@PathParam("name") String name) {
        MockEndpoint mock = context.getEndpoint("mock:" + name, MockEndpoint.class);
        mock.reset();
        return Response.ok().build();
    }
}
