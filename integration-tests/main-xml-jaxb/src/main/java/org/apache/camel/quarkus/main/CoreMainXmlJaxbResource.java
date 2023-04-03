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

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.apache.camel.ExtendedCamelContext;
import org.apache.camel.builder.TemplatedRouteBuilder;
import org.apache.camel.dsl.xml.io.XmlRoutesBuilderLoader;
import org.apache.camel.spi.RoutesBuilderLoader;
import org.apache.camel.support.PluginHelper;

@Path("/test")
@ApplicationScoped
public class CoreMainXmlJaxbResource {
    @Inject
    CamelMain main;

    @Path("/main/describe")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject describeMain() {
        final ExtendedCamelContext camelContext = main.getCamelContext().getCamelContextExtension();

        JsonArrayBuilder listeners = Json.createArrayBuilder();
        main.getMainListeners().forEach(listener -> listeners.add(listener.getClass().getName()));

        JsonArrayBuilder routeBuilders = Json.createArrayBuilder();
        main.configure().getRoutesBuilders().forEach(builder -> routeBuilders.add(builder.getClass().getName()));

        TemplatedRouteBuilder.builder(main.getCamelContext(), "myTemplate")
                .parameter("name", "Camel Quarkus")
                .parameter("greeting", "Hello")
                .routeId("templated-route")
                .add();

        JsonArrayBuilder routes = Json.createArrayBuilder();
        main.getCamelContext().getRoutes().forEach(route -> routes.add(route.getId()));

        return Json.createObjectBuilder()
                .add("xml-routes-builder-loader",
                        camelContext.getBootstrapFactoryFinder(RoutesBuilderLoader.FACTORY_PATH)
                                .findClass(XmlRoutesBuilderLoader.EXTENSION).get().getName())
                .add("xml-model-dumper", camelContext.getModelToXMLDumper().getClass().getName())
                .add("xml-model-factory", PluginHelper.getModelJAXBContextFactory(camelContext).getClass().getName())
                .add("routeBuilders", routeBuilders)
                .add("routes", routes)
                .build();
    }
}
