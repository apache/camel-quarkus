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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoIterable;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObjectBuilder;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.mongodb.MongoDbConstants;
import org.apache.camel.util.CollectionHelper;
import org.bson.Document;
import org.bson.conversions.Bson;

import static com.mongodb.client.model.Filters.eq;

@Path("/mongodb")
@ApplicationScoped
public class MongoDbResource {

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    CamelContext camelContext;

    @Inject
    @Named("results")
    Map<String, List<Document>> results;

    @POST
    @Path("/collection/{collectionName}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response writeToCollection(@PathParam("collectionName") String collectionName, String content,
            @HeaderParam("mongoClientName") String mongoClientName)
            throws URISyntaxException {

        producerTemplate.sendBody(
                String.format("mongodb:%s?database=test&collection=%s&operation=insert&dynamicity=true",
                        mongoClientName, collectionName),
                content);

        return Response
                .created(new URI("https://camel.apache.org/"))
                .build();
    }

    @GET
    @Path("/collection/{collectionName}")
    @Produces(MediaType.APPLICATION_JSON)
    @SuppressWarnings("unchecked")
    public JsonArray getCollection(@PathParam("collectionName") String collectionName,
            @HeaderParam("mongoClientName") String mongoClientName) {
        JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();

        MongoIterable<Document> iterable = producerTemplate.requestBody(
                String.format(
                        "mongodb:%s?database=test&collection=%s&operation=findAll&dynamicity=true&outputType=MongoIterable",
                        mongoClientName, collectionName),
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

    @GET
    @Path("/collectionAsList/{collectionName}")
    @Produces(MediaType.APPLICATION_JSON)
    @SuppressWarnings("unchecked")
    public List getCollectionAsList(@PathParam("collectionName") String collectionName,
            @HeaderParam("mongoClientName") String mongoClientName) {
        JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();

        List<Document> list = producerTemplate.requestBody(
                String.format(
                        "mongodb:%s?database=test&collection=%s&operation=findAll&dynamicity=true&outputType=DocumentList",
                        mongoClientName, collectionName),
                null, List.class);

        return list.stream().map(d -> d.getString("name")).collect(Collectors.toList());
    }

    @GET
    @Path("/searchByNameAsDocument/{collectionName}/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @SuppressWarnings("unchecked")
    public Document searchByNameAsDocument(@PathParam("collectionName") String collectionName,
            @PathParam("name") String name,
            @HeaderParam("mongoClientName") String mongoClientName) {
        Bson query = eq("name", name);
        return producerTemplate.requestBodyAndHeader(
                String.format(
                        "mongodb:%s?database=test&collection=%s&operation=findOneByQuery&dynamicity=true&outputType=Document",
                        mongoClientName, collectionName),
                query,
                MongoDbConstants.DISTINCT_QUERY_FIELD, "name", Document.class);
    }

    @POST
    @Path("/collection/dynamic/{collectionName}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Object dynamic(@PathParam("collectionName") String collectionName, String content,
            @HeaderParam("mongoClientName") String mongoClientName,
            @HeaderParam("dynamicOperation") String operation)
            throws URISyntaxException {

        Object result = producerTemplate.requestBodyAndHeader(
                String.format("mongodb:%s?database=test&collection=%s&operation=insert&dynamicity=true",
                        mongoClientName, collectionName),
                content, MongoDbConstants.OPERATION_HEADER, operation);

        return result;
    }

    @GET
    @Path("/route/{routeId}/{operation}")
    @Produces(MediaType.TEXT_PLAIN)
    public String restartRoute(@PathParam("routeId") String routeId, @PathParam("operation") String operation)
            throws Exception {
        switch (operation) {
        case "stop":
            camelContext.getRouteController().stopRoute(routeId);
            break;
        case "start":
            camelContext.getRouteController().startRoute(routeId);
            break;
        case "status":
            return camelContext.getRouteController().getRouteStatus(routeId).name();

        }

        return null;
    }

    @GET
    @Path("/results/{resultId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Map getResults(@PathParam("resultId") String resultId) {
        final List<Document> list = results.get(resultId);
        synchronized (list) {
            int size = list.size();
            Document last = null;
            if (!list.isEmpty()) {
                last = list.get(size - 1);
            }
            return CollectionHelper.mapOf("size", size, "last", last);
        }
    }

    @GET
    @Path("/resultsReset/{resultId}")
    public void resetResults(@PathParam("resultId") String resultId) {
        final List<Document> list = results.get(resultId);
        synchronized (list) {
            if (!list.isEmpty()) {
                list.clear();
            }
        }
    }

    @Path("/convertMapToDocument")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Map convertMapToDocument(Map input) {
        Document doc = camelContext.getTypeConverter().convertTo(Document.class, input);
        doc.put("clazz", doc.getClass().getName());
        return doc;
    }

    @Path("/convertAnyObjectToDocument")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public Map convertMapToDocument(String input) {

        Document doc = camelContext.getTypeConverter().convertTo(Document.class, new SimplePojo(input));
        doc.put("clazz", doc.getClass().getName());
        return doc;
    }

    @RegisterForReflection
    private static class SimplePojo {
        private String value;

        public SimplePojo(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

}
