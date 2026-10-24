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
package org.apache.camel.quarkus.component.mcp.server;

import java.lang.reflect.Type;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import io.quarkiverse.mcp.server.BlobResourceContents;
import io.quarkiverse.mcp.server.JsonRpcErrorCodes;
import io.quarkiverse.mcp.server.McpException;
import io.quarkiverse.mcp.server.McpServer;
import io.quarkiverse.mcp.server.ResourceContents;
import io.quarkiverse.mcp.server.ResourceManager;
import io.quarkiverse.mcp.server.ResourceResponse;
import io.quarkiverse.mcp.server.TextContent;
import io.quarkiverse.mcp.server.TextResourceContents;
import io.quarkiverse.mcp.server.ToolManager;
import io.quarkiverse.mcp.server.ToolResponse;
import io.vertx.core.json.JsonObject;
import org.apache.camel.CamelContext;
import org.apache.camel.component.ai.tool.AiToolAnnotations;
import org.apache.camel.component.ai.tool.AiToolParameterHelper.ParameterDef;
import org.apache.camel.component.mcp.server.McpResourceReadResult;
import org.apache.camel.component.mcp.server.McpServerEngine;
import org.apache.camel.component.mcp.server.McpServerInfo;
import org.apache.camel.component.mcp.server.McpServerResource;
import org.apache.camel.component.mcp.server.McpServerTool;
import org.apache.camel.component.mcp.server.McpToolCallResult;
import org.apache.camel.support.service.ServiceSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link McpServerEngine} publishing tools and resources into the quarkiverse quarkus-mcp-server through its
 * programmatic {@link ToolManager} and {@link ResourceManager} APIs. Serving concerns (endpoint path, transports,
 * authentication) are owned by quarkus-mcp-server and configured via {@code quarkus.mcp.server.*}.
 */
public class QuarkusMcpServerEngine extends ServiceSupport implements McpServerEngine {

    private static final Logger LOG = LoggerFactory.getLogger(QuarkusMcpServerEngine.class);

    private final ToolManager toolManager;
    private final ResourceManager resourceManager;
    // names of the tools registered by this engine; a refused tool's name belongs to an application tool
    private final Set<String> registeredToolNames = ConcurrentHashMap.newKeySet();
    // uris of the resources registered by this engine; a refused resource's uri may belong to an application resource
    private final Set<String> registeredResourceUris = ConcurrentHashMap.newKeySet();
    private CamelContext camelContext;

    public QuarkusMcpServerEngine(ToolManager toolManager, ResourceManager resourceManager) {
        this.toolManager = toolManager;
        this.resourceManager = resourceManager;
    }

    @Override
    public CamelContext getCamelContext() {
        return camelContext;
    }

    @Override
    public void setCamelContext(CamelContext camelContext) {
        this.camelContext = camelContext;
    }

    @Override
    public void initialize(McpServerInfo info) {
        // serving identity is owned by quarkus.mcp.server.*; the hint is deliberately ignored
        LOG.debug("quarkus-mcp-server engine initialized; serving configuration is owned by quarkus.mcp.server.*");
    }

    @Override
    public void toolAdded(McpServerTool tool) {
        ToolManager.ToolDefinition definition = toolManager.newTool(tool.name())
                .setDescription(tool.description());
        for (Map.Entry<String, ParameterDef> parameter : tool.parameters().entrySet()) {
            ParameterDef def = parameter.getValue();
            definition.addArgument(parameter.getKey(), def.getDescription(), def.isRequired(), javaType(def));
        }
        // publish the bridge's pre-built JSON Schema as-is so nested argSchema structures and
        // constraints such as enum survive; without this the schema shown to clients would be
        // regenerated from the flat arguments above (empty for argSchema-based tools)
        if (tool.inputSchemaJson() != null) {
            definition.setInputSchema(new JsonObject(tool.inputSchemaJson()));
        }
        if (tool.outputSchemaJson() != null) {
            definition.setOutputSchema(new JsonObject(tool.outputSchemaJson()));
        }
        applyAnnotations(definition, tool.annotations());
        definition.setHandler(arguments -> {
            Map<String, Object> args = arguments.args() != null ? arguments.args() : Map.of();
            McpToolCallResult result = tool.handler().call(args);
            // the text content is kept next to structuredContent for clients without structured output support
            return result.isError()
                    ? ToolResponse.error(result.text())
                    : new ToolResponse(false, List.of(new TextContent(result.text())), result.structuredContent(), null);
        });
        try {
            definition.register();
        } catch (IllegalArgumentException e) {
            // the name is taken by an application tool: refuse the tool, like the bridge does on a name clash
            LOG.error("Refusing to expose MCP tool '{}': {}. MCP has a flat tool namespace.", tool.name(), e.getMessage());
            return;
        }
        registeredToolNames.add(tool.name());
        LOG.debug("MCP tool added: {}", tool.name());
    }

    @Override
    public void toolRemoved(String toolName) {
        if (!registeredToolNames.remove(toolName)) {
            // refused in toolAdded: removing by name would delete the application tool with the same name
            return;
        }
        try {
            toolManager.removeTool(toolName, McpServer.DEFAULT);
            LOG.debug("MCP tool removed: {}", toolName);
        } catch (Exception e) {
            LOG.debug("Failed to remove MCP tool {}: {}", toolName, e.getMessage());
        }
    }

    @Override
    public void resourceAdded(McpServerResource resource) {
        ResourceManager.ResourceDefinition definition = resourceManager.newResource(resource.name())
                .setUri(resource.uri())
                .setMimeType(resource.mimeType())
                .setDescription(resource.description());
        if (resource.title() != null && !resource.title().isBlank()) {
            definition.setTitle(resource.title());
        }
        definition.setHandler(arguments -> {
            McpResourceReadResult result = resource.handler().read();
            if (result.isError()) {
                // resources/read has no in-band error flag; the message is pre-sanitized by the bridge
                throw new McpException(result.errorMessage(), JsonRpcErrorCodes.INTERNAL_ERROR);
            }
            // plain Base64 as in the Vert.x engine: BlobResourceContents.create(String, byte[]) adds MIME line breaks
            ResourceContents contents = result.blob() != null
                    ? new BlobResourceContents(resource.uri(), Base64.getEncoder().encodeToString(result.blob()),
                            resource.mimeType())
                    : new TextResourceContents(resource.uri(), result.text(), resource.mimeType());
            return new ResourceResponse(List.of(contents));
        });
        try {
            definition.register();
        } catch (IllegalArgumentException e) {
            // quarkus-mcp-server also requires unique names: refuse the resource, like the bridge does on a uri clash
            LOG.error("Refusing to expose MCP resource '{}': {}. Resource names and uris must be unique.",
                    resource.uri(), e.getMessage());
            return;
        }
        registeredResourceUris.add(resource.uri());
        LOG.debug("MCP resource added: {}", resource.uri());
    }

    @Override
    public void resourceRemoved(String resourceUri) {
        if (!registeredResourceUris.remove(resourceUri)) {
            // refused in resourceAdded: removing by uri would delete another resource with the same uri
            return;
        }
        try {
            // only from the default server, where resourceAdded registered it
            resourceManager.removeResource(resourceUri, McpServer.DEFAULT);
            LOG.debug("MCP resource removed: {}", resourceUri);
        } catch (Exception e) {
            LOG.debug("Failed to remove MCP resource {}: {}", resourceUri, e.getMessage());
        }
    }

    @Override
    public boolean supportsResources() {
        return true;
    }

    /**
     * Maps the ai-tool annotation hints to the quarkiverse tool definition: {@code title} to the tool's top-level
     * title and the boolean hints to {@link ToolManager.ToolAnnotations}. The quarkiverse annotations record uses
     * primitive booleans, so hints left unset on the ai-tool endpoint are published with the MCP specification's
     * client-side defaults (readOnly=false, destructive=true, idempotent=false, openWorld=true); when no hint is set
     * at all, no annotations are published.
     */
    private static void applyAnnotations(ToolManager.ToolDefinition definition, AiToolAnnotations annotations) {
        if (annotations == null) {
            return;
        }
        if (annotations.title() != null) {
            definition.setTitle(annotations.title());
        }
        if (annotations.readOnlyHint() != null || annotations.destructiveHint() != null
                || annotations.idempotentHint() != null || annotations.openWorldHint() != null) {
            definition.setAnnotations(new ToolManager.ToolAnnotations(
                    null,
                    annotations.readOnlyHint() != null ? annotations.readOnlyHint() : false,
                    annotations.destructiveHint() != null ? annotations.destructiveHint() : true,
                    annotations.idempotentHint() != null ? annotations.idempotentHint() : false,
                    annotations.openWorldHint() != null ? annotations.openWorldHint() : true));
        }
    }

    private static Type javaType(ParameterDef def) {
        String type = def.getType() != null ? def.getType() : "string";
        return switch (type) {
        case "integer", "int", "long" -> Long.class;
        case "number", "double", "float" -> Double.class;
        case "boolean", "bool" -> Boolean.class;
        default -> String.class;
        };
    }
}
