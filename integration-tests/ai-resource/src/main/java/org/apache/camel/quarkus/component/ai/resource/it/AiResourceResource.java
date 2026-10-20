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
package org.apache.camel.quarkus.component.ai.resource.it;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.component.ai.resource.AiResourceExecutor;
import org.apache.camel.component.ai.resource.AiResourceRegistry;
import org.apache.camel.component.ai.resource.AiResourceResult;
import org.apache.camel.component.ai.resource.AiResourceSpec;
import org.apache.camel.support.DefaultExchange;

@Path("/ai-resource")
@ApplicationScoped
public class AiResourceResource {

    @Inject
    CamelContext camelContext;

    @Path("/resources/tag/{tag}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getResourcesByTag(@PathParam("tag") String tag) {
        AiResourceRegistry registry = AiResourceRegistry.getOrCreate(camelContext);
        Set<AiResourceSpec> resources = registry.getResourcesByTag(tag);
        return resources.stream()
                .map(AiResourceSpec::getName)
                .sorted()
                .collect(Collectors.joining(","));
    }

    @Path("/resources/all")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getAllResources() {
        AiResourceRegistry registry = AiResourceRegistry.getOrCreate(camelContext);
        Set<AiResourceSpec> resources = registry.getAllResources();
        return resources.stream()
                .map(AiResourceSpec::getName)
                .sorted()
                .collect(Collectors.joining(","));
    }

    @Path("/resources/{resourceName}/description")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getResourceDescription(@PathParam("resourceName") String resourceName) {
        AiResourceSpec spec = findResource(resourceName);
        if (spec == null) {
            return "NOT_FOUND";
        }
        return spec.getDescription();
    }

    @Path("/read/{resourceName}")
    @GET
    public Response readResource(@PathParam("resourceName") String resourceName) {
        AiResourceSpec spec = findResource(resourceName);
        if (spec == null) {
            return Response.status(404).entity("Resource not found: " + resourceName).build();
        }

        Exchange exchange = new DefaultExchange(camelContext);
        AiResourceResult result = AiResourceExecutor.execute(spec, exchange);

        if (result instanceof AiResourceResult.Text t) {
            return Response.ok(t.value(), spec.getMimeType()).build();
        } else if (result instanceof AiResourceResult.Binary b) {
            return Response.ok(b.value(), spec.getMimeType()).build();
        } else if (result instanceof AiResourceResult.ExecutionError e) {
            return Response.status(500).entity(e.message()).build();
        }
        return Response.status(500).entity("Unknown result type").build();
    }

    @Path("/notes")
    @PUT
    @Consumes(MediaType.TEXT_PLAIN)
    public void writeNotes(String content) throws IOException {
        Files.createDirectories(Paths.get(AiResourceRoutes.NOTES_DIR));
        Files.writeString(Paths.get(AiResourceRoutes.NOTES_DIR, "notes.txt"), content);
    }

    private AiResourceSpec findResource(String resourceName) {
        AiResourceRegistry registry = AiResourceRegistry.getOrCreate(camelContext);
        return registry.getAllResources().stream()
                .filter(s -> s.getName().equals(resourceName))
                .findFirst()
                .orElse(null);
    }
}
