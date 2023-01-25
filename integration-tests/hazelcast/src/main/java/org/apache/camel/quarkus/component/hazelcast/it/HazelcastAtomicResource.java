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
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.hazelcast.HazelcastConstants;
import org.apache.camel.component.hazelcast.HazelcastOperation;
import org.jboss.logging.Logger;

@Path("/hazelcast/atomic")
@Produces(MediaType.TEXT_PLAIN)
@Consumes(MediaType.TEXT_PLAIN)
@ApplicationScoped
public class HazelcastAtomicResource {
    private static final Logger LOG = Logger.getLogger(HazelcastAtomicResource.class);

    @Inject
    ProducerTemplate producerTemplate;

    @PUT
    @Path("{value}")
    public Response set(@PathParam("value") Long value) {
        LOG.infof("setting atomic long value %s", value);
        producerTemplate.sendBodyAndHeader(
                "hazelcast-atomicvalue:foo-atomic",
                value, HazelcastConstants.OPERATION, HazelcastOperation.SET_VALUE);
        return Response.accepted().build();
    }

    @GET()
    public Long get() {
        LOG.infof("getting one atomic value");
        return producerTemplate.requestBodyAndHeader(
                "hazelcast-atomicvalue:foo-atomic",
                null, HazelcastConstants.OPERATION, HazelcastOperation.GET, Long.class);
    }

    @GET
    @Path("increment")
    public Long incrementAndGet() {
        LOG.infof("increment and get new value");
        return producerTemplate.requestBodyAndHeader(
                "hazelcast-atomicvalue:foo-atomic",
                null, HazelcastConstants.OPERATION, HazelcastOperation.INCREMENT, Long.class);
    }

    @GET
    @Path("decrement")
    public Long decrementAndGet() {
        LOG.infof("decrement and get new value");
        return producerTemplate.requestBodyAndHeader(
                "hazelcast-atomicvalue:foo-atomic",
                null, HazelcastConstants.OPERATION, HazelcastOperation.DECREMENT, Long.class);
    }

    @DELETE
    public Response destroy() {
        LOG.infof("destroy atomic value");
        producerTemplate.sendBodyAndHeader("hazelcast-atomicvalue:foo-atomic",
                null, HazelcastConstants.OPERATION, HazelcastOperation.DESTROY);
        return Response.accepted().build();
    }
}
