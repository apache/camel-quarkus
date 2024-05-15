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
package org.apache.camel.quarkus.component.microprofile.it.health;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import org.apache.camel.CamelContext;
import org.apache.camel.health.HealthCheckRegistry;

@Path("/microprofile-health")
@ApplicationScoped
public class MicroProfileHealthResource {

    @Inject
    CamelContext camelContext;

    @Path("/route/{routeId}/stop")
    @POST
    public void stopCamelRoute(@PathParam("routeId") String routeId) throws Exception {
        camelContext.getRouteController().stopRoute(routeId);
    }

    @Path("/route/{routeId}/start")
    @POST
    public void startCamelRoute(@PathParam("routeId") String routeId) throws Exception {
        camelContext.getRouteController().startRoute(routeId);
    }

    @Path("/{healthCheckId}")
    @POST
    public void healthCheckEnabled(@PathParam("healthCheckId") String healthCheckId,
            @QueryParam("healthCheckEnabled") boolean isHealthCheckEnabled) {
        HealthCheckRegistry registry = camelContext.getCamelContextExtension().getContextPlugin(HealthCheckRegistry.class);
        registry.getCheck(healthCheckId).ifPresent(healthCheck -> {
            healthCheck.setEnabled(isHealthCheckEnabled);
            if (isHealthCheckEnabled) {
                registry.register(healthCheck);
            } else {
                registry.unregister(healthCheck);
            }
        });
    }
}
