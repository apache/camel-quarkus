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
import java.util.Map;

import io.quarkiverse.mcp.server.TextContent;
import io.quarkiverse.mcp.server.ToolManager;
import io.quarkiverse.mcp.server.ToolResponse;
import io.vertx.core.json.JsonObject;
import org.apache.camel.CamelContext;
import org.apache.camel.component.ai.tool.AiToolAnnotations;
import org.apache.camel.component.ai.tool.AiToolParameterHelper.ParameterDef;
import org.apache.camel.component.mcp.server.McpServerEngine;
import org.apache.camel.component.mcp.server.McpServerInfo;
import org.apache.camel.component.mcp.server.McpServerTool;
import org.apache.camel.component.mcp.server.McpToolCallResult;
import org.apache.camel.support.service.ServiceSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link McpServerEngine} publishing tools into the quarkiverse quarkus-mcp-server through its programmatic
 * {@link ToolManager} API. Serving concerns (endpoint path, transports, authentication) are owned by
 * quarkus-mcp-server and configured via {@code quarkus.mcp.server.*}.
 */
public class QuarkusMcpServerEngine extends ServiceSupport implements McpServerEngine {

    private static final Logger LOG = LoggerFactory.getLogger(QuarkusMcpServerEngine.class);

    private final ToolManager toolManager;
    private CamelContext camelContext;

    public QuarkusMcpServerEngine(ToolManager toolManager) {
        this.toolManager = toolManager;
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
        applyAnnotations(definition, tool.annotations());
        definition.setHandler(arguments -> {
            Map<String, Object> args = arguments.args() != null ? arguments.args() : Map.of();
            McpToolCallResult result = tool.handler().call(args);
            return result.isError()
                    ? ToolResponse.error(result.text()) : ToolResponse.success(new TextContent(result.text()));
        });
        definition.register();
        LOG.debug("MCP tool added: {}", tool.name());
    }

    @Override
    public void toolRemoved(String toolName) {
        try {
            toolManager.removeTool(toolName);
            LOG.debug("MCP tool removed: {}", toolName);
        } catch (Exception e) {
            LOG.debug("Failed to remove MCP tool {}: {}", toolName, e.getMessage());
        }
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
