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
package org.apache.camel.quarkus.component.microprofile.health.it;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.apache.camel.CamelContext;
import org.apache.camel.spi.Registry;

@Path("/microprofile-health")
@ApplicationScoped
public class MicroProfileHealthResource {

    @Inject
    CamelContext camelContext;

    @Path("/checks/failing/{enabled}")
    @GET
    public void toggleFailingHealthCheck(@PathParam("enabled") boolean enabled) {
        Registry registry = camelContext.getRegistry();
        FailingHealthCheck failingHealthCheck = registry.lookupByNameAndType(FailingHealthCheck.class.getSimpleName(), FailingHealthCheck.class);
        failingHealthCheck.getConfiguration().setEnabled(enabled);
    }

    @Path("/route/{routeId}/stop")
    @GET
    public void stopCamelRoute(@PathParam("routeId") String routeId) throws Exception {
        camelContext.getRouteController().stopRoute(routeId);
    }

    @Path("/route/{routeId}/start")
    @GET
    public void startCamelRoute(@PathParam("routeId") String routeId) throws Exception {
        camelContext.getRouteController().startRoute(routeId);
    }
}
