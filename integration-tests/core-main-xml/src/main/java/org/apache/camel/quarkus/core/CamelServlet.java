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
package org.apache.camel.quarkus.core;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.camel.CamelContext;
import org.apache.camel.ExtendedCamelContext;
import org.apache.camel.spi.Registry;

@Path("/test")
@ApplicationScoped
public class CamelServlet {
    @Inject
    CamelMain main;
    @Inject
    Registry registry;
    @Inject
    CamelContext context;

    @Path("/main/describe")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject describeMain() {
        final ExtendedCamelContext camelContext = main.getCamelContext().adapt(ExtendedCamelContext.class);

        JsonArrayBuilder listeners = Json.createArrayBuilder();
        main.getMainListeners().forEach(listener -> listeners.add(listener.getClass().getName()));

        JsonArrayBuilder routeBuilders = Json.createArrayBuilder();
        main.getRoutesBuilders().forEach(builder -> routeBuilders.add(builder.getClass().getName()));

        JsonArrayBuilder routes = Json.createArrayBuilder();
        main.getCamelContext().getRoutes().forEach(route -> routes.add(route.getId()));

        JsonObjectBuilder collector = Json.createObjectBuilder();
        collector.add("type", main.getRoutesCollector().getClass().getName());
        if (main.getRoutesCollector() instanceof CamelRoutesCollector) {
            CamelRoutesCollector crc = (CamelRoutesCollector) main.getRoutesCollector();
            collector.add("type-registry", crc.getRegistryRoutesLoader().getClass().getName());
            collector.add("type-xml", crc.getXmlRoutesLoader().getClass().getName());
        }

        return Json.createObjectBuilder()
                .add("xml-loader", camelContext.getXMLRoutesDefinitionLoader().getClass().getName())
                .add("xml-model-dumper", camelContext.getModelToXMLDumper().getClass().getName())
                .add("routes-collector", collector)
                .add("listeners", listeners)
                .add("routeBuilders", routeBuilders)
                .add("routes", routes)
                .add("autoConfigurationLogSummary", main.getMainConfigurationProperties().isAutoConfigurationLogSummary())
                .build();
    }
}
