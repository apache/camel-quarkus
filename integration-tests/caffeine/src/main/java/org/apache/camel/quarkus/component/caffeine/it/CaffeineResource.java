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
package org.apache.camel.quarkus.component.caffeine.it;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.camel.CamelContext;
import org.apache.camel.FluentProducerTemplate;
import org.apache.camel.component.caffeine.CaffeineConstants;

@Path("/caffeine")
@ApplicationScoped
public class CaffeineResource {
    @Inject
    CamelContext context;
    @Inject
    FluentProducerTemplate template;

    @Path("/component/{componentName}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response loadComponent(@PathParam("componentName") String componentName) {
        return context.getComponent(componentName) != null
                ? Response.ok().build()
                : Response.status(500, componentName + " could not be loaded from the Camel context").build();
    }

    @Path("/request/{componentName}/{cacheName}/{key}")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String request(
            @PathParam("componentName") String componentName,
            @PathParam("cacheName") String cacheName,
            @PathParam("key") String key,
            String value) {

        FluentProducerTemplate t = template.toF("%s://%s", componentName, cacheName);
        t.withHeader(CaffeineConstants.ACTION, CaffeineConstants.ACTION_PUT);
        t.withHeader(CaffeineConstants.KEY, key);
        t.withBody(value);

        return t.request(String.class);
    }

    @Path("/request/{componentName}/{cacheName}/{key}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String request(
            @PathParam("componentName") String componentName,
            @PathParam("cacheName") String cacheName,
            @PathParam("key") String key) {

        FluentProducerTemplate t = template.toF("%s://%s", componentName, cacheName);
        t.withHeader(CaffeineConstants.ACTION, CaffeineConstants.ACTION_GET);
        t.withHeader(CaffeineConstants.KEY, key);

        return t.request(String.class);
    }
}
