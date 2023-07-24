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
package org.apache.camel.quarkus.k.loader;

import java.util.ServiceLoader;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.apache.camel.CamelContext;
import org.apache.camel.quarkus.k.core.Runtime;
import org.apache.camel.quarkus.k.loader.support.LoaderSupport;

@Path("/test")
@ApplicationScoped
public class Application {

    @Inject
    CamelContext context;

    // k-core
    @GET
    @Path("/services")
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject getServices() {
        JsonArrayBuilder builder = Json.createArrayBuilder();

        ServiceLoader.load(Runtime.Listener.class).forEach(listener -> {
            builder.add(listener.getClass().getName());
        });

        return Json.createObjectBuilder()
                .add("services", builder)
                .build();
    }

    @POST
    @Path("/load-routes/{loaderName}/{name}")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject loadRoutes(@PathParam("loaderName") String loaderName, @PathParam("name") String name, byte[] code)
            throws Exception {
        return LoaderSupport.inspectSource(context, name + "." + loaderName, code);
    }
}
