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

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.camel.ExtendedCamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.TemplatedRouteBuilder;

@Path("/test")
@ApplicationScoped
public class CoreMainXmlIoResource {
    @Inject
    CamelMain main;

    @Inject
    ProducerTemplate template;

    @Path("/main/describe")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject describeMain() {
        final ExtendedCamelContext camelContext = main.getCamelContext().adapt(ExtendedCamelContext.class);

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

        JsonObjectBuilder collector = Json.createObjectBuilder();
        collector.add("type", main.getRoutesCollector().getClass().getName());
        if (main.getRoutesCollector() instanceof CamelMainRoutesCollector) {
            CamelMainRoutesCollector crc = (CamelMainRoutesCollector) main.getRoutesCollector();
            collector.add("type-registry", crc.getRegistryRoutesLoader().getClass().getName());
            collector.add("type-xml", camelContext.getXMLRoutesDefinitionLoader().getClass().getName());
        }

        return Json.createObjectBuilder()
                .add("xml-loader", camelContext.getXMLRoutesDefinitionLoader().getClass().getName())
                .add("xml-model-dumper", camelContext.getModelToXMLDumper().getClass().getName())
                .add("xml-model-factory", camelContext.getModelJAXBContextFactory().getClass().getName())
                .add("routes-collector", collector)
                .add("listeners", listeners)
                .add("routeBuilders", routeBuilders)
                .add("routes", routes)
                .add("autoConfigurationLogSummary", main.getMainConfigurationProperties().isAutoConfigurationLogSummary())
                .build();
    }

    @Path("/xml-io/namespace-aware")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    @Consumes(MediaType.APPLICATION_XML)
    public String namespaceAware(String body) {
        return template.requestBody("direct:namespace-aware", body, String.class);
    }

}
