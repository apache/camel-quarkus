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
package org.apache.camel.quarkus.dsl.java.joor;

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
import org.apache.camel.dsl.java.joor.JavaRoutesBuilderLoader;
import org.apache.camel.quarkus.main.CamelMain;
import org.apache.camel.spi.RoutesBuilderLoader;

@Path("/java-joor-dsl")
@ApplicationScoped
public class JavaJoorDslResource {
    @Inject
    CamelMain main;

    @Inject
    ProducerTemplate producerTemplate;

    @Path("/main/javaRoutesBuilderLoader")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String javaRoutesBuilder() {
        final ExtendedCamelContext camelContext = main.getCamelContext().getCamelContextExtension();
        return camelContext.getBootstrapFactoryFinder(RoutesBuilderLoader.FACTORY_PATH)
                .findClass(JavaRoutesBuilderLoader.EXTENSION).get().getName();
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

    @POST
    @Path("/hello")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String hello(String message) {
        return producerTemplate.requestBody("direct:joorHello", message, String.class);
    }

    @POST
    @Path("/hi")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String hi(String name) {
        return producerTemplate.requestBody("direct:joorHi", name, String.class);
    }

    @POST
    @Path("/echo")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String echo(String msg) {
        return producerTemplate.requestBody("direct:joorEcho", msg, String.class);
    }

}
