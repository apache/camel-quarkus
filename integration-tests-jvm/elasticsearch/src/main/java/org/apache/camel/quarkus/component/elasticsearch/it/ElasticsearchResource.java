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
package org.apache.camel.quarkus.component.elasticsearch.it;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import co.elastic.clients.elasticsearch._types.query_dsl.MatchPhraseQuery;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.elasticsearch.core.MsearchRequest;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import co.elastic.clients.elasticsearch.core.bulk.IndexOperation;
import co.elastic.clients.elasticsearch.core.msearch.MultiSearchResponseItem;
import co.elastic.clients.elasticsearch.core.msearch.MultisearchBody;
import co.elastic.clients.elasticsearch.core.msearch.MultisearchHeader;
import co.elastic.clients.elasticsearch.core.msearch.RequestItem;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import co.elastic.clients.elasticsearch.indices.DeleteIndexRequest;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.camel.FluentProducerTemplate;
import org.apache.camel.component.es.ElasticsearchConstants;

@Path("/elasticsearch")
@ApplicationScoped
public class ElasticsearchResource {

    private static final String HEADER_COMPONENT = "component";

    @Inject
    FluentProducerTemplate fluentProducerTemplate;

    @Path("/get")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response getData(
            @QueryParam("component") String component,
            @QueryParam("indexName") String indexName,
            @QueryParam("indexId") String indexId,
            @QueryParam("indexKey") String indexKey) {

        GetResponse<?> response = fluentProducerTemplate.to("direct:get")
                .withBody(indexId)
                .withHeader(ElasticsearchConstants.PARAM_INDEX_NAME, indexName)
                .withHeader(HEADER_COMPONENT, component)
                .request(GetResponse.class);

        if (response.source() == null || !(response.source() instanceof ObjectNode)) {
            return Response.status(404).build();
        }

        return Response.ok().entity(((ObjectNode) response.source()).get(indexKey).asText()).build();
    }

    @Path("/index")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public Response indexData(
            @QueryParam("component") String component,
            @QueryParam("indexName") String indexName,
            @QueryParam("indexKey") String indexKey,
            String indexValue) throws Exception {

        String indexId = fluentProducerTemplate.to("direct:index")
                .withBody(createIndexedData(indexKey, indexValue))
                .withHeader(ElasticsearchConstants.PARAM_INDEX_NAME, indexName)
                .withHeader(HEADER_COMPONENT, component)
                .request(String.class);

        return Response
                .created(new URI("https://camel.apache.org/"))
                .entity(indexId)
                .build();
    }

    @Path("/update")
    @PATCH
    @Produces(MediaType.TEXT_PLAIN)
    public Response updateData(
            @QueryParam("component") String component,
            @QueryParam("indexName") String indexName,
            @QueryParam("indexId") String indexId,
            @QueryParam("indexKey") String indexKey,
            String indexValue) {

        fluentProducerTemplate.to("direct:update")
                .withBody(Map.of("doc", createIndexedData(indexKey, indexValue)))
                .withHeader(ElasticsearchConstants.PARAM_INDEX_NAME, indexName)
                .withHeader(ElasticsearchConstants.PARAM_INDEX_ID, indexId)
                .withHeader(HEADER_COMPONENT, component)
                .request();

        return Response.ok().build();
    }

    @Path("/delete")
    @DELETE
    @Produces(MediaType.TEXT_PLAIN)
    public Response deleteData(
            @QueryParam("component") String component,
            @QueryParam("indexName") String indexName,
            @QueryParam("indexId") String indexId) {

        fluentProducerTemplate.to("direct:delete")
                .withBody(indexId)
                .withHeader(ElasticsearchConstants.PARAM_INDEX_NAME, indexName)
                .withHeader(HEADER_COMPONENT, component)
                .request();

        return Response.noContent().build();
    }

    @Path("/delete/index")
    @DELETE
    @Produces(MediaType.TEXT_PLAIN)
    public Response deleteIndexData(
            @QueryParam("component") String component,
            @QueryParam("indexName") String indexName) {

        DeleteIndexRequest.Builder request = new DeleteIndexRequest.Builder().index(indexName);

        Boolean result = fluentProducerTemplate.to("direct:deleteIndex")
                .withBody(request)
                .withHeader(HEADER_COMPONENT, component)
                .request(Boolean.class);

        return Response.ok(result).build();
    }

    @Path("/ping")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response ping(@QueryParam("component") String component) {
        Boolean result = fluentProducerTemplate.to("direct:ping")
                .withHeader(HEADER_COMPONENT, component)
                .request(Boolean.class);

        return Response.ok(result).build();
    }

    @Path("/exists")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response exists(
            @QueryParam("component") String component,
            @QueryParam("indexName") String indexName) {

        Boolean result = fluentProducerTemplate.to("direct:exists")
                .withHeader(ElasticsearchConstants.PARAM_INDEX_NAME, indexName)
                .withHeader(HEADER_COMPONENT, component)
                .request(Boolean.class);

        return Response.ok(result).build();
    }

    @Path("/bulk")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public Response bulk(
            @QueryParam("component") String component,
            @QueryParam("indexName") String indexName) {

        BulkRequest.Builder request = new BulkRequest.Builder();
        request.operations(new BulkOperation.Builder()
                .index(new IndexOperation.Builder<>().index(indexName).document(Map.of("camel", "quarkus")).build()).build());

        BulkResponseItem[] result = fluentProducerTemplate.to("direct:bulk")
                .withBody(request)
                .withHeader(HEADER_COMPONENT, component)
                .request(BulkResponseItem[].class);

        return Response.ok(result[0].id()).build();
    }

    @Path("/search")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response searchByMap(
            @QueryParam("component") String component,
            @QueryParam("indexKey") String indexKey,
            String searchString) {

        Map<String, Object> actualQuery = new HashMap<>();
        actualQuery.put(indexKey, searchString);

        Map<String, Object> match = new HashMap<>();
        match.put("match", actualQuery);

        Map<String, Object> query = new HashMap<>();
        query.put("query", match);

        HitsMetadata<?> result = fluentProducerTemplate.to("direct:search")
                .withBody(query)
                .withHeader(HEADER_COMPONENT, component)
                .request(HitsMetadata.class);

        if (result.hits().size() > 0) {
            Object source = result.hits().get(0).source();
            if (source instanceof ObjectNode) {
                return Response.ok(((ObjectNode) source).get(indexKey).asText()).build();
            }
        }
        // return OK as it is called in loop
        return Response.ok().build();
    }

    @Path("/search/json")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response searchByJSON(
            @QueryParam("component") String component,
            @QueryParam("indexKey") String indexKey,
            String searchString) {

        String query = "{\"query\":{\"match\":{\"%s\":\"%s\"}}}";

        HitsMetadata<?> result = fluentProducerTemplate.to("direct:search")
                .withBody(String.format(query, indexKey, searchString))
                .withHeader(HEADER_COMPONENT, component)
                .request(HitsMetadata.class);

        if (result.hits().size() > 0) {
            Object source = result.hits().get(0).source();
            if (source instanceof ObjectNode) {
                return Response.ok(((ObjectNode) source).get(indexKey).asText()).build();
            }
        }
        // return OK as it is called in loop
        return Response.ok().build();
    }

    @Path("/search/multi")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response searchMulti(
            @QueryParam("component") String component,
            @QueryParam("indexName") String indexName,
            @QueryParam("indexKey") String indexKey,
            String searchStrings) {

        final String[] searchTerms = searchStrings.split(",");
        MsearchRequest.Builder builder = new MsearchRequest.Builder().index(indexName);
        for (String searchTerm : searchTerms) {
            builder.searches(new RequestItem.Builder().header(new MultisearchHeader.Builder().build())
                    .body(new MultisearchBody.Builder().query(q -> q
                            .matchPhrase(new MatchPhraseQuery.Builder().field(indexKey).query(searchTerm).build()))
                            .build())
                    .build());
        }

        MultiSearchResponseItem[] result = fluentProducerTemplate.to("direct:multiSearch")
                .withBody(builder)
                .withHeader(HEADER_COMPONENT, component)
                .request(MultiSearchResponseItem[].class);

        if (result.length > 0) {
            int totalHits = 0;
            for (MultiSearchResponseItem item : result) {
                totalHits += item.result().hits().total().value();
            }
            return Response.ok(totalHits).build();
        }
        return Response.ok().build();
    }

    private Map<String, String> createIndexedData(String indexKey, String indexValue) {
        Map<String, String> map = new HashMap<>();
        map.put(indexKey, indexValue);
        return map;
    }
}
