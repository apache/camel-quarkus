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

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.apache.camel.ProducerTemplate;
import org.jboss.logging.Logger;

@Path("/controlbus")
@ApplicationScoped
public class ControlbusResource {

    private static final Logger LOG = Logger.getLogger(ControlbusResource.class);

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    RestartRoutePolicy restartRoutePolicy;

    @Path("/status")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String status() throws Exception {
        return this.process("status");
    }

    @Path("/start")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String start() throws Exception {
        return this.process("startRoute");
    }

    @Path("/stop")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String stop() throws Exception {
        return this.process("stopRoute");
    }

    @Path("/suspend")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String suspend() throws Exception {
        return this.process("suspendRoute");
    }

    @Path("/resume")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String resume() throws Exception {
        return this.process("resumeRoute");
    }

    @Path("/fail")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String fail() throws Exception {
        return this.process("failRoute");
    }

    @Path("/restart")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public RouteStats restart() throws Exception {
        restartRoutePolicy.reset();
        this.process("restartRoute");
        return new RouteStats(restartRoutePolicy.getStart(), restartRoutePolicy.getStop());
    }

    private String process(String endpointName) throws Exception {
        final String message = producerTemplate.requestBody("direct:" + endpointName, "", String.class);
        LOG.infof("Received from controlbus: %s", message);
        return message;
    }

}
