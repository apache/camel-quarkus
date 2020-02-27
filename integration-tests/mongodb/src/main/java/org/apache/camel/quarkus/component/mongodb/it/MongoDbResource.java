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
package org.apache.camel.quarkus.component.mongodb.it;

import java.net.URI;
import java.net.URISyntaxException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoIterable;
import org.apache.camel.ProducerTemplate;
import org.bson.Document;

@Path("/mongodb")
@ApplicationScoped
public class MongoDbResource {
    @Inject
    ProducerTemplate producerTemplate;

    @POST
    @Path("/collection/{collectionName}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response writeToCollection(@PathParam("collectionName") String collectionName, String content)
            throws URISyntaxException {

        producerTemplate.sendBody(
                "mongodb:camelMongoClient?database=test&collection=" + collectionName + "&operation=insert&dynamicity=true",
                content);

        return Response
                .created(new URI("https://camel.apache.org/"))
                .build();
    }

    @GET
    @Path("/collection/{collectionName}")
    @Produces(MediaType.APPLICATION_JSON)
    @SuppressWarnings("unchecked")
    public JsonArray getCollection(@PathParam("collectionName") String collectionName) {
        JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();

        MongoIterable<Document> iterable = producerTemplate.requestBody(
                "mongodb:camelMongoClient?database=test&collection=" + collectionName
                        + "&operation=findAll&dynamicity=true&outputType=MongoIterable",
                null, MongoIterable.class);

        MongoCursor<Document> iterator = iterable.iterator();
        while (iterator.hasNext()) {
            Document document = iterator.next();
            JsonObjectBuilder objectBuilder = Json.createObjectBuilder();
            objectBuilder.add("message", (String) document.get("message"));
            arrayBuilder.add(objectBuilder.build());
        }

        return arrayBuilder.build();
    }

    @javax.enterprise.inject.Produces
    @Named("camelMongoClient")
    public MongoClient camelMongoClient() {
        return new MongoClient(
                System.getProperty("camel.mongodb.test-host"),
                Integer.getInteger("camel.mongodb.test-port"));
    }
}
