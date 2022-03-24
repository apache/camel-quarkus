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
package org.apache.camel.quarkus.component.infinispan;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PATCH;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.infinispan.InfinispanConstants;
import org.apache.camel.component.infinispan.InfinispanQueryBuilder;
import org.apache.camel.component.infinispan.remote.InfinispanRemoteComponent;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.quarkus.component.infinispan.model.Person;
import org.apache.camel.util.CollectionHelper;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.ServerStatistics;

import static org.apache.camel.quarkus.component.infinispan.InfinispanRoutes.CORRELATOR_HEADER;

@Path("/infinispan")
@ApplicationScoped
public class InfinispanResources {
    public static final String CACHE_NAME_CAMEL = "camel";
    public static final String CACHE_NAME_QUARKUS = "quarkus";

    @Inject
    RemoteCacheManager cacheManager;

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
                .add("hosts", component.getConfiguration().getHosts())
                .add("cache-manager", Objects.toString(component.getConfiguration().getCacheContainer(), "none"))
                .build();
    }

    @Path("/aggregate")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    public void aggregate(@QueryParam("component") String component, String content) {
        String uri = component.equals("infinispan") ? "direct:camelAggregation" : "direct:quarkusAggregation";
        Map<String, Object> headers = getCommonHeaders(component);
        headers.put(CORRELATOR_HEADER, CORRELATOR_HEADER);
        template.sendBodyAndHeaders(uri, content, headers);
    }

    @Path("/clear")
    @DELETE
    public void clear(@QueryParam("component") String component) {
        Map<String, Object> headers = getCommonHeaders(component);
        template.sendBodyAndHeaders("direct:clear", null, headers);
    }

    @Path("/clearAsync")
    @DELETE
    public void clearAsync(@QueryParam("component") String component)
            throws ExecutionException, InterruptedException, TimeoutException {
        Map<String, Object> headers = getCommonHeaders(component);
        CompletableFuture<?> future = template.requestBodyAndHeaders("direct:clearAsync", null, headers,
                CompletableFuture.class);
        future.get(5, TimeUnit.SECONDS);
    }

    @Path("/compute")
    @POST
    public void compute(@QueryParam("component") String component) {
        Map<String, Object> headers = getCommonHeaders(component);
        template.sendBodyAndHeaders("direct:compute", null, headers);
    }

    @Path("/computeAsync")
    @POST
    public void computeAsync(@QueryParam("component") String component)
            throws ExecutionException, InterruptedException, TimeoutException {
        Map<String, Object> headers = getCommonHeaders(component);
        CompletableFuture<?> future = template.requestBodyAndHeaders("direct:computeAsync", null, headers,
                CompletableFuture.class);
        future.get(5, TimeUnit.SECONDS);
    }

    @Path("/containsKey")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Boolean containsKey(@QueryParam("component") String component) {
        Map<String, Object> headers = getCommonHeaders(component);
        return template.requestBodyAndHeaders("direct:containsKey", null, headers, Boolean.class);
    }

    @Path("/containsValue")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Boolean containsValue(@QueryParam("component") String component, @QueryParam("value") String value) {
        Map<String, Object> headers = getCommonHeaders(component);
        return template.requestBodyAndHeaders("direct:containsValue", value, headers, Boolean.class);
    }

    @Path("/get")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String get(@QueryParam("component") String component, @QueryParam("key") String key) {
        Map<String, Object> headers = getCommonHeaders(component);
        headers.put(InfinispanConstants.KEY, Objects.requireNonNullElse(key, "the-key"));
        return template.requestBodyAndHeaders("direct:get", null, headers, String.class);
    }

    @Path("/getOrDefault")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getOrDefault(@QueryParam("component") String component) {
        Map<String, Object> headers = getCommonHeaders(component);
        return template.requestBodyAndHeaders("direct:getOrDefault", null, headers, String.class);
    }

    @Path("/put")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String put(@QueryParam("component") String component, String content) {
        Map<String, Object> headers = getCommonHeaders(component);
        return template.requestBodyAndHeaders("direct:put", content, headers, String.class);
    }

    @Path("/putAsync")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    public void putAsync(@QueryParam("component") String component, String content)
            throws ExecutionException, InterruptedException, TimeoutException {
        Map<String, Object> headers = getCommonHeaders(component);
        CompletableFuture<?> future = template.requestBodyAndHeaders("direct:putAsync", content, headers,
                CompletableFuture.class);
        future.get(5, TimeUnit.SECONDS);
    }

    @Path("/putAll")
    @POST
    public void putAll(@QueryParam("component") String component) {
        Map<String, String> body = CollectionHelper.mapOf("key-1", "value-1", "key-2", "value-2");
        Map<String, Object> headers = getCommonHeaders(component);
        template.sendBodyAndHeaders("direct:putAll", body, headers);
    }

    @Path("/putAllAsync")
    @POST
    public void putAllAsync(@QueryParam("component") String component)
            throws ExecutionException, InterruptedException, TimeoutException {
        Map<String, String> body = CollectionHelper.mapOf("key-1", "value-1", "key-2", "value-2");
        Map<String, Object> headers = getCommonHeaders(component);
        CompletableFuture<?> future = template.requestBodyAndHeaders("direct:putAllAsync", body, headers,
                CompletableFuture.class);
        future.get(5, TimeUnit.SECONDS);
    }

    @Path("/putIdempotent")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    public void putIdempotent(
            @QueryParam("component") String component,
            @QueryParam("messageId") String messageId,
            String content) {
        String uri = component.equals("infinispan") ? "direct:camelIdempotent" : "direct:quarkusIdempotent";
        Map<String, Object> headers = getCommonHeaders(component);
        headers.put("MessageId", messageId);
        template.sendBodyAndHeaders(uri, content, headers);
    }

    @Path("/putIfAbsent")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String putIfAbsent(@QueryParam("component") String component, String content) {
        Map<String, Object> headers = getCommonHeaders(component);
        return template.requestBodyAndHeaders("direct:putIfAbsent", content, headers, String.class);
    }

    @Path("/putIfAbsentAsync")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    public void putIfAbsentAsync(@QueryParam("component") String component, String content)
            throws ExecutionException, InterruptedException, TimeoutException {
        Map<String, Object> headers = getCommonHeaders(component);
        CompletableFuture<?> future = template.requestBodyAndHeaders("direct:putIfAbsentAsync", content, headers,
                CompletableFuture.class);
        future.get(5, TimeUnit.SECONDS);
    }

    @Path("/query")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @SuppressWarnings("unchecked")
    public Response query(@QueryParam("component") String component) {
        Map<String, Object> headers = getCommonHeaders(component);
        String cacheName = (String) headers.get("cacheName");

        cacheManager.getCache(cacheName).put("person", new Person("Test", "Person"));

        String query = "FROM person.Person WHERE firstName = 'Test'";
        InfinispanQueryBuilder builder = InfinispanQueryBuilder.create(query);

        headers.put(InfinispanConstants.QUERY_BUILDER, builder);

        List<String> result = template.requestBodyAndHeaders("direct:query", null, headers, List.class);
        if (result.isEmpty()) {
            return Response.status(404).build();
        }
        return Response.ok().entity(result.get(0)).build();
    }

    @Path("/remove")
    @DELETE
    public void remove(@QueryParam("component") String component) {
        Map<String, Object> headers = getCommonHeaders(component);
        template.requestBodyAndHeaders("direct:remove", null, headers, String.class);
    }

    @Path("/removeAsync")
    @DELETE
    public void removeAsync(@QueryParam("component") String component)
            throws ExecutionException, InterruptedException, TimeoutException {
        Map<String, Object> headers = getCommonHeaders(component);
        CompletableFuture<?> future = template.requestBodyAndHeaders("direct:removeAsync", null, headers,
                CompletableFuture.class);
        future.get(5, TimeUnit.SECONDS);
    }

    @Path("/replace")
    @PATCH
    @Consumes(MediaType.TEXT_PLAIN)
    public void replace(@QueryParam("component") String component, String content) {
        Map<String, Object> headers = getCommonHeaders(component);
        template.sendBodyAndHeaders("direct:replace", content, headers);
    }

    @Path("/replaceAsync")
    @PATCH
    @Consumes(MediaType.TEXT_PLAIN)
    public void replaceAsync(@QueryParam("component") String component, String content)
            throws ExecutionException, InterruptedException, TimeoutException {
        Map<String, Object> headers = getCommonHeaders(component);
        CompletableFuture<?> future = template.requestBodyAndHeaders("direct:replaceAsync", content, headers,
                CompletableFuture.class);
        future.get(5, TimeUnit.SECONDS);
    }

    @Path("/size")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Integer size(@QueryParam("component") String component) {
        Map<String, Object> headers = getCommonHeaders(component);
        return template.requestBodyAndHeaders("direct:size", null, headers, Integer.class);
    }

    @Path("/stats")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Integer stats(@QueryParam("component") String component) {
        Map<String, Object> headers = getCommonHeaders(component);
        ServerStatistics statistics = template.requestBodyAndHeaders("direct:stats", null, headers, ServerStatistics.class);
        return statistics.getIntStatistic(ServerStatistics.CURRENT_NR_OF_ENTRIES);
    }

    @Path("/mock/aggregation/results")
    @GET
    public void assertMockEndpointAggregationResults(@QueryParam("uri") String uri) throws InterruptedException {
        MockEndpoint mockEndpoint = camelContext.getEndpoint(uri, MockEndpoint.class);
        mockEndpoint.expectedMessageCount(2);
        mockEndpoint.expectedBodiesReceived(1 + 3 + 4 + 5, 6 + 7 + 20 + 21);

        try {
            mockEndpoint.assertIsSatisfied(5000);
        } finally {
            mockEndpoint.reset();
        }
    }

    @Path("/mock/event/results")
    @GET
    public void assertMockEndpointEventResults(@QueryParam("uri") String uri) throws InterruptedException {
        MockEndpoint mockEndpoint = camelContext.getEndpoint(uri, MockEndpoint.class);
        mockEndpoint.expectedMessageCount(1);
        mockEndpoint.message(0).header(InfinispanConstants.EVENT_TYPE).isEqualTo("CLIENT_CACHE_ENTRY_CREATED");
        mockEndpoint.message(0).header(InfinispanConstants.CACHE_NAME).isNotNull();
        mockEndpoint.message(0).header(InfinispanConstants.KEY).isEqualTo("test-key");

        try {
            mockEndpoint.assertIsSatisfied(5000);
        } finally {
            mockEndpoint.reset();
        }
    }

    @Path("/mock/idempotent/results")
    @GET
    public void assertMockEndpointIdempotentResults(@QueryParam("uri") String uri) throws InterruptedException {
        MockEndpoint mockEndpoint = camelContext.getEndpoint(uri, MockEndpoint.class);
        mockEndpoint.expectedMessageCount(1);
        try {
            mockEndpoint.assertIsSatisfied(5000);
        } finally {
            mockEndpoint.reset();
        }
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

    private Map<String, Object> getCommonHeaders(String componentName) {
        Map<String, Object> headers = new HashMap<>();
        headers.put("component", componentName);

        if (componentName.equals("infinispan")) {
            headers.put("cacheName", CACHE_NAME_CAMEL);
        } else {
            headers.put("cacheName", CACHE_NAME_QUARKUS);
        }

        return headers;
    }
}
