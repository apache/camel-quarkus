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
package org.apache.camel.quarkus.component.redis.it;

import java.util.concurrent.ConcurrentLinkedQueue;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.apache.camel.ProducerTemplate;
import org.jboss.logging.Logger;

@Path("/redis")
@ApplicationScoped
public class RedisResource {

    private static final Logger LOG = Logger.getLogger(RedisResource.class);

    private ConcurrentLinkedQueue<String> aggregates = new ConcurrentLinkedQueue<>();

    @Inject
    ProducerTemplate producerTemplate;

    @Path("/aggregate/{message}/{correlationKey}")
    @POST
    public void aggregate(@PathParam("message") String message, @PathParam("correlationKey") int correlationKey) {
        LOG.debugf("Calling aggregate(%s, %d)", message, correlationKey);

        producerTemplate.sendBodyAndHeader("direct:start", message, "myId", correlationKey);
    }

    @Path("/get-aggregates")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public ConcurrentLinkedQueue<String> getAggregates() {
        LOG.debug("Calling getAggregates()");
        return aggregates;
    }

    void storeMessage(String message) {
        aggregates.add(message);
    }

}
