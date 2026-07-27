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
package org.apache.camel.quarkus.component.support.langchain4j;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.service.tool.ToolExecutor;
import dev.langchain4j.service.tool.ToolProvider;
import dev.langchain4j.service.tool.ToolProviderRequest;
import dev.langchain4j.service.tool.ToolProviderResult;
import jakarta.inject.Inject;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.component.ai.tool.AiToolExecutor;
import org.apache.camel.component.ai.tool.AiToolRegistry;
import org.apache.camel.component.ai.tool.AiToolResult;
import org.apache.camel.component.ai.tool.AiToolSpec;
import org.apache.camel.support.DefaultExchange;
import org.jboss.logging.Logger;

/**
 * Bridges Camel's {@link AiToolRegistry} to langchain4j's {@link ToolProvider} SPI. When registered as a CDI bean (done
 * automatically by the deployment processor when both {@code camel-ai-tool} and quarkus-langchain4j are on the
 * classpath), all Camel routes registered via {@code ai-tool:} endpoints become available to
 * {@code @RegisterAiService} AI services without explicit configuration.
 */
public class CamelAiToolProvider implements ToolProvider {

    private static final Logger LOG = Logger.getLogger(CamelAiToolProvider.class);
    static final Map<String, String> TAG_MAP = new ConcurrentHashMap<>();
    // Set by CamelAiToolsInterceptor before each AI service call so provideTools() can filter by the calling service's tag
    private static final ThreadLocal<String> CURRENT_TAG = new ThreadLocal<>();

    @Inject
    CamelContext camelContext;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    AiToolSpecConverter converter;

    static String getCurrentTag() {
        return CURRENT_TAG.get();
    }

    static void setCurrentTag(String tag) {
        CURRENT_TAG.set(tag);
    }

    static void clearCurrentTag() {
        CURRENT_TAG.remove();
    }

    @Override
    public ToolProviderResult provideTools(ToolProviderRequest request) {
        AiToolRegistry registry = AiToolRegistry.getOrCreate(camelContext);
        String effectiveTag = CURRENT_TAG.get();
        Set<AiToolSpec> tools = effectiveTag != null ? registry.getToolsByTag(effectiveTag) : registry.getAllTools();

        ToolProviderResult.Builder resultBuilder = ToolProviderResult.builder();
        for (AiToolSpec spec : tools) {
            ToolSpecification toolSpec = converter.toToolSpecification(spec);
            ToolExecutor executor = createExecutor(spec);
            resultBuilder.add(toolSpec, executor);
        }

        return resultBuilder.build();
    }

    private ToolExecutor createExecutor(AiToolSpec spec) {
        return (ToolExecutionRequest request, Object memoryId) -> {
            Map<String, Object> arguments = parseArguments(request);
            if (arguments == null) {
                return "Invalid arguments: could not parse the provided JSON arguments: " + request.arguments();
            }
            Exchange exchange = new DefaultExchange(camelContext);
            AiToolResult result = AiToolExecutor.execute(spec, arguments, exchange);
            return toToolResponse(spec.getName(), result);
        };
    }

    private Map<String, Object> parseArguments(ToolExecutionRequest request) {
        String jsonArguments = request.arguments();
        if (jsonArguments == null || jsonArguments.trim().isEmpty()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(jsonArguments, new TypeReference<>() {
            });
        } catch (Exception e) {
            LOG.debugf(e, "Failed to parse tool arguments: %s", jsonArguments);
            return null;
        }
    }

    private String toToolResponse(String toolName, AiToolResult result) {
        if (result instanceof AiToolResult.Success success) {
            return success.value();
        } else if (result instanceof AiToolResult.ArgumentError error) {
            return "Invalid arguments: " + error.message();
        } else if (result instanceof AiToolResult.ExecutionError error) {
            LOG.warnf("Tool '%s' execution failed: %s", toolName, error.message());
            return "Tool execution failed";
        }
        return "Tool execution failed";
    }

}
