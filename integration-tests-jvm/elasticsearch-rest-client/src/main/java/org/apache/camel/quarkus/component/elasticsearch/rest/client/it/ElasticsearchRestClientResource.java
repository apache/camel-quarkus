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
package org.apache.camel.quarkus.component.elasticsearch.rest.client.it;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.camel.CamelExecutionException;
import org.apache.camel.FluentProducerTemplate;
import org.apache.camel.component.elasticsearch.rest.client.ElasticSearchRestClientConstant;
import org.elasticsearch.client.ResponseException;

@Path("/elasticsearch-rest-client")
@ApplicationScoped
public class ElasticsearchRestClientResource {
    private static final String HEADER_COMPONENT = "component";

    @Inject
    FluentProducerTemplate fluentProducerTemplate;

    @Path("/get")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getData(@QueryParam("indexName") String indexName, @QueryParam("indexId") String indexId) {
        try {
            Document document = fluentProducerTemplate.to("direct:get")
                    .withHeader(ElasticSearchRestClientConstant.INDEX_NAME, indexName)
                    .withHeader(ElasticSearchRestClientConstant.ID, indexId)
                    .request(Document.class);
            return Response.ok().entity(document).build();
        } catch (CamelExecutionException e) {
            if (e.getCause() instanceof ResponseException responseException) {
                return Response.status(responseException.getResponse().getStatusLine().getStatusCode()).build();
            }
        }
        return Response.serverError().build();
    }

    @Path("/index/create")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public Response createIndex(
            @QueryParam("indexName") String indexName,
            String settings) throws Exception {

        Map<String, Object> headers = new HashMap<>();
        headers.put(ElasticSearchRestClientConstant.INDEX_NAME, indexName);
        if (settings != null) {
            headers.put(ElasticSearchRestClientConstant.INDEX_SETTINGS, settings);
        }

        boolean success = fluentProducerTemplate.to("direct:createIndex")
                .withHeaders(headers)
                .request(boolean.class);

        return Response.created(new URI("https://camel.apache.org/"))
                .entity(success)
                .build();
    }

    @Path("/index")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response indexData(
            @QueryParam("indexName") String indexName,
            @QueryParam("indexId") String indexId,
            Document document) throws Exception {

        Map<String, Object> headers = new HashMap<>();
        headers.put(ElasticSearchRestClientConstant.INDEX_NAME, indexName);
        if (indexId != null) {
            headers.put(ElasticSearchRestClientConstant.ID, indexId);
        }

        String result = fluentProducerTemplate.to("direct:index")
                .withBody(document)
                .withHeaders(headers)
                .request(String.class);

        if (indexId != null) {
            return Response.ok(result).build();
        } else {
            return Response.created(new URI("https://camel.apache.org/"))
                    .entity(result)
                    .build();
        }
    }

    @Path("/delete")
    @DELETE
    @Produces(MediaType.TEXT_PLAIN)
    public Response deleteData(@QueryParam("indexName") String indexName, @QueryParam("indexId") String indexId) {
        fluentProducerTemplate.to("direct:delete")
                .withBody(indexId)
                .withHeader(ElasticSearchRestClientConstant.INDEX_NAME, indexName)
                .withHeader(ElasticSearchRestClientConstant.ID, indexId)
                .request();

        return Response.noContent().build();
    }

    @Path("/delete/index")
    @DELETE
    @Produces(MediaType.TEXT_PLAIN)
    public Response deleteIndexData(@QueryParam("indexName") String indexName) {
        boolean result = fluentProducerTemplate.to("direct:deleteIndex")
                .withHeader(ElasticSearchRestClientConstant.INDEX_NAME, indexName)
                .request(boolean.class);
        return Response.ok(result).build();
    }

    @Path("/search")
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response search(@QueryParam("indexName") String indexName, Map<String, String> criteria) {
        String result = fluentProducerTemplate.to("direct:search")
                .withHeader(ElasticSearchRestClientConstant.INDEX_NAME, indexName)
                .withBody(criteria)
                .request(String.class);
        return Response.ok(result).build();
    }

    @Path("/search")
    @GET
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public Response searchByJSON(@QueryParam("indexName") String indexName, String query) {
        String result = fluentProducerTemplate.to("direct:search")
                .withHeader(ElasticSearchRestClientConstant.INDEX_NAME, indexName)
                .withHeader(ElasticSearchRestClientConstant.SEARCH_QUERY, query)
                .request(String.class);
        return Response.ok(result).build();
    }
}
