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
import java.util.HashMap;
import java.util.Map;

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

import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.elasticsearch.ElasticsearchConstants;
import org.elasticsearch.action.get.GetResponse;

@Path("/elasticsearch-rest")
@ApplicationScoped
public class ElasticsearchRestResource {

    @Inject
    ProducerTemplate producerTemplate;

    @Path("/get")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response getData(@QueryParam("component") String component, @QueryParam("indexId") String indexId) {
        GetResponse response = producerTemplate.requestBodyAndHeader("direct:get", indexId, "component", component,
                GetResponse.class);
        if (response.getSource() == null) {
            return Response.status(404).build();
        }
        return Response.ok().entity(response.getSource().get("test-key")).build();
    }

    @Path("/index")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public Response indexData(@QueryParam("component") String component, String indexValue) throws Exception {
        Map<String, String> data = createIndexedData(indexValue);
        String indexId = producerTemplate.requestBodyAndHeader("direct:index", data, "component", component, String.class);
        return Response
                .created(new URI("https://camel.apache.org/"))
                .entity(indexId)
                .build();
    }

    @Path("/update")
    @PATCH
    @Produces(MediaType.TEXT_PLAIN)
    public Response updateData(@QueryParam("component") String component, @QueryParam("indexId") String indexId,
            String indexValue) {
        Map<String, String> data = createIndexedData(indexValue);
        Map<String, Object> headers = new HashMap<>();
        headers.put(ElasticsearchConstants.PARAM_INDEX_ID, indexId);
        headers.put("component", component);

        producerTemplate.requestBodyAndHeaders("direct:update", data, headers);
        return Response.ok().build();
    }

    @Path("/delete")
    @DELETE
    @Produces(MediaType.TEXT_PLAIN)
    public Response deleteData(@QueryParam("component") String component, @QueryParam("indexId") String indexId) {
        producerTemplate.requestBodyAndHeader("direct:delete", indexId, "component", component);
        return Response.noContent().build();
    }

    private Map<String, String> createIndexedData(String indexValue) {
        Map<String, String> map = new HashMap<>();
        map.put("test-key", indexValue);
        return map;
    }

}
