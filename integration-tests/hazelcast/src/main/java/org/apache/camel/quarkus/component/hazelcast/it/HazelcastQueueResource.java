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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.hazelcast.collection.IQueue;
import com.hazelcast.core.HazelcastInstance;
import io.quarkus.runtime.StartupEvent;
import org.apache.camel.component.hazelcast.HazelcastConstants;
import org.apache.camel.component.hazelcast.HazelcastOperation;
import org.apache.camel.component.mock.MockEndpoint;

import static org.apache.camel.quarkus.component.hazelcast.it.HazelcastRoutes.MOCK_QUEUE_ADDED;
import static org.apache.camel.quarkus.component.hazelcast.it.HazelcastRoutes.MOCK_QUEUE_DELETED;
import static org.apache.camel.quarkus.component.hazelcast.it.HazelcastRoutes.MOCK_QUEUE_POLL;

@Path("/hazelcast/queue")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class HazelcastQueueResource extends AbstractHazelcastCollectionResource {

    @Inject
    HazelcastInstance hazelcastInstance;

    @Override
    public void init(@Observes StartupEvent startupEvent) {
        endpointUri = "hazelcast-queue:foo-queue";
        mockAddedEndpoint = MOCK_QUEUE_ADDED;
        mockDeletedEndpoint = MOCK_QUEUE_DELETED;
    }

    @PUT
    @Path("put")
    public Response addBlocking(String value) {
        producerTemplate.sendBodyAndHeader(endpointUri, value, HazelcastConstants.OPERATION, HazelcastOperation.PUT);
        return Response.accepted().build();
    }

    @PUT
    @Path("offer")
    public Response offer(String value) {
        producerTemplate.sendBodyAndHeader(endpointUri, value, HazelcastConstants.OPERATION, HazelcastOperation.OFFER);
        return Response.accepted().build();
    }

    @DELETE
    @Path("poll")
    public String poll() {
        return producerTemplate.requestBodyAndHeader(endpointUri, null, HazelcastConstants.OPERATION, HazelcastOperation.POLL,
                String.class);
    }

    @GET
    @Path("peek")
    public String peek() {
        return producerTemplate.requestBodyAndHeader(endpointUri, null, HazelcastConstants.OPERATION, HazelcastOperation.PEEK,
                String.class);
    }

    @DELETE
    @Path("take")
    public String take() {
        return producerTemplate.requestBodyAndHeader(endpointUri, null, HazelcastConstants.OPERATION, HazelcastOperation.TAKE,
                String.class);
    }

    @GET
    @Path("remainingCapacity")
    public Integer remainingCapacity() {
        return producerTemplate.requestBodyAndHeader(endpointUri, null, HazelcastConstants.OPERATION,
                HazelcastOperation.REMAINING_CAPACITY, Integer.class);
    }

    @DELETE
    @Path("drain")
    public List<String> drainTo() {
        List<String> result = new ArrayList<>();
        Map<String, Object> headers = new HashMap<>();
        headers.put(HazelcastConstants.OPERATION, HazelcastOperation.DRAIN_TO);
        headers.put(HazelcastConstants.DRAIN_TO_COLLECTION, result);
        producerTemplate.sendBodyAndHeaders(endpointUri, "", headers);
        return result;
    }

    /**
     * add list of values to queue with poll consumer
     *
     */
    @PUT
    @Path("poll/list")
    public Response addListToPollConsumer(List<String> values) {
        IQueue<String> queue = hazelcastInstance.getQueue("foo-queue-poll");
        queue.addAll(values);
        return Response.accepted().build();
    }

    @GET
    @Path("polled")
    public List<String> getPolledValues() {
        MockEndpoint mockEndpoint = context.getEndpoint(MOCK_QUEUE_POLL, MockEndpoint.class);
        return mockEndpoint.getReceivedExchanges().stream().map(
                exchange -> exchange.getIn().getBody(String.class))
                .collect(Collectors.toList());
    }

}
