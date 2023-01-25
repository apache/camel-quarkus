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
package org.apache.camel.quarkus.component.nats.it;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.apache.camel.Body;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.jboss.logging.Logger;

@Path("/nats/")
@ApplicationScoped
public class NatsResource {

    private static final Logger LOG = Logger.getLogger(NatsResource.class);

    private final ConcurrentHashMap<String, ConcurrentLinkedQueue<String>> msgStore = new ConcurrentHashMap<>();

    @Inject
    ProducerTemplate template;

    @Path("/send")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    public void send(@HeaderParam("sendToEndpointUri") String sendToEndpointUri, String message) {
        LOG.debugf("Invoking send with (%s, %s)", sendToEndpointUri, message);
        template.sendBody(sendToEndpointUri, message);
    }

    void storeMessage(Exchange e, @Body String message) {
        LOG.debugf("Invoking storeMessage with (%s, %s)", e, message);
        msgStore.computeIfAbsent(e.getFromRouteId(), s -> new ConcurrentLinkedQueue<>()).add(message);
    }

    @Path("/messages/{route-id}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Collection<String> getRouteMessages(@PathParam("route-id") String routeId) {
        return msgStore.get(routeId);
    }

}
