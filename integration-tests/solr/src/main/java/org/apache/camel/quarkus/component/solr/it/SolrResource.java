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
package org.apache.camel.quarkus.component.solr.it;

import java.net.URI;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.camel.FluentProducerTemplate;
import org.apache.camel.component.solr.SolrConstants;
import org.apache.camel.component.solr.SolrOperation;
import org.apache.camel.quarkus.component.solr.it.model.Item;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.SolrPingResponse;

@Path("/solr")
public class SolrResource {
    @Inject
    FluentProducerTemplate fluentProducerTemplate;

    @Consumes(MediaType.APPLICATION_JSON)
    @POST
    public Response insert(Item item) throws Exception {
        fluentProducerTemplate.to("direct:start")
                .withBody(item)
                .withHeader(SolrConstants.PARAM_OPERATION, SolrConstants.OPERATION_INSERT)
                .send();
        return Response.created(new URI("https://camel.apache.org")).build();
    }

    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    @GET
    public Response query(String queryString) {
        QueryResponse response = fluentProducerTemplate.to("direct:start")
                .withHeader(SolrConstants.PARAM_OPERATION, SolrConstants.OPERATION_QUERY)
                .withHeader(SolrConstants.PARAM_QUERY_STRING, queryString)
                .request(QueryResponse.class);
        return Response.ok(response.getResults()).build();
    }

    @Consumes(MediaType.TEXT_PLAIN)
    @DELETE
    public void delete(String id) throws Exception {
        fluentProducerTemplate.to("direct:start")
                .withBody(id)
                .withHeader(SolrConstants.PARAM_OPERATION, SolrConstants.OPERATION_DELETE_BY_ID)
                .send();
    }

    @Path("/ping")
    @Produces(MediaType.TEXT_PLAIN)
    @GET
    public Response ping() {
        SolrPingResponse response = fluentProducerTemplate.to("direct:start")
                .withHeader(SolrConstants.PARAM_OPERATION, SolrOperation.PING)
                .request(SolrPingResponse.class);
        return Response.ok(response.getStatus()).build();
    }
}
