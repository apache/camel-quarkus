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

import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.quarkus.runtime.StartupEvent;
import org.apache.camel.component.hazelcast.HazelcastConstants;
import org.apache.camel.component.hazelcast.HazelcastOperation;

import static org.apache.camel.quarkus.component.hazelcast.it.HazelcastRoutes.MOCK_LIST_ADDED;
import static org.apache.camel.quarkus.component.hazelcast.it.HazelcastRoutes.MOCK_LIST_DELETED;

@Path("/hazelcast/list")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class HazelcastListResource extends AbstractHazelcastCollectionResource {

    @Override
    public void init(@Observes StartupEvent startupEvent) {
        endpointUri = "hazelcast-list:foo-list";
        mockAddedEndpoint = MOCK_LIST_ADDED;
        mockDeletedEndpoint = MOCK_LIST_DELETED;
    }

    @GET
    @Path("{index}")
    public String getByIndex(@PathParam("index") Integer index) {
        Map<String, Object> headers = new HashMap<>();
        headers.put(HazelcastConstants.OPERATION, HazelcastOperation.GET);
        headers.put(HazelcastConstants.OBJECT_POS, index);
        return producerTemplate.requestBodyAndHeaders(endpointUri, null, headers,
                String.class);
    }

    @DELETE
    @Path("index")
    public Response delete(Integer index) {
        Map<String, Object> headers = new HashMap<>();
        headers.put(HazelcastConstants.OPERATION, HazelcastOperation.REMOVE_VALUE);
        headers.put(HazelcastConstants.OBJECT_POS, index);
        producerTemplate.sendBodyAndHeaders(endpointUri, null, headers);
        return Response.accepted().build();
    }

}
