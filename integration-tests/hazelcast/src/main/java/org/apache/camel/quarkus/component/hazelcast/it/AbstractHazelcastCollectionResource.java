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
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import com.hazelcast.collection.ItemEvent;
import io.quarkus.runtime.StartupEvent;
import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.hazelcast.HazelcastConstants;
import org.apache.camel.component.hazelcast.HazelcastOperation;
import org.apache.camel.component.mock.MockEndpoint;
import org.jboss.logging.Logger;

public abstract class AbstractHazelcastCollectionResource {

    private static final Logger LOG = Logger.getLogger(AbstractHazelcastCollectionResource.class);

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    CamelContext context;

    protected String endpointUri;
    protected String mockAddedEndpoint;
    protected String mockDeletedEndpoint;

    /**
     * init endpoints
     *
     * @param startupEvent
     */
    public abstract void init(StartupEvent startupEvent);

    @PUT
    public Response add(String value) {
        producerTemplate.sendBodyAndHeader(endpointUri, value, HazelcastConstants.OPERATION, HazelcastOperation.ADD);
        return Response.accepted().build();
    }

    @PUT
    @Path("all")
    public Response addAll(Collection values) {
        producerTemplate.sendBodyAndHeader(endpointUri, values, HazelcastConstants.OPERATION, HazelcastOperation.ADD_ALL);
        return Response.accepted().build();
    }

    @DELETE
    @Path("value")
    public Response delete(String value) {
        Map<String, Object> headers = new HashMap<>();
        headers.put(HazelcastConstants.OPERATION, HazelcastOperation.REMOVE_VALUE);
        producerTemplate.sendBodyAndHeaders(endpointUri, value, headers);
        return Response.accepted().build();
    }

    @DELETE
    @Path("all")
    public Response delete(Collection values) {
        Map<String, Object> headers = new HashMap<>();
        headers.put(HazelcastConstants.OPERATION, HazelcastOperation.REMOVE_ALL);
        producerTemplate.sendBodyAndHeaders(endpointUri, values, headers);
        return Response.accepted().build();
    }

    @POST
    @Path("retain")
    public Response retainAll(Collection values) {
        producerTemplate.sendBodyAndHeader(endpointUri, values, HazelcastConstants.OPERATION, HazelcastOperation.RETAIN_ALL);
        return Response.accepted().build();
    }

    @GET
    @Path("added")
    public List<String> getAddedValues() {
        return getValues(mockAddedEndpoint);
    }

    @GET
    @Path("deleted")
    public List<String> getDeletedValues() {
        return getValues(mockDeletedEndpoint);
    }

    private List<String> getValues(String endpointName) {
        LOG.infof("getting response from mock endpoint %s", endpointName);
        MockEndpoint mockEndpoint = context.getEndpoint(endpointName, MockEndpoint.class);
        List<String> values = mockEndpoint.getReceivedExchanges().stream().map(
                exchange -> {
                    ItemEvent itemEvent = exchange.getIn().getBody(ItemEvent.class);
                    return (String) itemEvent.getItem();
                })
                .collect(Collectors.toList());
        return values;
    }
}
