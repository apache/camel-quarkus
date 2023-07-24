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
package org.apache.camel.quarkus.k.runtime.support;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonArray;
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
import org.apache.camel.model.ToDefinition;
import org.apache.camel.model.rest.RestDefinition;
import org.eclipse.microprofile.config.Config;

import static org.apache.camel.model.ProcessorDefinitionHelper.filterTypeInOutputs;

@Path("/runtime")
@ApplicationScoped
public class RuntimeInspector {
    @Inject
    CamelContext camelContext;
    @Inject
    Config config;

    @GET
    @Path("/inspect")
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject inspect() {
        return Json.createObjectBuilder()
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
        return config.getValue(name, String.class);
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

    @GET
    @Path("/route-outputs/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    public JsonArray routeOutputs(@PathParam("name") String name) {
        RouteDefinition def = camelContext.getCamelContextExtension().getContextPlugin(Model.class).getRouteDefinition(name);
        if (def == null) {
            throw new IllegalArgumentException("RouteDefinition with name: " + name + " not found");
        }

        Collection<ToDefinition> toDefinitions = filterTypeInOutputs(def.getOutputs(), ToDefinition.class);

        List<String> endpoints = toDefinitions.stream()
                .map(td -> td.getEndpointUri())
                .collect(Collectors.toList());

        return Json.createArrayBuilder(endpoints).build();
    }
}
