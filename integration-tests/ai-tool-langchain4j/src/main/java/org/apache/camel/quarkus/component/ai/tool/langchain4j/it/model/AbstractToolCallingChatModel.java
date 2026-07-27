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
package org.apache.camel.quarkus.component.ai.tool.langchain4j.it.model;

import java.util.List;
import java.util.function.Supplier;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;

/**
 * Simulates the two-turn tool-calling protocol used by langchain4j:
 *
 * <ol>
 * <li><b>Turn 1</b> — langchain4j sends the user message and available tool specifications to the model.
 * The model responds with a {@link ToolExecutionRequest} ("I want to call tool X with args Y").
 * No tool is executed yet.</li>
 * <li><b>Tool execution</b> — langchain4j sees the request, invokes the matching {@code ToolExecutor}
 * (in our case the Camel {@code ai-tool:} route), and captures the result.</li>
 * <li><b>Turn 2</b> — langchain4j calls the model again, appending a {@link ToolExecutionResultMessage}
 * with the tool's output. The model returns a final text answer.</li>
 * </ol>
 *
 * Subclasses only define which tool to call and how to format the final response.
 */
public abstract class AbstractToolCallingChatModel implements Supplier<ChatModel> {

    // Correlation ID for matching ToolExecutionResultMessage back to the request — required by the
    // builder but the actual value is irrelevant in tests with a single tool call per turn.
    protected abstract String toolCallId();

    protected abstract String toolName();

    protected abstract String toolArguments();

    protected abstract String formatResponse(String toolResult);

    protected abstract String getNameOfService();

    @Override
    public ChatModel get() {
        return new ChatModel() {
            @Override
            public ChatResponse doChat(ChatRequest chatRequest) {
                List<ChatMessage> messages = chatRequest.messages();

                boolean hasToolResult = messages.stream()
                        .anyMatch(m -> m instanceof ToolExecutionResultMessage);

                if (!hasToolResult) {
                    ToolExecutionRequest toolRequest = ToolExecutionRequest.builder()
                            .id(toolCallId())
                            .name(toolName())
                            .arguments(toolArguments())
                            .build();
                    return ChatResponse.builder()
                            .aiMessage(AiMessage.from(toolRequest))
                            .build();
                }

                // #2 tool's response already present
                String toolResult = messages.stream()
                        .filter(m -> m instanceof ToolExecutionResultMessage)
                        .map(m -> ((ToolExecutionResultMessage) m).text())
                        .findFirst()
                        .orElse("unknown");

                return ChatResponse.builder()
                        .aiMessage(new AiMessage(formatResponse(toolResult)))
                        .build();
            }
        };
    }
}
