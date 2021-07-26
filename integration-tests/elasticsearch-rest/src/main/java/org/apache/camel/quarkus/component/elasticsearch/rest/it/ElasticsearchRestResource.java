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
package org.apache.camel.quarkus.component.elasticsearch.rest.it;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PATCH;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.camel.FluentProducerTemplate;
import org.apache.camel.component.elasticsearch.ElasticsearchConstants;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.MultiSearchRequest;
import org.elasticsearch.action.search.MultiSearchResponse.Item;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;

@Path("/elasticsearch-rest")
@ApplicationScoped
public class ElasticsearchRestResource {

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

        GetResponse response = fluentProducerTemplate.to("direct:get")
                .withBody(indexId)
                .withHeader(ElasticsearchConstants.PARAM_INDEX_NAME, indexName)
                .withHeader(HEADER_COMPONENT, component)
                .request(GetResponse.class);

        if (response.getSource() == null) {
            return Response.status(404).build();
        }

        return Response.ok().entity(response.getSource().get(indexKey)).build();
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
                .withBody(createIndexedData(indexKey, indexValue))
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

        DeleteIndexRequest request = new DeleteIndexRequest(indexName);

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

        BulkRequest request = new BulkRequest();
        request.add(new IndexRequest(indexName).source("camel", "quarkus"));

        BulkItemResponse[] result = fluentProducerTemplate.to("direct:bulk")
                .withBody(request)
                .withHeader(HEADER_COMPONENT, component)
                .request(BulkItemResponse[].class);

        return Response.ok(result[0].getResponse().getId()).build();
    }

    @Path("/bulk/index")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public Response bulkIndex(
            @QueryParam("component") String component,
            @QueryParam("indexName") String indexName) {

        List<Map<String, String>> documents = new ArrayList<>();
        documents.add(createIndexedData("foo", "bar"));
        documents.add(createIndexedData("cheese", "wine"));

        List<BulkItemResponse> result = fluentProducerTemplate.to("direct:bulkIndex")
                .withBody(documents)
                .withHeader(ElasticsearchConstants.PARAM_INDEX_NAME, indexName)
                .withHeader(HEADER_COMPONENT, component)
                .request(List.class);

        String ids = result.stream().map(bulkItem -> bulkItem.getResponse().getId()).collect(Collectors.joining(","));

        return Response.ok(ids).build();
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

        SearchHits result = fluentProducerTemplate.to("direct:search")
                .withBody(query)
                .withHeader(HEADER_COMPONENT, component)
                .request(SearchHits.class);

        if (result.getHits().length > 0) {
            Map<String, Object> source = result.getAt(0).getSourceAsMap();
            return Response.ok(source.get(indexKey)).build();
        }
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

        SearchHits result = fluentProducerTemplate.to("direct:search")
                .withBody(String.format(query, indexKey, searchString))
                .withHeader(HEADER_COMPONENT, component)
                .request(SearchHits.class);

        if (result.getHits().length > 0) {
            Map<String, Object> source = result.getAt(0).getSourceAsMap();
            return Response.ok(source.get(indexKey)).build();
        }
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

        String[] searchTerms = searchStrings.split(",");

        MultiSearchRequest request = new MultiSearchRequest();
        SearchRequest searchA = new SearchRequest(indexName);
        SearchSourceBuilder builderA = new SearchSourceBuilder();
        builderA.query(QueryBuilders.matchPhraseQuery(indexKey, searchTerms[0]));
        searchA.source(builderA);
        request.add(searchA);

        SearchRequest searchB = new SearchRequest(indexName);
        SearchSourceBuilder builderB = new SearchSourceBuilder();
        builderB.query(QueryBuilders.matchPhraseQuery(indexKey, searchTerms[1]));
        searchB.source(builderB);
        request.add(searchB);

        Item[] result = fluentProducerTemplate.to("direct:multiSearch")
                .withBody(request)
                .withHeader(HEADER_COMPONENT, component)
                .request(Item[].class);

        if (result.length > 0) {
            int totalHits = 0;
            for (Item item : result) {
                totalHits += item.getResponse().getHits().getHits().length;
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
