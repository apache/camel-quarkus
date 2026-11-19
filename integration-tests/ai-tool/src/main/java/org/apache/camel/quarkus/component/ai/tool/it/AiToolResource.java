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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
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
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.ai.tool.AiToolExecutor;
import org.apache.camel.component.ai.tool.AiToolRegistry;
import org.apache.camel.component.ai.tool.AiToolRegistryListener;
import org.apache.camel.component.ai.tool.AiToolResult;
import org.apache.camel.component.ai.tool.AiToolSpec;
import org.apache.camel.spi.CamelEvent.RouteStartedEvent;
import org.apache.camel.support.DefaultExchange;

@Path("/ai-tool")
@ApplicationScoped
public class AiToolResource {

    @Inject
    CamelContext camelContext;

    private volatile String toolsAtProbeStart;

    void onRouteStarted(@Observes RouteStartedEvent event) {
        if ("startupProbe".equals(event.getRoute().getRouteId())) {
            toolsAtProbeStart = getAllTools();
        }
    }

    @Path("/tools/at-startup")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getToolsAtStartup() {
        return toolsAtProbeStart;
    }

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

    @Path("/tools/{toolName}/annotations")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getToolAnnotations(@PathParam("toolName") String toolName) {
        return String.valueOf(findTool(toolName).getAnnotations());
    }

    @Path("/invalid/{name}")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public String addInvalidTool(@PathParam("name") String name) throws Exception {
        String uri = switch (name) {
        case "argSchemaAndParameters" ->
            "ai-tool:invalid?parameter.a=string&argSchema=RAW({\"properties\":{\"a\":{\"type\":\"string\"}}})";
        case "argSchemaNotObject" -> "ai-tool:invalid?argSchema=RAW([1])";
        case "argSchemaWithoutProperties" -> "ai-tool:invalid?argSchema=RAW({\"type\":\"object\"})";
        case "argSchemaBadRequired" ->
            "ai-tool:invalid?argSchema=RAW({\"properties\":{\"a\":{\"type\":\"string\"}},\"required\":[\"b\"]})";
        case "duplicateName" -> "ai-tool:getWeather?tags=weather&description=Duplicate";
        default -> throw new IllegalArgumentException(name);
        };
        try {
            camelContext.addRoutes(new RouteBuilder() {
                @Override
                public void configure() {
                    from(uri).routeId("invalid").log("invalid called");
                }
            });
            return "started";
        } catch (Exception e) {
            Throwable cause = e;
            while (cause.getCause() != null) {
                cause = cause.getCause();
            }
            return cause.getMessage();
        } finally {
            if (camelContext.getRoute("invalid") != null) {
                camelContext.getRouteController().stopRoute("invalid");
                camelContext.removeRoute("invalid");
            }
        }
    }

    @Path("/producer")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public String createProducer() {
        try {
            camelContext.getEndpoint("ai-tool:getWeather").createProducer();
            return "created";
        } catch (Exception e) {
            return e.getMessage();
        }
    }

    @Path("/routes/{routeId}/suspend-resume")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public String suspendAndResume(@PathParam("routeId") String routeId) throws Exception {
        List<String> events = new CopyOnWriteArrayList<>();
        AiToolRegistryListener listener = new AiToolRegistryListener() {
            @Override
            public void toolRegistered(String tag, AiToolSpec spec) {
                events.add("registered:" + tag + ":" + spec.getName());
            }

            @Override
            public void toolDeregistered(String tag, AiToolSpec spec) {
                events.add("deregistered:" + tag + ":" + spec.getName());
            }
        };
        AiToolRegistry registry = AiToolRegistry.getOrCreate(camelContext);
        registry.addListener(listener);
        try {
            camelContext.getRouteController().suspendRoute(routeId);
            events.add("suspended:" + (findTool(routeId) != null ? "visible" : "hidden"));
            camelContext.getRouteController().resumeRoute(routeId);
            events.add("resumed:" + (findTool(routeId) != null ? "visible" : "hidden"));
        } finally {
            registry.removeListener(listener);
        }
        return String.join(",", events);
    }

    private AiToolSpec findTool(String toolName) {
        AiToolRegistry registry = AiToolRegistry.getOrCreate(camelContext);
        return registry.getAllTools().stream()
                .filter(s -> s.getName().equals(toolName))
                .findFirst()
                .orElse(null);
    }
}
