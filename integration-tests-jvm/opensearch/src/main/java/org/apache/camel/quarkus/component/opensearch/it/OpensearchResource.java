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
package org.apache.camel.quarkus.component.opensearch.it;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import org.apache.camel.FluentProducerTemplate;
import org.apache.camel.component.opensearch.OpensearchConstants;
import org.jboss.logging.Logger;
import org.opensearch.client.json.JsonData;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch.core.BulkRequest;
import org.opensearch.client.opensearch.core.DeleteRequest;
import org.opensearch.client.opensearch.core.GetRequest;
import org.opensearch.client.opensearch.core.GetResponse;
import org.opensearch.client.opensearch.core.MsearchRequest;
import org.opensearch.client.opensearch.core.bulk.BulkResponseItem;
import org.opensearch.client.opensearch.core.bulk.IndexOperation;
import org.opensearch.client.opensearch.core.mget.MultiGetResponseItem;
import org.opensearch.client.opensearch.core.msearch.MultiSearchResponseItem;
import org.opensearch.client.opensearch.core.msearch.MultisearchBody;
import org.opensearch.client.opensearch.core.msearch.MultisearchHeader;
import org.opensearch.client.opensearch.core.msearch.RequestItem;

@Path("/opensearch")
@ApplicationScoped
public class OpensearchResource {

    private static final Logger LOG = Logger.getLogger(OpensearchResource.class);

    @Inject
    FluentProducerTemplate fluentProducerTemplate;

    @POST
    @Path("/index/{index}/{id}")
    public Response indexDoc(String body,
            @PathParam("index") String index,
            @PathParam("id") String id) {
        Map<String, Object> headers = Map.of(
                OpensearchConstants.PARAM_INDEX_ID, id,
                OpensearchConstants.PARAM_INDEX_NAME, index);
        String response = fluentProducerTemplate.to("direct:indexDoc")
                .withBody(body)
                .withHeaders(headers)
                .request(String.class);

        if (response.isBlank() || response.isEmpty()) {
            return Response.status(404).build();
        }

        return Response.ok().entity(response).build();

    }

    @POST
    @Path("/bulk/{index}")
    public Response bulkIndex(List<Map<String, Object>> docs,
            @PathParam("index") String index) {

        BulkRequest.Builder br = new BulkRequest.Builder();
        for (Map<String, Object> doc : docs) {
            String id = (String) doc.get("id");
            br.operations(op -> op
                    .index(IndexOperation.of(idx -> idx
                            .index(index)
                            .id(id)
                            .document(JsonData.of(doc)))));
        }

        BulkResponseItem[] bulkResponse = fluentProducerTemplate.to("direct:bulkIndex")
                .withBody(br)
                .request(BulkResponseItem[].class);

        List<String> insertedIds = Arrays.asList(bulkResponse)
                .stream().map(BulkResponseItem::id)
                .collect(Collectors.toList());

        return Response.ok(insertedIds).build();

    }

    @GET
    @Path("/get/{index}/{id}")
    public Response getDoc(@PathParam("index") String index,
            @PathParam("id") String id) {

        GetRequest.Builder builder = new GetRequest.Builder();
        builder.index(index).id(id);

        GetResponse<?> response = fluentProducerTemplate.to("direct:getDoc")
                .withBody(builder)
                .request(GetResponse.class);

        if (response.source() == null || !response.found()) {
            return Response.status(404).build();
        }

        return Response.ok().entity(response.id()).build();
    }

    @POST
    @Path("/multiget/{index}")
    public Response multiGet(@PathParam("index") String index,
            List<String> body) {

        MultiGetResponseItem<?> responseItem[] = fluentProducerTemplate.to("direct:multiget")
                .withBody(body)
                .withHeader(OpensearchConstants.PARAM_INDEX_NAME, index)
                .request(MultiGetResponseItem[].class);

        int totalFound = Arrays.asList(responseItem).stream().map(s -> s.result().found())
                .collect(Collectors.toList()).size();
        return Response.ok(totalFound).build();

    }

    @GET
    @Path("/multisearch")
    public Response multiSearch(@QueryParam("users") String user,
            @QueryParam("orders") String order) {

        MsearchRequest.Builder builder = null;

        if (user != null & order != null) {
            builder = new MsearchRequest.Builder().searches(
                    new RequestItem.Builder()
                            .header(new MultisearchHeader.Builder().index("users").build())
                            .body(new MultisearchBody.Builder()
                                    .query(b -> b.match(m -> m.field("name")
                                            .query(FieldValue.of(user))))
                                    .build())
                            .build(),
                    new RequestItem.Builder()
                            .header(new MultisearchHeader.Builder().index("orders").build())
                            .body(new MultisearchBody.Builder()
                                    .query(b -> b.match(m -> m.field("item")
                                            .query(FieldValue.of(order))))
                                    .build())
                            .build());

        }

        MultiSearchResponseItem<?>[] response = fluentProducerTemplate.to("direct:multiSearch")
                .withBody(builder)
                .request(MultiSearchResponseItem[].class);

        if (response.length > 0) {
            int totalFound = 0;
            for (MultiSearchResponseItem<?> item : response) {
                if (!item.isFailure() && item.isResult() && item.result() != null) {
                    totalFound++;
                }
            }
            return Response.ok(totalFound).build();
        }
        return Response.ok().build();

    }

    @DELETE
    @Path("/delete/{index}/{id}")
    public Response deleteDoc(@PathParam("index") String index,
            @PathParam("id") String id) {

        DeleteRequest.Builder builder = new DeleteRequest.Builder();
        builder.id(id);
        builder.index(index);
        String response = fluentProducerTemplate.to("direct:deleteDoc")
                .withBody(builder)
                .request(String.class);

        return Response.ok(response).build();
    }

    @POST
    @Path("/search/{index}")
    public Response search(String body, @PathParam("index") String index) {
        String response = fluentProducerTemplate.to("direct:search")
                .withBody(body)
                .request(String.class);
        return Response.ok(response).build();
    }

    @POST
    @Path("/scroll")
    public Response scroll(String body) {
        String response = fluentProducerTemplate.to("direct:search")
                .withBody(body)
                .request(String.class);
        return Response.ok(response).build();
    }

}
