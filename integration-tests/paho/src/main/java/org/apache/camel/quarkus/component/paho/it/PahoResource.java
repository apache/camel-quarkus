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
package org.apache.camel.quarkus.component.paho.it;

import org.apache.camel.ConsumerTemplate;
import org.apache.camel.ProducerTemplate;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/paho")
@ApplicationScoped
public class PahoResource {

    private static final Logger LOG = Logger.getLogger(PahoResource.class);

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    ConsumerTemplate consumerTemplate;

    @Path("/queue")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String get() {
        final String message = consumerTemplate.receiveBody("paho:test/queue", String.class);
        return message;
    }

    @Path("/queue")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public Response publish(String message) {
        LOG.infof("Sending to paho: %s", message);
        producerTemplate.requestBody("paho:test/queue?retained=true", message, String.class);
        LOG.infof("Sent to paho: %s", message);

        return Response.ok().build();
    }
}
