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
package org.apache.camel.quarkus.component.mcp.server.it;

import java.util.List;

import io.quarkiverse.mcp.server.Prompt;
import io.quarkiverse.mcp.server.PromptManager;
import io.quarkiverse.mcp.server.PromptMessage;
import io.quarkiverse.mcp.server.PromptResponse;
import io.quarkiverse.mcp.server.Role;
import io.quarkiverse.mcp.server.TextContent;
import io.quarkus.arc.Arc;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

/**
 * Application prompts from the Quarkus annotation, MCP Java annotation and programmatic APIs.
 */
@ApplicationScoped
public class QuarkusDefinedPrompts {

    public static final String QUARKIVERSE_PROMPT_ANSWER = "Quarkus prompt (declared with @Prompt)";
    public static final String MCPJAVA_PROMPT_ANSWER = "Quarkus prompt (declared with the mcpjava @Prompt)";
    public static final String PROGRAMMATIC_PROMPT_ANSWER = "Prompt registered programmatically";
    public static final String OTHER_SERVER_PROMPT_ANSWER = "Prompt registered on the other MCP server";

    @Prompt(name = "clashing_prompt", description = "Name also requested by an ai-prompt route")
    PromptMessage quarkusPrompt() {
        return PromptMessage.withUserRole(new TextContent(QUARKIVERSE_PROMPT_ANSWER));
    }

    @org.mcpjava.server.prompts.Prompt(name = "mcpjava_clashing_prompt", description = "Name also requested by an ai-prompt route")
    org.mcpjava.server.prompts.PromptResponse mcpJavaPrompt() {
        return org.mcpjava.server.prompts.PromptResponse.of(
                org.mcpjava.server.Role.USER,
                org.mcpjava.server.content.TextContent.of(MCPJAVA_PROMPT_ANSWER));
    }

    void registerPrompts(@Observes StartupEvent event) {
        // Looked up rather than injected, just like QuarkusDefinedResources.
        PromptManager promptManager = Arc.container().instance(PromptManager.class).get();
        promptManager.newPrompt("user_prompt")
                .setDescription("Prompt registered by the application")
                .setHandler(args -> new PromptResponse(null,
                        List.of(new PromptMessage(Role.USER,
                                new TextContent(PROGRAMMATIC_PROMPT_ANSWER)))))
                .register();

        // This prompt deliberately shares a name with the default-server Camel prompt.
        promptManager.newPrompt("greeting")
                .setServerName("other")
                .setDescription("Prompt registered by the application on another server")
                .setHandler(args -> new PromptResponse(null,
                        List.of(new PromptMessage(Role.USER,
                                new TextContent(OTHER_SERVER_PROMPT_ANSWER)))))
                .register();
    }
}
