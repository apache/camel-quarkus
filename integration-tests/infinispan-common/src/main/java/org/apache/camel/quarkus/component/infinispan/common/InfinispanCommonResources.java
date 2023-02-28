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
package org.apache.camel.quarkus.component.infinispan.common;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.infinispan.InfinispanConstants;
import org.apache.camel.component.infinispan.InfinispanOperation;
import org.apache.camel.component.infinispan.InfinispanQueryBuilder;
import org.apache.camel.component.infinispan.remote.InfinispanRemoteComponent;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.quarkus.component.infinispan.common.model.Person;
import org.apache.camel.util.CollectionHelper;
import org.infinispan.client.hotrod.ServerStatistics;

import static org.apache.camel.quarkus.component.infinispan.common.InfinispanCommonRoutes.CORRELATOR_HEADER;

@Path("/infinispan")
@ApplicationScoped
public class InfinispanCommonResources {
    public static final String CACHE_NAME = "camel-infinispan";

    @Inject
    ProducerTemplate template;

    @Inject
    CamelContext camelContext;

    @Path("/inspect")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject inspectCamelInfinispanClientConfiguration() {
        InfinispanRemoteComponent component = camelContext.getComponent("infinispan", InfinispanRemoteComponent.class);

        return Json.createObjectBuilder()
                .add("hosts", Objects.toString(component.getConfiguration().getHosts(), "none"))
                .add("cache-manager", Objects.toString(component.getConfiguration().getCacheContainer(), "none"))
                .build();
    }

    @Path("/aggregate")
    @GET
    public void aggregate() throws InterruptedException {
        MockEndpoint mockEndpoint = camelContext.getEndpoint("mock:aggregationResult", MockEndpoint.class);
        mockEndpoint.expectedMessageCount(2);
        mockEndpoint.expectedBodiesReceived(1 + 3 + 4 + 5, 6 + 7 + 20 + 21);

        try {
            Map<String, Object> headers = Map.of(CORRELATOR_HEADER, CORRELATOR_HEADER);

            Stream.of(1, 3, 4, 5, 6, 7, 20, 21)
                    .forEach(value -> template.sendBodyAndHeaders("direct:aggregation", value, headers));

            mockEndpoint.assertIsSatisfied(15000);
        } finally {
            mockEndpoint.reset();
        }
    }

    @Path("/clear")
    @DELETE
    public void clear() {
        template.sendBody("direct:clear", null);
    }

    @Path("/clearAsync")
    @DELETE
    public void clearAsync()
            throws ExecutionException, InterruptedException, TimeoutException {
        CompletableFuture<?> future = template.requestBody("direct:clearAsync", null, CompletableFuture.class);
        future.get(5, TimeUnit.SECONDS);
    }

    @Path("/compute")
    @POST
    public void compute() {
        template.sendBody("direct:compute", null);
    }

    @Path("/computeAsync")
    @POST
    public void computeAsync()
            throws ExecutionException, InterruptedException, TimeoutException {
        CompletableFuture<?> future = template.requestBody("direct:computeAsync", null, CompletableFuture.class);
        future.get(5, TimeUnit.SECONDS);
    }

    @Path("/containsKey")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Boolean containsKey() {
        return template.requestBody("direct:containsKey", null, Boolean.class);
    }

    @Path("/containsValue")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Boolean containsValue(@QueryParam("value") String value) {
        return template.requestBody("direct:containsValue", value, Boolean.class);
    }

    @Path("/event/verify")
    @GET
    public void listener(
            @QueryParam("mockEndpointUri") String mockEndpointUri,
            String content) throws InterruptedException {
        MockEndpoint mockEndpoint = camelContext.getEndpoint(mockEndpointUri, MockEndpoint.class);
        mockEndpoint.expectedMessageCount(1);
        mockEndpoint.message(0).header(InfinispanConstants.EVENT_TYPE).isEqualTo("CLIENT_CACHE_ENTRY_CREATED");
        mockEndpoint.message(0).header(InfinispanConstants.CACHE_NAME).isNotNull();
        mockEndpoint.message(0).header(InfinispanConstants.KEY).isEqualTo("the-key");

        try {
            template.sendBody("direct:put", content);
            mockEndpoint.assertIsSatisfied(5000);
        } finally {
            mockEndpoint.reset();
        }
    }

    @Path("/get")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String get(@QueryParam("key") String key) {
        Map<String, Object> headers = Map.of(InfinispanConstants.KEY, Objects.requireNonNullElse(key, "the-key"));
        return template.requestBodyAndHeaders("direct:get", null, headers, String.class);
    }

    @Path("/getOrDefault")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getOrDefault() {
        return template.requestBody("direct:getOrDefault", null, String.class);
    }

    @Path("/put")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String put(String content) {
        return template.requestBody("direct:put", content, String.class);
    }

    @Path("/putAsync")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    public void putAsync(String content)
            throws ExecutionException, InterruptedException, TimeoutException {
        CompletableFuture<?> future = template.requestBody("direct:putAsync", content, CompletableFuture.class);
        future.get(5, TimeUnit.SECONDS);
    }

    @Path("/putAll")
    @POST
    public void putAll() {
        Map<String, String> body = CollectionHelper.mapOf("key-1", "value-1", "key-2", "value-2");
        template.sendBody("direct:putAll", body);
    }

    @Path("/putAllAsync")
    @POST
    public void putAllAsync()
            throws ExecutionException, InterruptedException, TimeoutException {
        Map<String, String> body = CollectionHelper.mapOf("key-1", "value-1", "key-2", "value-2");
        CompletableFuture<?> future = template.requestBody("direct:putAllAsync", body, CompletableFuture.class);
        future.get(5, TimeUnit.SECONDS);
    }

    @Path("/putIdempotent")
    @GET
    public void putIdempotent() throws InterruptedException {
        MockEndpoint mockEndpoint = camelContext.getEndpoint("mock:idempotentResult", MockEndpoint.class);
        mockEndpoint.expectedMessageCount(1);

        String messageId = UUID.randomUUID().toString();
        try {
            IntStream.of(1, 10).forEach(value -> {
                Map<String, Object> headers = Map.of("MessageId", messageId);
                template.sendBodyAndHeaders("direct:idempotent", "Message " + value, headers);
            });

            mockEndpoint.assertIsSatisfied(5000);
        } finally {
            mockEndpoint.reset();
        }
    }

    @Path("/putIfAbsent")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String putIfAbsent(@QueryParam("component") String component, String content) {
        return template.requestBody("direct:putIfAbsent", content, String.class);
    }

    @Path("/putIfAbsentAsync")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    public void putIfAbsentAsync(String content)
            throws ExecutionException, InterruptedException, TimeoutException {
        CompletableFuture<?> future = template.requestBody("direct:putIfAbsentAsync", content, CompletableFuture.class);
        future.get(5, TimeUnit.SECONDS);
    }

    @Path("/query")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @SuppressWarnings("unchecked")
    public Response query() {
        Map<String, Object> putOperationHeaders = Map.of(
                InfinispanConstants.OPERATION, InfinispanOperation.PUT,
                InfinispanConstants.KEY, "person",
                InfinispanConstants.VALUE, new Person("Test", "Person"));

        template.sendBodyAndHeaders("infinispan:" + CACHE_NAME, null, putOperationHeaders);

        String query = "FROM person.Person WHERE firstName = 'Test'";
        InfinispanQueryBuilder builder = InfinispanQueryBuilder.create(query);

        Map<String, Object> headers = Map.of(InfinispanConstants.QUERY_BUILDER, builder);

        List<String> result = template.requestBodyAndHeaders("direct:query", null, headers, List.class);
        if (result.isEmpty()) {
            return Response.status(404).build();
        }
        return Response.ok().entity(result.get(0)).build();
    }

    @Path("/remove")
    @DELETE
    public void remove() {
        template.requestBody("direct:remove", null, String.class);
    }

    @Path("/removeAsync")
    @DELETE
    public void removeAsync()
            throws ExecutionException, InterruptedException, TimeoutException {
        CompletableFuture<?> future = template.requestBody("direct:removeAsync", null, CompletableFuture.class);
        future.get(5, TimeUnit.SECONDS);
    }

    @Path("/replace")
    @PATCH
    @Consumes(MediaType.TEXT_PLAIN)
    public void replace(String content) {
        template.sendBody("direct:replace", content);
    }

    @Path("/replaceAsync")
    @PATCH
    @Consumes(MediaType.TEXT_PLAIN)
    public void replaceAsync(String content)
            throws ExecutionException, InterruptedException, TimeoutException {
        CompletableFuture<?> future = template.requestBody("direct:replaceAsync", content, CompletableFuture.class);
        future.get(5, TimeUnit.SECONDS);
    }

    @Path("/size")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Integer size() {
        return template.requestBody("direct:size", null, Integer.class);
    }

    @Path("/stats")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Integer stats() {
        ServerStatistics statistics = template.requestBody("direct:stats", null, ServerStatistics.class);
        return statistics.getIntStatistic(ServerStatistics.APPROXIMATE_ENTRIES);
    }

    @POST
    @Path("consumer/{routeId}/{enabled}")
    public void manageRoute(
            @PathParam("routeId") String routeId,
            @PathParam("enabled") boolean enabled) throws Exception {
        if (enabled) {
            camelContext.getRouteController().startRoute(routeId);
        } else {
            camelContext.getRouteController().stopRoute(routeId);
        }
    }
}
