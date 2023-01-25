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
package org.apache.camel.quarkus.core.it.routeconfigurations;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.apache.camel.ProducerTemplate;

@Path("/core")
@ApplicationScoped
public class RouteConfigurationsResource {

    @Inject
    ProducerTemplate producerTemplate;

    @Path("/route-configurations/routeConfigurationWithExplicitId")
    @GET
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String routeConfigurationWithExplicitId(String content) {
        return producerTemplate.requestBody("direct:routeConfigurationWithExplicitId", content, String.class);
    }

    @Path("/route-configurations/fallbackRouteConfiguration")
    @GET
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String fallbackRouteConfiguration(String content) {
        return producerTemplate.requestBody("direct:fallbackRouteConfiguration", content, String.class);
    }

    @Path("/route-configurations/endpointRouteConfigurationWithExplicitId")
    @GET
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String endpointRouteConfigurationWithExplicitId(String content) {
        return producerTemplate.requestBody("direct:endpointRouteConfigurationWithExplicitId", content, String.class);
    }

    @Path("/route-configurations/xmlRoute")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String xmlRoute() {
        return producerTemplate.requestBody("direct:xmlRoute", null, String.class);
    }

    @Path("/route-configurations/yamlRoute")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String yamlRoute() {
        return producerTemplate.requestBody("direct:yamlRoute", null, String.class);
    }

}
