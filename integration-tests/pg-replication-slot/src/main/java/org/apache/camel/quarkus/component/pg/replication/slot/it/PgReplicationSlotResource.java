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
package org.apache.camel.quarkus.component.pg.replication.slot.it;

import java.util.concurrent.ConcurrentLinkedQueue;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.jboss.logging.Logger;

@Path("/pg-replication-slot")
@ApplicationScoped
public class PgReplicationSlotResource {

    private static final Logger LOG = Logger.getLogger(PgReplicationSlotResource.class);

    private final ConcurrentLinkedQueue<String> replicationEvents = new ConcurrentLinkedQueue<>();

    void logReplicationEvent(String event) {
        LOG.debugf("Calling logReplicationEvent(\"%s\")", event);
        replicationEvents.add(event);
    }

    @Path("/get-events")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public ConcurrentLinkedQueue<String> getReplicationEvents() {
        LOG.debug("Calling getReplicationEvents");
        return replicationEvents;
    }
}
