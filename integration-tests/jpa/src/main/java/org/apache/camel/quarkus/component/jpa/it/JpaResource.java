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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.camel.CamelContext;
import org.apache.camel.ConsumerTemplate;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.quarkus.component.jpa.it.model.Fruit;

@Path("/jpa")
public class JpaResource {

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    ConsumerTemplate consumerTemplate;

    @Inject
    CamelContext context;

    @Path("/get")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @SuppressWarnings("unchecked")
    public List<Fruit> getFruits() {
        return consumerTemplate.receiveBodyNoWait("jpa:" + Fruit.class.getName(), List.class);
    }

    @Path("/get/{id}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Fruit getFruit(@PathParam("id") int id) {
        return producerTemplate.requestBody("jpa:" + Fruit.class.getName() + "?findEntity=true", id, Fruit.class);
    }

    @Path("/get/query/{id}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @SuppressWarnings("unchecked")
    public List<Fruit> getFruitByQuery(@PathParam("id") int id) {
        return producerTemplate.requestBody("jpa:" + Fruit.class.getName() + "?query="
                + "select f from " + Fruit.class.getName() + " f where f.id = " + id,
                null, List.class);
    }

    @Path("/get/query/named/{id}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @SuppressWarnings("unchecked")
    public List<Fruit> getFruitByNamedQuery(@PathParam("id") int id) {
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("fruitId", id);
        context.getRegistry().bind("parameters", queryParams);
        return producerTemplate.requestBody("jpa:" + Fruit.class.getName() + "?namedQuery=findWithId&parameters=#parameters",
                null, List.class);
    }

    @Path("/get/query/native/{id}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @SuppressWarnings("unchecked")
    public String getFruitByNativeQuery(@PathParam("id") int id) {
        List<Object[]> results = producerTemplate.requestBody(
                "jpa:" + Fruit.class.getName() + "?nativeQuery=SELECT * FROM fruit WHERE id = " + id, null, List.class);

        if (results.isEmpty()) {
            throw new IllegalStateException("Expected at least 1 fruit to be retrieved but query returned no results");
        }

        Object[] result = results.get(0);
        return (String) result[1];
    }

    @Path("/post")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    public Response createFruit(String name) throws Exception {
        producerTemplate.sendBody("jpa:" + Fruit.class.getName(), new Fruit(name));
        return Response.created(new URI("https://camel.apache.org/")).build();
    }
}
