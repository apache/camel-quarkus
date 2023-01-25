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

import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.apache.camel.ExtendedCamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.dsl.yaml.YamlRoutesBuilderLoader;
import org.apache.camel.spi.RoutesBuilderLoader;

@Path("/main/yaml")
@ApplicationScoped
public class CoreMainYamlResource {
    @Inject
    CamelMain main;

    @Inject
    ProducerTemplate producerTemplate;

    @Path("/describe")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @SuppressWarnings("unchecked")
    public JsonObject describeMain() {
        final ExtendedCamelContext camelContext = main.getCamelContext().adapt(ExtendedCamelContext.class);

        JsonArrayBuilder listeners = Json.createArrayBuilder();
        main.getMainListeners().forEach(listener -> listeners.add(listener.getClass().getName()));

        JsonArrayBuilder routeBuilders = Json.createArrayBuilder();
        main.configure().getRoutesBuilders().forEach(builder -> routeBuilders.add(builder.getClass().getName()));

        JsonArrayBuilder routes = Json.createArrayBuilder();
        main.getCamelContext().getRoutes().forEach(route -> routes.add(route.getId()));

        return Json.createObjectBuilder()
                .add("yaml-routes-builder-loader",
                        camelContext.getBootstrapFactoryFinder(RoutesBuilderLoader.FACTORY_PATH)
                                .findClass(YamlRoutesBuilderLoader.EXTENSION).get().getName())
                .add("routeBuilders", routeBuilders)
                .add("routes", routes)
                .add("global-options", Json.createObjectBuilder((Map) main.getCamelContext().getGlobalOptions()).build())
                .build();
    }

    @Path("/greet")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String greet(@QueryParam("forceFailure") boolean forceFailure) {
        return producerTemplate.requestBodyAndHeader("direct:start", null, "forceFailure", forceFailure, String.class);
    }

    @Path("/try/catch")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String tryCatch() {
        return producerTemplate.requestBody("direct:tryCatch", null, String.class);
    }
}
