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
package org.apache.camel.quarkus.component.ai.tool.it;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.component.ai.tool.AiToolExecutor;
import org.apache.camel.component.ai.tool.AiToolRegistry;
import org.apache.camel.component.ai.tool.AiToolResult;
import org.apache.camel.component.ai.tool.AiToolSpec;
import org.apache.camel.support.DefaultExchange;

@Path("/ai-tool")
@ApplicationScoped
public class AiToolResource {

    @Inject
    CamelContext camelContext;

    @Path("/tools/tag/{tag}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getToolsByTag(@PathParam("tag") String tag) {
        AiToolRegistry registry = AiToolRegistry.getOrCreate(camelContext);
        Set<AiToolSpec> tools = registry.getToolsByTag(tag);
        return tools.stream()
                .map(AiToolSpec::getName)
                .sorted()
                .collect(Collectors.joining(","));
    }

    @Path("/tools/all")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getAllTools() {
        AiToolRegistry registry = AiToolRegistry.getOrCreate(camelContext);
        Set<AiToolSpec> tools = registry.getAllTools();
        return tools.stream()
                .map(AiToolSpec::getName)
                .sorted()
                .collect(Collectors.joining(","));
    }

    @Path("/tools/{toolName}/description")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getToolDescription(@PathParam("toolName") String toolName) {
        AiToolSpec spec = findTool(toolName);
        if (spec == null) {
            return "NOT_FOUND";
        }
        return spec.getDescription();
    }

    @Path("/tools/{toolName}/schema")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String getToolSchema(@PathParam("toolName") String toolName) {
        AiToolSpec spec = findTool(toolName);
        if (spec == null) {
            return "{}";
        }
        return spec.getParametersJsonSchema();
    }

    @Path("/execute/{toolName}")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public Response executeTool(
            @PathParam("toolName") String toolName,
            @QueryParam("param") java.util.List<String> params) {

        AiToolSpec spec = findTool(toolName);
        if (spec == null) {
            return Response.status(404).entity("Tool not found: " + toolName).build();
        }

        Map<String, Object> arguments = new HashMap<>();
        if (params != null) {
            for (String param : params) {
                String[] parts = param.split("=", 2);
                if (parts.length == 2) {
                    arguments.put(parts[0], parts[1]);
                }
            }
        }

        Exchange exchange = new DefaultExchange(camelContext);
        AiToolResult result = AiToolExecutor.execute(spec, arguments, exchange);

        if (result instanceof AiToolResult.Success s) {
            return Response.ok(s.value()).build();
        } else if (result instanceof AiToolResult.ArgumentError e) {
            return Response.status(400).entity(e.message()).build();
        } else if (result instanceof AiToolResult.ExecutionError e) {
            return Response.status(500).entity(e.message()).build();
        }
        return Response.status(500).entity("Unknown result type").build();
    }

    private AiToolSpec findTool(String toolName) {
        AiToolRegistry registry = AiToolRegistry.getOrCreate(camelContext);
        return registry.getAllTools().stream()
                .filter(s -> s.getName().equals(toolName))
                .findFirst()
                .orElse(null);
    }
}
