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
package org.apache.camel.quarkus.dsl.groovy;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.camel.ExtendedCamelContext;
import org.apache.camel.dsl.groovy.GroovyRoutesBuilderLoader;
import org.apache.camel.quarkus.main.CamelMain;
import org.apache.camel.spi.RoutesBuilderLoader;

@Path("/test")
@ApplicationScoped
public class GroovyDslResource {
    @Inject
    CamelMain main;

    @Path("/main/describe")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject describeMain() {
        final ExtendedCamelContext camelContext = main.getCamelContext().adapt(ExtendedCamelContext.class);

        JsonArrayBuilder routeBuilders = Json.createArrayBuilder();
        main.configure().getRoutesBuilders().forEach(builder -> routeBuilders.add(builder.getClass().getName()));

        JsonArrayBuilder routes = Json.createArrayBuilder();
        main.getCamelContext().getRoutes().forEach(route -> routes.add(route.getId()));

        return Json.createObjectBuilder()
                .add("routes-builder-loader",
                        camelContext.getBootstrapFactoryFinder(RoutesBuilderLoader.FACTORY_PATH)
                                .findClass(GroovyRoutesBuilderLoader.EXTENSION).get().getName())
                .add("routeBuilders", routeBuilders)
                .add("routes", routes)
                .build();
    }
}
