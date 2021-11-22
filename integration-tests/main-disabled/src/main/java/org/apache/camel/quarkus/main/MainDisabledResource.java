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
package org.apache.camel.quarkus.main;

import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.Route;

@Path("/main-disabled")
@ApplicationScoped
public class MainDisabledResource {

    @Inject
    CamelContext context;

    @Inject
    ProducerTemplate template;

    @Path("/inspect")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject inspect() {
        // collect the list of route classes instead of just the
        // number of routes to help debugging in case of a failing
        // tests
        List<String> routes = context.getRoutes().stream()
                .map(Route::getClass)
                .map(Class::getName)
                .collect(Collectors.toList());

        return Json.createObjectBuilder()
                .add("routes", Json.createArrayBuilder(routes))
                .build();
    }

    @Path("/invoke-main-disabled-route")
    @GET
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String invokeMainDisabledRoute(String content) {
        return template.requestBody("direct:main-disabled", content, String.class);
    }

    @Path("/invoke-main-disabled-route-cdi")
    @GET
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String invokeMainDisabledRouteCdi(String content) {
        return template.requestBody("direct:main-disabled-cdi", content, String.class);
    }
}
