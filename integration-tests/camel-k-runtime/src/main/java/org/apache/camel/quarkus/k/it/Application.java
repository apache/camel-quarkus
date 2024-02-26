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
package org.apache.camel.quarkus.k.it;

import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.apache.camel.CamelContext;
import org.apache.camel.Route;
import org.apache.camel.model.Model;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.model.rest.RestDefinition;

@Path("/camel-k")
@ApplicationScoped
public class Application {

    @Inject
    CamelContext camelContext;

    @GET
    @Path("/inspect")
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject inspect() {
        return Json.createObjectBuilder()
                .add("model-reifier-factory", Json.createValue(
                        camelContext.getCamelContextExtension().getContextPlugin(Model.class)
                                .getModelReifierFactory().getClass().getName()))
                .add("routes", Json.createArrayBuilder(
                        camelContext.getRoutes().stream()
                                .map(Route::getId)
                                .collect(Collectors.toList())))
                .add("route-definitions", Json.createArrayBuilder(
                        camelContext.getCamelContextExtension().getContextPlugin(Model.class).getRouteDefinitions().stream()
                                .map(RouteDefinition::getId)
                                .collect(Collectors.toList())))
                .add("rest-definitions", Json.createArrayBuilder(
                        camelContext.getCamelContextExtension().getContextPlugin(Model.class).getRestDefinitions().stream()
                                .map(RestDefinition::getId)
                                .collect(Collectors.toList())))
                .build();
    }

    @GET
    @Path("/property/{name}")
    @Produces(MediaType.TEXT_PLAIN)
    public String property(@PathParam("name") String name) {
        return camelContext.getPropertiesComponent().resolveProperty(name).orElse("");
    }

    @GET
    @Path("/properties")
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject properties() {
        return Json.createObjectBuilder(
                camelContext.getPropertiesComponent().loadPropertiesAsMap())
                .build();
    }

    @GET
    @Path("/registry/beans/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    public String bean(@PathParam("name") String name) throws Exception {
        Object bean = camelContext.getRegistry().lookupByName(name);
        if (bean == null) {
            throw new IllegalArgumentException("Bean with name: " + name + " not found");
        }

        try (Jsonb jsonb = JsonbBuilder.create()) {
            return jsonb.toJson(bean);
        }
    }
}
