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

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.hazelcast.HazelcastConstants;
import org.apache.camel.component.hazelcast.HazelcastOperation;

@Path("/hazelcast/ringbuffer")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class HazelcastRingbufferResource {
    private static final String ENDPOINT_URI = "hazelcast-ringbuffer:foo-ringbuffer";

    @Inject
    ProducerTemplate producerTemplate;

    @PUT
    public Response add(String value) {
        producerTemplate.sendBodyAndHeader(ENDPOINT_URI, value, HazelcastConstants.OPERATION, HazelcastOperation.ADD);
        return Response.accepted().build();
    }

    @GET
    @Path("capacity")
    public Long getCapacity() {
        return producerTemplate.requestBodyAndHeader(ENDPOINT_URI, null, HazelcastConstants.OPERATION,
                HazelcastOperation.CAPACITY, Long.class);
    }

    @GET
    @Path("capacity/remaining")
    public Long getRemainingCapacity() {
        return producerTemplate.requestBodyAndHeader(ENDPOINT_URI, null, HazelcastConstants.OPERATION,
                HazelcastOperation.REMAINING_CAPACITY, Long.class);
    }

    @GET
    @Path("tail")
    public String getTail() {
        return producerTemplate.requestBodyAndHeader(ENDPOINT_URI, null, HazelcastConstants.OPERATION,
                HazelcastOperation.READ_ONCE_TAIL, String.class);
    }

    @GET
    @Path("head")
    public String getHead() {
        return producerTemplate.requestBodyAndHeader(ENDPOINT_URI, null, HazelcastConstants.OPERATION,
                HazelcastOperation.READ_ONCE_HEAD, String.class);
    }
}
