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
package org.apache.camel.quarkus.component.langchain.chat.it;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.langchain4j.chat.LangChain4jChatHeaders;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.util.ObjectHelper;
import org.eclipse.microprofile.config.ConfigProvider;

@Path("/langchain4j-chat")
@ApplicationScoped
public class LangChain4jChatResource {
    @Inject
    CamelContext context;

    @Inject
    ProducerTemplate producerTemplate;

    @Path("/simple-message")
    @GET
    public Response sendSimpleMessage(
            @QueryParam("directEndpointUri") String directEndpointUri,
            @QueryParam("mockEndpointUri") String mockEndpointUri) throws Exception {
        MockEndpoint mockEndpoint = context.getEndpoint(mockEndpointUri, MockEndpoint.class);
        mockEndpoint.expectedMessageCount(1);
        mockEndpoint.expectedMessagesMatches(exchange -> {
            String body = exchange.getMessage().getBody(String.class);
            return body.trim().startsWith("Hello");
        });

        producerTemplate.sendBody(directEndpointUri, "Hello my name is Darth Vader!");
        mockEndpoint.assertIsSatisfied(10000);

        return Response.ok().build();
    }

    @Path("/prompt-message")
    @GET
    public Response sendMessageWithPrompt() throws Exception {
        MockEndpoint mockEndpoint = context.getEndpoint("mock:messagePromptResponse", MockEndpoint.class);
        mockEndpoint.expectedMessageCount(1);
        mockEndpoint.expectedMessagesMatches(exchange -> {
            String body = exchange.getMessage().getBody(String.class);
            body = body.trim();
            return body.contains("potato") &&
                    body.contains("tomato") &&
                    body.contains("feta") &&
                    body.contains("olive oil");
        });

        var promptTemplate = "Create a recipe for a {{dishType}} with the following ingredients: {{ingredients}}";

        Map<String, Object> variables = new HashMap<>();
        variables.put("dishType", "oven dish");
        variables.put("ingredients", "potato, tomato, feta, olive oil");

        producerTemplate.sendBodyAndHeader("direct:send-message-prompt", variables,
                LangChain4jChatHeaders.PROMPT_TEMPLATE, promptTemplate);

        mockEndpoint.assertIsSatisfied(10000);

        return Response.ok().build();
    }

    @Path("/multiple-messages")
    @GET
    public Response sendMultipleMessage() throws Exception {
        MockEndpoint mockEndpoint = context.getEndpoint("mock:multipleMessageResponse", MockEndpoint.class);
        mockEndpoint.expectedMessageCount(1);
        mockEndpoint.expectedMessagesMatches(exchange -> {
            String chatResponse = exchange.getMessage().getBody(String.class);
            return ObjectHelper.isNotEmpty(chatResponse);
        });

        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new SystemMessage("You are asked to provide recommendations for a restaurant based on user reviews."));
        messages.add(new UserMessage("Hello, my name is Karen."));
        messages.add(new AiMessage("Hello Karen, how can I help you?"));
        messages.add(new UserMessage("I'd like you to recommend a restaurant for me."));
        messages.add(new AiMessage("Sure, what type of cuisine are you interested in?"));
        messages.add(new UserMessage("I'd like Moroccan food."));
        messages.add(new AiMessage("Sure, do you have a preference for the location?"));
        messages.add(new UserMessage("Paris, Rue Montorgueil."));

        producerTemplate.sendBody("direct:send-multiple", messages);

        mockEndpoint.assertIsSatisfied(10000);

        return Response.ok().build();
    }

    @Produces
    @Named("m1")
    ChatModel model() {
        return OllamaChatModel.builder()
                .baseUrl(ConfigProvider.getConfig().getValue("langchain4j.ollama.base-url", String.class))
                .modelName("orca-mini")
                .temperature(0.3)
                .build();
    }

    @Produces
    @Named("m2")
    ChatModel model2() {
        return OllamaChatModel.builder()
                .baseUrl(ConfigProvider.getConfig().getValue("langchain4j.ollama.base-url", String.class))
                .modelName("orca-mini")
                .temperature(0.3)
                .build();
    }
}
