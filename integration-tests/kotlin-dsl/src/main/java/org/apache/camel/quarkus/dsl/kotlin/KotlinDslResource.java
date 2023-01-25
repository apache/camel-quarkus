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
package org.apache.camel.quarkus.dsl.kotlin;

import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.apache.camel.ExtendedCamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.Route;
import org.apache.camel.component.direct.DirectEndpoint;
import org.apache.camel.dsl.kotlin.KotlinConstantsKt;
import org.apache.camel.quarkus.main.CamelMain;
import org.apache.camel.spi.RoutesBuilderLoader;

@Path("/kotlin-dsl")
@ApplicationScoped
public class KotlinDslResource {
    @Inject
    CamelMain main;

    @Inject
    ProducerTemplate producerTemplate;

    @Path("/main/kotlinRoutesBuilderLoader")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String kotlinRoutesBuilder() {
        final ExtendedCamelContext camelContext = main.getCamelContext().adapt(ExtendedCamelContext.class);
        return camelContext.getBootstrapFactoryFinder(RoutesBuilderLoader.FACTORY_PATH)
                .findClass(KotlinConstantsKt.EXTENSION).get().getName();
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
        int successful = 0;
        Set<String> excluded = Set.of("my-kotlin-route", "routes-with-rest-dsl-get", "routes-with-rest-dsl-post");
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
        return producerTemplate.requestBody("direct:kotlinHello", message, String.class);
    }
}
