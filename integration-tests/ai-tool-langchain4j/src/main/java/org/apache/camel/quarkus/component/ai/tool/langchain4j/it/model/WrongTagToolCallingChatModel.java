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
 * Mock model for testing tag isolation. Calls {@code getWeather} only if it appears in the
 * available tool specifications — when the interceptor works correctly and this service is
 * tagged with {@code adminTag}, the weather tool should NOT be visible.
 */
public class WrongTagToolCallingChatModel implements Supplier<ChatModel> {

    @Override
    public ChatModel get() {
        return new ChatModel() {
            @Override
            public ChatResponse doChat(ChatRequest chatRequest) {
                List<ChatMessage> messages = chatRequest.messages();
                boolean hasToolResult = messages.stream()
                        .anyMatch(m -> m instanceof ToolExecutionResultMessage);

                if (!hasToolResult) {
                    boolean weatherToolAvailable = chatRequest.toolSpecifications() != null
                            && chatRequest.toolSpecifications().stream()
                                    .anyMatch(ts -> "getWeather".equals(ts.name()));

                    if (weatherToolAvailable) {
                        ToolExecutionRequest request = ToolExecutionRequest.builder()
                                .id("call_wrong_tag")
                                .name("getWeather")
                                .arguments("{\"city\":\"Prague\"}")
                                .build();
                        return ChatResponse.builder()
                                .aiMessage(AiMessage.from(request))
                                .build();
                    }

                    return ChatResponse.builder()
                            .aiMessage(new AiMessage("No weather tool available"))
                            .build();
                }

                String toolResult = messages.stream()
                        .filter(m -> m instanceof ToolExecutionResultMessage)
                        .map(m -> ((ToolExecutionResultMessage) m).text())
                        .findFirst()
                        .orElse("unknown");
                return ChatResponse.builder()
                        .aiMessage(new AiMessage(toolResult))
                        .build();
            }
        };
    }
}
