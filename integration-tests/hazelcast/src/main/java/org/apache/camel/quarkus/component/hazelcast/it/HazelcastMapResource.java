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
package org.apache.camel.quarkus.component.hazelcast.it;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.quarkus.runtime.StartupEvent;
import org.apache.camel.component.hazelcast.HazelcastConstants;
import org.apache.camel.component.hazelcast.HazelcastOperation;

import static org.apache.camel.quarkus.component.hazelcast.it.HazelcastRoutes.MOCK_MAP_ADDED;
import static org.apache.camel.quarkus.component.hazelcast.it.HazelcastRoutes.MOCK_MAP_DELETED;
import static org.apache.camel.quarkus.component.hazelcast.it.HazelcastRoutes.MOCK_MAP_EVICTED;
import static org.apache.camel.quarkus.component.hazelcast.it.HazelcastRoutes.MOCK_MAP_UPDATED;

@Path("/hazelcast/map")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class HazelcastMapResource extends AbstractHazelcastMapResource {

    @Override
    public void init(@Observes StartupEvent startupEvent) {
        endpointUri = "hazelcast-map:foo-map";
        mockAddedEndpoint = MOCK_MAP_ADDED;
        mockDeletedEndpoint = MOCK_MAP_DELETED;
        mockEvictedEndpoint = MOCK_MAP_EVICTED;
        mockUpdatedEndpoint = MOCK_MAP_UPDATED;
    }

    @POST
    @Path("get")
    public Map getAll(Set oids) {
        Map<String, Object> headers = new HashMap<>();
        headers.put(HazelcastConstants.OPERATION, HazelcastOperation.GET_ALL);
        headers.put(HazelcastConstants.OBJECT_ID, oids);
        return producerTemplate.requestBodyAndHeaders(endpointUri, null, headers, Map.class);
    }

    @GET
    @Path("updated")
    public List getUpdatedValues() {
        return getValues(mockUpdatedEndpoint);
    }

    @GET
    @Path("evict/{id}")
    public Response evict(@PathParam("id") String id) {
        Map<String, Object> headers = new HashMap<>();
        headers.put(HazelcastConstants.OPERATION, HazelcastOperation.EVICT);
        headers.put(HazelcastConstants.OBJECT_ID, id);
        producerTemplate.sendBodyAndHeaders(endpointUri, null, headers);
        return Response.accepted().build();
    }

    @GET
    @Path("evict")
    public Response evict() {
        Map<String, Object> headers = new HashMap<>();
        headers.put(HazelcastConstants.OPERATION, HazelcastOperation.EVICT_ALL);
        producerTemplate.sendBodyAndHeaders(endpointUri, "", headers);
        return Response.accepted().build();
    }

    @GET
    @Path("evicted")
    public List getEvictedValues() {
        return getValues(mockEvictedEndpoint);
    }

    @POST
    @Path("get/query")
    public Collection query(String sqlQuery) {
        Map<String, Object> headers = new HashMap<>();
        headers.put(HazelcastConstants.OPERATION, HazelcastOperation.QUERY);
        headers.put(HazelcastConstants.QUERY, sqlQuery);
        return producerTemplate.requestBodyAndHeaders(endpointUri, null, headers, Collection.class);
    }

    @POST
    @Path("update/{id}")
    public Response update(@PathParam("id") String id, String value) {
        Map<String, Object> headers = new HashMap<>();
        headers.put(HazelcastConstants.OPERATION, HazelcastOperation.UPDATE);
        headers.put(HazelcastConstants.OBJECT_ID, id);
        producerTemplate.sendBodyAndHeaders(endpointUri, value, headers);
        return Response.accepted().build();
    }
}
