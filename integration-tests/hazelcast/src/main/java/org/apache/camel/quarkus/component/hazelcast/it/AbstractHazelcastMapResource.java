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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import io.quarkus.runtime.StartupEvent;
import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.hazelcast.HazelcastConstants;
import org.apache.camel.component.hazelcast.HazelcastOperation;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.quarkus.component.hazelcast.it.model.HazelcastMapRequest;
import org.jboss.logging.Logger;

public abstract class AbstractHazelcastMapResource {
    private static final Logger LOG = Logger.getLogger(AbstractHazelcastMapResource.class);

    protected String endpointUri;
    protected String mockAddedEndpoint;
    protected String mockDeletedEndpoint;
    protected String mockEvictedEndpoint;
    protected String mockUpdatedEndpoint;

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    CamelContext context;

    /**
     * init endpoints
     *
     * @param startupEvent
     */
    public abstract void init(StartupEvent startupEvent);

    @POST
    @Path("add")
    public Response add(HazelcastMapRequest request) {
        Map<String, Object> headers = new HashMap<>();
        headers.put(HazelcastConstants.OPERATION, HazelcastOperation.PUT);
        headers.put(HazelcastConstants.OBJECT_ID, request.getId());
        if (request.getTtl() != null && request.getTtlUnit() != null) {
            headers.put(HazelcastConstants.TTL_UNIT, request.getTtlUnit());
            headers.put(HazelcastConstants.TTL_VALUE, request.getTtl());
        }
        producerTemplate.sendBodyAndHeaders(endpointUri, request.getValue(), headers);
        return Response.accepted().build();
    }

    @GET
    @Path("get/{id}")
    public Object get(@PathParam("id") String id) {
        Map<String, Object> headers = new HashMap<>();
        headers.put(HazelcastConstants.OPERATION, HazelcastOperation.GET);
        headers.put(HazelcastConstants.OBJECT_ID, id);
        return producerTemplate.requestBodyAndHeaders(endpointUri, null, headers);
    }

    @DELETE
    @Path("{id}")
    public Response delete(@PathParam("id") String id) {
        Map<String, Object> headers = new HashMap<>();
        headers.put(HazelcastConstants.OPERATION, HazelcastOperation.DELETE);
        headers.put(HazelcastConstants.OBJECT_ID, id);
        producerTemplate.sendBodyAndHeaders(endpointUri, null, headers);
        return Response.accepted().build();
    }

    @GET
    @Path("clear")
    public Response clear() {
        Map<String, Object> headers = new HashMap<>();
        headers.put(HazelcastConstants.OPERATION, HazelcastOperation.CLEAR);
        producerTemplate.sendBodyAndHeaders(endpointUri, "", headers);
        return Response.accepted().build();
    }

    @GET
    @Path("key/{id}")
    public Boolean containsKey(@PathParam("id") String id) {
        Map<String, Object> headers = new HashMap<>();
        headers.put(HazelcastConstants.OPERATION, HazelcastOperation.CONTAINS_KEY);
        headers.put(HazelcastConstants.OBJECT_ID, id);
        return producerTemplate.requestBodyAndHeaders(endpointUri, null, headers, Boolean.class);
    }

    @GET
    @Path("value/{value}")
    public Boolean containsValue(@PathParam("value") String value) {
        return producerTemplate.requestBodyAndHeader(endpointUri, value, HazelcastConstants.OPERATION,
                HazelcastOperation.CONTAINS_VALUE, Boolean.class);
    }

    @GET
    @Path("added")
    public List getAddedValues() {
        return getValues(mockAddedEndpoint);
    }

    @GET
    @Path("deleted")
    public List getDeletedValues() {
        return getValues(mockDeletedEndpoint);
    }

    protected List getValues(String endpointName) {
        LOG.infof("getting response from mock endpoint %s", endpointName);
        MockEndpoint mockEndpoint = context.getEndpoint(endpointName, MockEndpoint.class);
        List<String> values = mockEndpoint.getReceivedExchanges().stream().map(
                exchange -> exchange.getIn().getHeader(HazelcastConstants.OBJECT_ID, String.class))
                .collect(Collectors.toList());
        return values;
    }
}
