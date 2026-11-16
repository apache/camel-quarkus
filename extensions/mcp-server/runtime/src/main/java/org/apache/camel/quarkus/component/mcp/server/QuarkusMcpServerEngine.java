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
import java.security.Principal;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import io.quarkiverse.mcp.server.AudioContent;
import io.quarkiverse.mcp.server.BlobResourceContents;
import io.quarkiverse.mcp.server.Content;
import io.quarkiverse.mcp.server.EmbeddedResource;
import io.quarkiverse.mcp.server.ImageContent;
import io.quarkiverse.mcp.server.JsonRpcErrorCodes;
import io.quarkiverse.mcp.server.McpException;
import io.quarkiverse.mcp.server.McpServer;
import io.quarkiverse.mcp.server.PromptManager;
import io.quarkiverse.mcp.server.PromptMessage;
import io.quarkiverse.mcp.server.PromptResponse;
import io.quarkiverse.mcp.server.ResourceContents;
import io.quarkiverse.mcp.server.ResourceLink;
import io.quarkiverse.mcp.server.ResourceManager;
import io.quarkiverse.mcp.server.ResourceResponse;
import io.quarkiverse.mcp.server.ResourceTemplateManager;
import io.quarkiverse.mcp.server.Role;
import io.quarkiverse.mcp.server.TextContent;
import io.quarkiverse.mcp.server.TextResourceContents;
import io.quarkiverse.mcp.server.ToolManager;
import io.quarkiverse.mcp.server.ToolResponse;
import io.quarkus.security.identity.CurrentIdentityAssociation;
import io.quarkus.security.identity.SecurityIdentity;
import io.vertx.core.json.JsonObject;
import org.apache.camel.CamelContext;
import org.apache.camel.component.ai.resource.AiResourceUriTemplate;
import org.apache.camel.component.ai.tool.AiToolAnnotations;
import org.apache.camel.component.ai.tool.AiToolParameterHelper.ParameterDef;
import org.apache.camel.component.mcp.server.McpPromptArgument;
import org.apache.camel.component.mcp.server.McpPromptMessage;
import org.apache.camel.component.mcp.server.McpPromptResult;
import org.apache.camel.component.mcp.server.McpResourceReadResult;
import org.apache.camel.component.mcp.server.McpServerEngine;
import org.apache.camel.component.mcp.server.McpServerInfo;
import org.apache.camel.component.mcp.server.McpServerPrompt;
import org.apache.camel.component.mcp.server.McpServerRefusedException;
import org.apache.camel.component.mcp.server.McpServerResource;
import org.apache.camel.component.mcp.server.McpServerResourceTemplate;
import org.apache.camel.component.mcp.server.McpServerTool;
import org.apache.camel.component.mcp.server.McpToolCallContext;
import org.apache.camel.component.mcp.server.McpToolCallResult;
import org.apache.camel.support.service.ServiceSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link McpServerEngine} publishing tools, resources and resource templates into the quarkiverse quarkus-mcp-server
 * through its programmatic manager APIs. Serving concerns (endpoint path, transports, authentication) are owned by
 * quarkus-mcp-server and configured via {@code quarkus.mcp.server.*}.
 */
public class QuarkusMcpServerEngine extends ServiceSupport implements McpServerEngine {

    private static final Logger LOG = LoggerFactory.getLogger(QuarkusMcpServerEngine.class);

    private final ToolManager toolManager;
    private final ResourceManager resourceManager;
    private final ResourceTemplateManager resourceTemplateManager;
    private final PromptManager promptManager;
    // CDI request-scoped proxy; resolve the identity for each tool call, never cache it.
    private final CurrentIdentityAssociation currentIdentityAssociation;
    // names of the tools registered by this engine; a refused tool's name belongs to an application tool
    private final Set<String> registeredToolNames = ConcurrentHashMap.newKeySet();
    // uris of the resources registered by this engine; a refused resource's uri may belong to an application resource
    private final Set<String> registeredResourceUris = ConcurrentHashMap.newKeySet();
    // map URI templates to names so removal only touches templates registered by this engine
    private final Map<String, String> registeredResourceTemplates = new ConcurrentHashMap<>();
    // prompt names registered by this engine; a refused name belongs to an application prompt
    private final Set<String> registeredPromptNames = ConcurrentHashMap.newKeySet();
    private CamelContext camelContext;

    public QuarkusMcpServerEngine(
            ToolManager toolManager,
            ResourceManager resourceManager,
            ResourceTemplateManager resourceTemplateManager,
            PromptManager promptManager,
            CurrentIdentityAssociation currentIdentityAssociation) {
        this.toolManager = toolManager;
        this.resourceManager = resourceManager;
        this.resourceTemplateManager = resourceTemplateManager;
        this.promptManager = promptManager;
        this.currentIdentityAssociation = currentIdentityAssociation;
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

    /**
     * Converts the current Quarkus request identity to the transport-neutral Camel MCP context.
     *
     * The CurrentIdentityAssociation is request scoped. It is deliberately read inside the
     * tool callback so concurrent MCP requests cannot share or overwrite each other's identity.
     */
    private McpToolCallContext currentToolCallContext() {
        // Applications without quarkus-security do not have this association.
        if (currentIdentityAssociation == null) {
            return McpToolCallContext.EMPTY;
        }

        SecurityIdentity identity = currentIdentityAssociation.getIdentity();
        if (identity == null || identity.isAnonymous()) {
            return McpToolCallContext.EMPTY;
        }

        Principal principal = identity.getPrincipal();
        String principalName = principal != null ? principal.getName() : null;
        if (principalName != null && principalName.isBlank()) {
            principalName = null;
        }

        List<String> roles = identity.getRoles().stream()
                .filter(role -> role != null && !role.isBlank())
                .sorted()
                .toList();

        OAuthIdentity oauth = oauthIdentity(principal);

        if (principal == null && principalName == null && roles.isEmpty() && oauth == null) {
            return McpToolCallContext.EMPTY;
        }

        return new McpToolCallContext(
                principal,
                principalName,
                roles.isEmpty() ? null : roles,
                oauth != null ? oauth.subject() : null,
                oauth != null ? oauth.scopes() : null,
                oauth != null ? oauth.claims() : null,
                null);
    }

    /**
     * Extracts OAuth information from a MicroProfile JWT principal without making
     * the MP JWT API a mandatory runtime dependency of the MCP extension.
     */
    private static OAuthIdentity oauthIdentity(Principal principal) {
        if (principal == null) {
            return null;
        }

        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            if (classLoader == null) {
                classLoader = principal.getClass().getClassLoader();
            }

            Class<?> jwtType = Class.forName(
                    "org.eclipse.microprofile.jwt.JsonWebToken", false, classLoader);

            if (!jwtType.isInstance(principal)) {
                return null;
            }

            String subject = (String) jwtType.getMethod("getSubject").invoke(principal);
            Object claimNames = jwtType.getMethod("getClaimNames").invoke(principal);

            Map<String, Object> claims = new LinkedHashMap<>();
            if (claimNames instanceof Collection<?> names) {
                var getClaim = jwtType.getMethod("getClaim", String.class);
                for (Object name : names) {
                    if (name instanceof String claimName) {
                        claims.put(claimName, getClaim.invoke(principal, claimName));
                    }
                }
            }

            List<String> scopes = oauthScopes(claims.get("scope"));

            return new OAuthIdentity(
                    subject,
                    scopes.isEmpty() ? null : scopes,
                    claims.isEmpty() ? null : claims);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
            // The MP JWT API is optional. Principal/name/roles propagation must still work.
            LOG.debug("Unable to extract OAuth JWT information from MCP caller principal", e);
            return null;
        }
    }

    private static List<String> oauthScopes(Object scopeClaim) {
        if (scopeClaim instanceof String scopeString) {
            if (scopeString.isBlank()) {
                return List.of();
            }
            return Arrays.stream(scopeString.trim().split("\\s+"))
                    .filter(scope -> !scope.isBlank())
                    .toList();
        }

        if (scopeClaim instanceof Collection<?> values) {
            return values.stream()
                    .filter(value -> value != null)
                    .map(String::valueOf)
                    .filter(scope -> !scope.isBlank())
                    .toList();
        }

        return List.of();
    }

    private record OAuthIdentity(String subject, List<String> scopes, Map<String, Object> claims) {
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
            // Resolve SecurityIdentity in the current MCP request context. Quarkiverse's
            // synchronous programmatic tool handler is executed on a worker thread.
            McpToolCallContext callContext = currentToolCallContext();
            McpToolCallResult result = tool.handler().call(args, callContext);
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

    @Override
    public void resourceTemplateAdded(McpServerResourceTemplate resourceTemplate) {
        AiResourceUriTemplate matcher;
        try {
            matcher = new AiResourceUriTemplate(resourceTemplate.uriTemplate());
        } catch (IllegalArgumentException e) {
            throw new McpServerRefusedException(
                    "Unsupported MCP resource template '" + resourceTemplate.uriTemplate() + "'", e);
        }

        // Retry cleanup if a previous route stop failed to remove this template.
        try {
            removeRegisteredResourceTemplate(resourceTemplate.uriTemplate());
        } catch (Exception e) {
            throw new McpServerRefusedException(
                    "Previously registered MCP resource template '" + resourceTemplate.uriTemplate()
                            + "' could not be removed before re-registration",
                    e);
        }

        // Multiple matching templates make resource reads ambiguous in quarkus-mcp-server.
        // Definitions on other MCP servers are independent and must remain untouched.
        for (ResourceTemplateManager.ResourceTemplateInfo existing : resourceTemplateManager) {
            if (existing.serverNames().contains(McpServer.DEFAULT)
                    && templatesOverlap(matcher, existing.uriTemplate())) {
                throw new McpServerRefusedException(
                        "Resource template '" + resourceTemplate.uriTemplate()
                                + "' overlaps resource template '"
                                + existing.uriTemplate() + "'");
            }
        }

        ResourceTemplateManager.ResourceTemplateDefinition definition = resourceTemplateManager
                .newResourceTemplate(resourceTemplate.name())
                .setUriTemplate(resourceTemplate.uriTemplate())
                .setMimeType(resourceTemplate.mimeType())
                .setDescription(resourceTemplate.description());
        if (resourceTemplate.title() != null && !resourceTemplate.title().isBlank()) {
            definition.setTitle(resourceTemplate.title());
        }
        definition.setHandler(arguments -> {
            String requestedUri = arguments.requestUri().value();
            McpResourceReadResult result = resourceTemplate.handler().read(requestedUri);
            if (result.isError()) {
                throw new McpException(result.errorMessage(), JsonRpcErrorCodes.INTERNAL_ERROR);
            }
            // Keep Base64 unwrapped: BlobResourceContents.create(String, byte[]) inserts MIME line breaks.
            ResourceContents contents = result.blob() != null
                    ? new BlobResourceContents(requestedUri, Base64.getEncoder().encodeToString(result.blob()),
                            resourceTemplate.mimeType())
                    : new TextResourceContents(requestedUri, result.text(), resourceTemplate.mimeType());
            return new ResourceResponse(List.of(contents));
        });
        try {
            definition.register();
        } catch (IllegalArgumentException e) {
            throw new McpServerRefusedException(
                    "Resource template '" + resourceTemplate.uriTemplate() + "' could not be registered: "
                            + e.getMessage(),
                    e);
        }
        registeredResourceTemplates.put(resourceTemplate.uriTemplate(), resourceTemplate.name());
        LOG.debug("MCP resource template added: {}", resourceTemplate.uriTemplate());
    }

    @Override
    public void resourceTemplateRemoved(String uriTemplate) {
        try {
            removeRegisteredResourceTemplate(uriTemplate);
            LOG.debug("MCP resource template removed: {}", uriTemplate);
        } catch (Exception e) {
            // Keep the ownership entry so a later route start can retry the removal.
            LOG.warn(
                    "Failed to remove MCP resource template {}; ownership retained for retry",
                    uriTemplate, e);
        }
    }

    private void removeRegisteredResourceTemplate(String uriTemplate) throws Exception {
        String name = registeredResourceTemplates.get(uriTemplate);
        if (name == null) {
            // Refused templates are not owned by this engine.
            return;
        }

        resourceTemplateManager.removeResourceTemplate(name, McpServer.DEFAULT);
        registeredResourceTemplates.remove(uriTemplate, name);
    }

    @Override
    public boolean supportsResourceTemplates() {
        return true;
    }

    /**
     * Tests whether a Camel resource template can overlap an application-defined template. If the latter uses a
     * different, valid URI scheme, the templates cannot overlap. Otherwise an unsupported application template is
     * refused because the engine cannot prove that it is safe to publish both.
     */
    private static boolean templatesOverlap(AiResourceUriTemplate candidate, String existingTemplate) {
        try {
            return candidate.overlaps(new AiResourceUriTemplate(existingTemplate));
        } catch (IllegalArgumentException e) {
            String candidateScheme = uriScheme(candidate.getTemplate());
            String existingScheme = uriScheme(existingTemplate);
            if (candidateScheme != null && existingScheme != null
                    && !candidateScheme.equalsIgnoreCase(existingScheme)) {
                return false;
            }
            throw new McpServerRefusedException(
                    "Cannot verify whether resource template '" + candidate.getTemplate()
                            + "' overlaps application template '" + existingTemplate + "'",
                    e);
        }
    }

    private static String uriScheme(String uri) {
        if (uri == null) {
            return null;
        }
        int colon = uri.indexOf(':');
        if (colon <= 0) {
            return null;
        }
        String scheme = uri.substring(0, colon);
        char first = scheme.charAt(0);
        if (!((first >= 'A' && first <= 'Z') || (first >= 'a' && first <= 'z'))) {
            return null;
        }
        for (int i = 1; i < scheme.length(); i++) {
            char c = scheme.charAt(i);
            boolean asciiLetterOrDigit = (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z')
                    || (c >= '0' && c <= '9');
            if (!asciiLetterOrDigit && c != '+' && c != '-' && c != '.') {
                return null;
            }
        }
        return scheme;
    }

    @Override
    public void promptAdded(McpServerPrompt prompt) {
        try {
            removeRegisteredPrompt(prompt.name());
        } catch (RuntimeException e) {
            throw new McpServerRefusedException(
                    "Previously registered MCP prompt '" + prompt.name()
                            + "' could not be removed before re-registration",
                    e);
        }

        // Do not replace a prompt registered directly by the application.
        if (promptManager.getPrompt(prompt.name(), McpServer.DEFAULT) != null) {
            throw new McpServerRefusedException(
                    "MCP prompt '" + prompt.name() + "' is already registered by the application");
        }

        PromptManager.PromptDefinition definition = promptManager.newPrompt(prompt.name())
                .setDescription(prompt.description());

        if (prompt.title() != null && !prompt.title().isBlank()) {
            definition.setTitle(prompt.title());
        }

        for (McpPromptArgument argument : prompt.arguments()) {
            definition.addArgument(argument.name(), argument.description(), argument.required());
        }

        definition.setHandler(arguments -> {
            Map<String, String> args = arguments.args() != null ? arguments.args() : Map.of();
            McpPromptResult result = prompt.handler().get(args);

            if (result == null) {
                throw new McpException("Prompt execution failed", JsonRpcErrorCodes.INTERNAL_ERROR);
            }

            if (result.isError()) {
                int errorCode = result.argumentError()
                        ? JsonRpcErrorCodes.INVALID_PARAMS
                        : JsonRpcErrorCodes.INTERNAL_ERROR;
                throw new McpException(result.errorMessage(), errorCode);
            }

            List<PromptMessage> messages = result.messages().stream()
                    .map(QuarkusMcpServerEngine::toPromptMessage)
                    .toList();

            return new PromptResponse(result.description(), messages);
        });

        try {
            definition.register();
        } catch (IllegalArgumentException e) {
            throw new McpServerRefusedException(
                    "MCP prompt '" + prompt.name() + "' could not be registered: " + e.getMessage(),
                    e);
        }

        registeredPromptNames.add(prompt.name());
        LOG.debug("MCP prompt added: {}", prompt.name());
    }

    @Override
    public void promptRemoved(String promptName) {
        try {
            removeRegisteredPrompt(promptName);
            LOG.debug("MCP prompt removed: {}", promptName);
        } catch (Exception e) {
            // Keep ownership so a later route start can retry the removal.
            LOG.warn(
                    "Failed to remove MCP prompt {}; ownership retained for retry",
                    promptName, e);
        }
    }

    private void removeRegisteredPrompt(String promptName) {
        if (!registeredPromptNames.contains(promptName)) {
            // Refused prompts are not owned by this engine.
            return;
        }

        promptManager.removePrompt(promptName, McpServer.DEFAULT);
        registeredPromptNames.remove(promptName);
    }

    @Override
    public boolean supportsPrompts() {
        return true;
    }

    private static PromptMessage toPromptMessage(McpPromptMessage message) {
        Role role;
        try {
            role = Role.valueOf(message.role().toUpperCase(Locale.ROOT));
        } catch (RuntimeException e) {
            throw new McpException(
                    "Prompt returned an invalid message role",
                    JsonRpcErrorCodes.INTERNAL_ERROR);
        }

        return new PromptMessage(role, toPromptContent(message.content()));
    }

    private static Content toPromptContent(Object value) {
        if (value instanceof String text) {
            return new TextContent(text);
        }
        if (value instanceof Content content) {
            return content;
        }
        if (value instanceof Map<?, ?> map) {
            return toPromptContentMap(map);
        }
        throw new McpException(
                "Prompt returned an unsupported content value",
                JsonRpcErrorCodes.INTERNAL_ERROR);
    }

    /**
     * Converts the runtime-neutral MCP content-block maps used by Camel prompt routes
     * into Quarkiverse MCP content types.
     */
    private static Content toPromptContentMap(Map<?, ?> map) {
        Object typeValue = map.get("type");
        if (!(typeValue instanceof String type)) {
            throw invalidPromptContent("Prompt content block has no valid type");
        }

        return switch (type.toLowerCase(Locale.ROOT)) {
        case "text" -> new TextContent(requiredString(map, "text"));
        case "image" -> new ImageContent(
                requiredString(map, "data"),
                requiredString(map, "mimeType"));
        case "audio" -> new AudioContent(
                requiredString(map, "data"),
                requiredString(map, "mimeType"));
        case "resource" -> toEmbeddedResource(map);
        case "resource_link" -> toResourceLink(map);
        default -> throw invalidPromptContent("Prompt returned an unsupported MCP content type");
        };
    }

    private static Content toEmbeddedResource(Map<?, ?> map) {
        Object resourceValue = map.get("resource");
        if (!(resourceValue instanceof Map<?, ?> resource)) {
            throw invalidPromptContent("Embedded prompt resource is invalid");
        }

        String uri = requiredString(resource, "uri");
        String mimeType = optionalString(resource, "mimeType");

        Object text = resource.get("text");
        if (text instanceof String textValue) {
            return new EmbeddedResource(new TextResourceContents(uri, textValue, mimeType));
        }

        Object blob = resource.get("blob");
        if (blob instanceof String blobValue) {
            return new EmbeddedResource(new BlobResourceContents(uri, blobValue, mimeType));
        }

        throw invalidPromptContent("Embedded prompt resource has no text or blob data");
    }

    private static Content toResourceLink(Map<?, ?> map) {
        String uri = requiredString(map, "uri");
        String name = requiredString(map, "name");
        String mimeType = optionalString(map, "mimeType");
        String title = optionalString(map, "title");
        String description = optionalString(map, "description");

        Integer size = null;
        Object sizeValue = map.get("size");
        if (sizeValue instanceof Number number) {
            size = number.intValue();
        }

        return new ResourceLink(uri, mimeType, name, title, description, size, null, null);
    }

    private static String requiredString(Map<?, ?> map, String key) {
        Object value = map.get(key);
        if (!(value instanceof String stringValue)) {
            throw invalidPromptContent("Prompt content block has an invalid '" + key + "' value");
        }
        return stringValue;
    }

    private static String optionalString(Map<?, ?> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return null;
        }
        if (!(value instanceof String stringValue)) {
            throw invalidPromptContent("Prompt content block has an invalid '" + key + "' value");
        }
        return stringValue;
    }

    private static McpException invalidPromptContent(String message) {
        return new McpException(message, JsonRpcErrorCodes.INTERNAL_ERROR);
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
