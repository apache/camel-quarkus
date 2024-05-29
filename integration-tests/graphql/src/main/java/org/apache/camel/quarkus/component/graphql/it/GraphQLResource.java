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
package org.apache.camel.quarkus.component.graphql.it;

import java.util.Map;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.util.json.JsonObject;

@Path("/graphql")
public class GraphQLResource {

    @Inject
    ProducerTemplate producerTemplate;

    @Path("/queryFile")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response multipleQueries(@QueryParam("testPort") int port, @QueryParam("bookId") int bookId) {
        JsonObject variables = new JsonObject();
        variables.put("id", bookId);

        final Map<String, Object> headers = Map.of("port", port);

        final String result = producerTemplate.requestBodyAndHeaders(
                "direct:getBookGraphQL",
                variables, headers,
                String.class);

        return Response
                .ok()
                .entity(result)
                .build();
    }

    @Path("/queryFile/authenticated")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response multipleQueriesAuthenticated(@QueryParam("testPort") int port, @QueryParam("bookId") int bookId) {
        JsonObject variables = new JsonObject();
        variables.put("id", bookId);

        final Map<String, Object> headers = Map.of("port", port);

        final String result = producerTemplate.requestBodyAndHeaders(
                "direct:getBookGraphQLAuthenticated",
                variables, headers,
                String.class);

        return Response
                .ok()
                .entity(result)
                .build();
    }

    @Path("/mutation")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Response mutation(
            @QueryParam("testPort") int port,
            @QueryParam("bookId") int id,
            @QueryParam("author") String author,
            @QueryParam("name") String name) {

        JsonObject bookInput = new JsonObject();
        bookInput.put("id", id);
        bookInput.put("name", name);
        bookInput.put("author", author);
        JsonObject variables = new JsonObject();
        variables.put("bookInput", bookInput);

        final Map<String, Object> headers = Map.of("port", port);

        final String result = producerTemplate.requestBodyAndHeaders(
                "direct:addBookGraphQL",
                variables, headers,
                String.class);

        return Response
                .ok()
                .entity(result)
                .build();
    }

    @Path("/query")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response booksQueryWithStaticQuery(@QueryParam("testPort") int port) {
        final Map<String, Object> headers = Map.of("port", port);

        String result = producerTemplate.requestBodyAndHeaders("direct:getQuery", null, headers, String.class);

        return Response.ok().entity(result).build();
    }

    @Singleton
    @Named("bookByIdQueryVariables")
    public JsonObject bookByIdQueryVariables() {
        JsonObject variables = new JsonObject();
        variables.put("id", 1);
        return variables;
    }

    @Path("/queryVariables")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response queryBookByIdVariables(@QueryParam("testPort") int port) {
        final Map<String, Object> headers = Map.of("port", port);

        String result = producerTemplate.requestBodyAndHeaders("direct:addQueryVariables", null, headers, String.class);

        return Response.ok().entity(result).build();
    }
}
