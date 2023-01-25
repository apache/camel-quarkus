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
package org.apache.camel.quarkus.component.direct.it;

import java.io.IOException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.TemplatedRouteBuilder;
import org.apache.camel.catalog.RuntimeCamelCatalog;
import org.apache.camel.quarkus.core.CamelRuntimeCatalog;

@ApplicationScoped
@Path("/direct")
public class DirectResource {

    @Inject
    CamelContext context;

    @Inject
    ProducerTemplate producerTemplate;

    @Path("/routes/template/{id}/{greeting}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String routeTemplate(@PathParam("id") String id, @PathParam("greeting") String greeting) {
        String uuid = context.getUuidGenerator().generateUuid();
        TemplatedRouteBuilder.builder(context, id)
                .routeId(uuid)
                .parameter("uuid", uuid)
                .parameter("greeting", greeting)
                .add();

        return context.createFluentProducerTemplate().toF("direct:%s", uuid).request(String.class);
    }

    @Path("/catalog/{type}/{name}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response catalog(@PathParam("type") String type, @PathParam("name") String name) throws IOException {
        final CamelRuntimeCatalog catalog = (CamelRuntimeCatalog) context.getExtension(RuntimeCamelCatalog.class);

        try {
            final String schema;
            switch (type) {
            case "component":
                schema = catalog.componentJSonSchema(name);
                break;
            case "language":
                schema = catalog.languageJSonSchema(name);
                break;
            case "dataformat":
                schema = catalog.dataFormatJSonSchema(name);
                break;
            case "model":
                schema = catalog.modelJSonSchema(name);
                break;
            default:
                throw new IllegalArgumentException("Unknown type " + type);
            }
            return Response.ok(schema).build();
        } catch (Exception e) {
            return Response.serverError().entity(e.getClass().getSimpleName() + ": " + e.getMessage()).build();
        }
    }

    @Path("/route/{route}")
    @POST
    public void route(@PathParam("route") String route, String message) {
        producerTemplate.sendBody("direct:" + route, message);
    }

}
