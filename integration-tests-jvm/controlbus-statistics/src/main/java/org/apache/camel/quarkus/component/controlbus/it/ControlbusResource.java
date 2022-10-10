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
package org.apache.camel.quarkus.component.controlbus.it;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.camel.ProducerTemplate;
import org.jboss.logging.Logger;

@Path("/controlbus")
@ApplicationScoped
public class ControlbusResource {

    private static final Logger LOG = Logger.getLogger(ControlbusResource.class);

    @Inject
    ProducerTemplate producerTemplate;

    @Path("/status")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String status() throws Exception {
        final String message = producerTemplate.requestBody("direct:status", "", String.class);
        LOG.infof("Received from controlbus: %s", message);
        return message;
    }

    @Path("/stats")
    @GET
    @Produces(MediaType.APPLICATION_XML)
    public String stats() throws Exception {
        final String message = producerTemplate.requestBody("direct:statsRoute", "", String.class);
        LOG.infof("Received from controlbus: %s", message);
        return message;
    }

}
