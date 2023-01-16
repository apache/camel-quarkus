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
package org.apache.camel.quarkus.js;

import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.camel.ExtendedCamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.Route;
import org.apache.camel.component.direct.DirectEndpoint;
import org.apache.camel.dsl.js.JavaScriptRoutesBuilderLoader;
import org.apache.camel.quarkus.main.CamelMain;
import org.apache.camel.spi.RoutesBuilderLoader;

@Path("/js-dsl")
@ApplicationScoped
public class JavaScriptDslResource {
    @Inject
    CamelMain main;

    @Inject
    ProducerTemplate producerTemplate;

    @Path("/main/jsRoutesBuilderLoader")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String jsRoutesBuilder() {
        final ExtendedCamelContext camelContext = main.getCamelContext().adapt(ExtendedCamelContext.class);
        return camelContext.getBootstrapFactoryFinder(RoutesBuilderLoader.FACTORY_PATH)
                .findClass(JavaScriptRoutesBuilderLoader.EXTENSION).get().getName();
    }

    @Path("/main/routeBuilders")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String routeBuilders() {
        return main.configure().getRoutesBuilders().stream()
                .map(rb -> rb.getClass().getSimpleName())
                .sorted()
                .collect(Collectors.joining(","));
    }

    @Path("/main/routes")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String routes() {
        return main.getCamelContext().getRoutes().stream()
                .map(Route::getId)
                .sorted()
                .collect(Collectors.joining(","));
    }

    @GET
    @Path("/main/successful/routes")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public int successfulRoutes() {
        Set<String> excluded = Set.of("my-js-route", "routes-with-rest-dsl-hello", "routes-with-rest-configuration-goodbye");
        int successful = 0;
        for (Route route : main.getCamelContext().getRoutes()) {
            String name = route.getRouteId();
            if (route.getEndpoint() instanceof DirectEndpoint && !excluded.contains(name)
                    && producerTemplate.requestBody(route.getEndpoint(), "", Boolean.class) == Boolean.TRUE) {
                successful++;
            }
        }
        return successful;
    }

    @POST
    @Path("/hello")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String hello(String message) {
        return producerTemplate.requestBody("direct:jsHello", message, String.class);
    }
}
