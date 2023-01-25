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

import java.util.List;
import java.util.Map;

import io.quarkus.runtime.StartupEvent;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.hazelcast.HazelcastConstants;
import org.apache.camel.component.hazelcast.HazelcastOperation;
import org.jboss.logging.Logger;

public abstract class AbstractHazelcastCollectionResource {

    private static final Logger LOG = Logger.getLogger(AbstractHazelcastCollectionResource.class);

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    CamelContext context;

    @Inject
    @Named("hazelcastResults")
    Map<String, List<String>> hazelcastResults;

    protected String endpointUri;
    protected String mockAddedEndpoint;
    protected String mockDeletedEndpoint;

    /**
     * init endpoints
     */
    public abstract void init(StartupEvent startupEvent);

    @PUT
    public Response add(String value) {
        producerTemplate.sendBodyAndHeader(endpointUri, value, HazelcastConstants.OPERATION, HazelcastOperation.ADD);
        return Response.accepted().build();
    }

    @PUT
    @Path("all")
    public Response addAll(List<String> values) {
        producerTemplate.sendBodyAndHeader(endpointUri, values, HazelcastConstants.OPERATION, HazelcastOperation.ADD_ALL);
        return Response.accepted().build();
    }

    @DELETE
    @Path("value")
    public Response delete(String value) {
        producerTemplate.sendBodyAndHeader(endpointUri, value, HazelcastConstants.OPERATION, HazelcastOperation.REMOVE_VALUE);
        return Response.accepted().build();
    }

    @DELETE
    @Path("all")
    public Response delete(List<String> values) {
        producerTemplate.sendBodyAndHeader(endpointUri, values, HazelcastConstants.OPERATION, HazelcastOperation.REMOVE_ALL);
        return Response.accepted().build();
    }

    @POST
    @Path("retain")
    public Response retainAll(List<String> values) {
        producerTemplate.sendBodyAndHeader(endpointUri, values, HazelcastConstants.OPERATION, HazelcastOperation.RETAIN_ALL);
        return Response.accepted().build();
    }

    @GET
    @Path("added")
    public List<String> getAddedValues() {
        return hazelcastResults.get(mockAddedEndpoint);
    }

    @GET
    @Path("deleted")
    public List<String> getDeletedValues() {
        return hazelcastResults.get(mockDeletedEndpoint);
    }

}
