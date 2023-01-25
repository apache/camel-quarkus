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

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
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

    @SuppressWarnings("unchecked")
    @POST
    @Path("get")
    public Map<String, Object> getAll(Set<String> oids) {
        Map<String, Object> headers = new HashMap<>();
        headers.put(HazelcastConstants.OPERATION, HazelcastOperation.GET_ALL);
        headers.put(HazelcastConstants.OBJECT_ID, oids);
        return producerTemplate.requestBodyAndHeaders(endpointUri, null, headers, Map.class);
    }

    @GET
    @Path("updated")
    public List<String> getUpdatedValues() {
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
    public List<String> getEvictedValues() {
        return getValues(mockEvictedEndpoint);
    }

    @SuppressWarnings("unchecked")
    @POST
    @Path("get/query")
    public Collection<String> query(String sqlQuery) {
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
