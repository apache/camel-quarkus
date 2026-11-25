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
import java.util.stream.Collectors;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;

/**
 * Mock model driven by the user message: {@code list} answers with the names of the offered tools (or
 * {@code none}), {@code schema <tool>} answers with the JSON of the offered tool specification,
 * {@code call <tool> <arguments>} calls the tool and answers with its result.
 */
public class ScriptedChatModel implements Supplier<ChatModel> {

    @Override
    public ChatModel get() {
        return new ChatModel() {
            @Override
            public ChatResponse doChat(ChatRequest chatRequest) {
                List<ChatMessage> messages = chatRequest.messages();
                for (ChatMessage message : messages) {
                    if (message instanceof ToolExecutionResultMessage result) {
                        return answer(result.text());
                    }
                }

                String text = messages.stream()
                        .filter(UserMessage.class::isInstance)
                        .map(m -> ((UserMessage) m).singleText())
                        .reduce((first, last) -> last)
                        .orElse("");
                if (text.startsWith("call ")) {
                    String[] parts = text.split(" ", 3);
                    ToolExecutionRequest request = ToolExecutionRequest.builder()
                            .id("call_scripted")
                            .name(parts[1])
                            .arguments(parts[2])
                            .build();
                    return ChatResponse.builder().aiMessage(AiMessage.from(request)).build();
                }

                List<ToolSpecification> tools = chatRequest.toolSpecifications();
                if (tools == null || tools.isEmpty()) {
                    return answer("none");
                }
                if (text.startsWith("schema ")) {
                    String name = text.substring("schema ".length());
                    return answer(tools.stream()
                            .filter(tool -> tool.name().equals(name))
                            .map(ToolSpecification::toJson)
                            .findFirst()
                            .orElse("none"));
                }
                return answer(tools.stream().map(ToolSpecification::name).sorted().collect(Collectors.joining(",")));
            }
        };
    }

    private static ChatResponse answer(String text) {
        return ChatResponse.builder().aiMessage(AiMessage.from(text)).build();
    }
}
