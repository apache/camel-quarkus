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

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import graphql.GraphQL;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.graphql.GraphQLHandler;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.quarkus.component.graphql.it.model.Book;
import org.apache.camel.util.json.JsonObject;

@Path("/graphql")
public class GraphQLResource {

    private static final List<Book> BOOKS = Arrays.asList(
            new Book("book-1", "Harry Potter and the Philosophers Stone", "author-1"),
            new Book("book-2", "Moby Dick", "author-2"),
            new Book("book-3", "Interview with the vampire", "author-3"));

    @Inject
    ProducerTemplate producerTemplate;

    public void setupRouter(@Observes Router router) {
        SchemaParser schemaParser = new SchemaParser();
        final TypeDefinitionRegistry typeDefinitionRegistry;
        try (Reader r = new InputStreamReader(getClass().getClassLoader().getResourceAsStream("graphql/schema.graphql"),
                StandardCharsets.UTF_8)) {
            typeDefinitionRegistry = schemaParser.parse(r);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        DataFetcher<CompletionStage<Book>> dataFetcher = environment -> {
            CompletableFuture<Book> completableFuture = new CompletableFuture<>();
            Book book = getBookById(environment);
            completableFuture.complete(book);
            return completableFuture;
        };

        RuntimeWiring runtimeWiring = RuntimeWiring.newRuntimeWiring()
                .type("Query", builder -> builder.dataFetcher("bookById", dataFetcher))
                .build();

        SchemaGenerator schemaGenerator = new SchemaGenerator();
        GraphQLSchema graphQLSchema = schemaGenerator.makeExecutableSchema(typeDefinitionRegistry, runtimeWiring);

        GraphQL graphQL = GraphQL.newGraphQL(graphQLSchema).build();

        router.post().handler(BodyHandler.create());
        router.route("/graphql/server").handler(GraphQLHandler.create(graphQL));
    }

    @Path("/query")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response multipleQueries(@QueryParam("testPort") int port, @QueryParam("bookId") String bookId) {
        JsonObject variables = new JsonObject();
        variables.put("id", bookId);

        producerTemplate.getCamelContext().getRegistry().bind("bookByIdQueryVariables", variables);

        final String result = producerTemplate.requestBody(
                "graphql://http://localhost:" + port
                        + "/graphql/server?queryFile=graphql/bookQuery.graphql&operationName=BookById&variables=#bookByIdQueryVariables",
                null,
                String.class);

        return Response
                .ok()
                .entity(result)
                .build();
    }

    private Book getBookById(DataFetchingEnvironment environment) {
        String bookId = environment.getArgument("id");
        return BOOKS.stream().filter(book -> book.getId().equals(bookId)).findFirst().orElse(null);
    }
}
