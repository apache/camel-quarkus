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
package org.apache.camel.quarkus.component.langchain4j.chat.ql4j.it;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.langchain4j.chat.LangChain4jChatHeaders;

@Path("/langchain4j-chat-ql4j")
@ApplicationScoped
public class LangChain4jChatQl4jResource {
    @Inject
    ProducerTemplate producerTemplate;

    @Path("/simple-message")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public String defaultModelSimpleMessage(String message) {
        return producerTemplate.requestBody("direct:defaultModelSimpleMessage", message, String.class);
    }

    @Path("/prompt-message")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String defaultModelPromptMessage() {
        var promptTemplate = "Create a recipe for a {{dishType}} with the following ingredients: {{ingredients}}";

        Map<String, Object> variables = new HashMap<>();
        variables.put("dishType", "oven dish");
        variables.put("ingredients", "potato, tomato, feta, olive oil");

        return producerTemplate.requestBodyAndHeader("direct:defaultModelPromptMessage", variables,
                LangChain4jChatHeaders.PROMPT_TEMPLATE, promptTemplate, String.class);
    }

    @Path("/multi-turn-conversation")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String defaultModelMultiTurnConversation() {
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new SystemMessage("You are asked to provide recommendations for a restaurant based on user reviews."));
        messages.add(new UserMessage("Hello, my name is Karen."));
        messages.add(new AiMessage("Hello Karen, how can I help you?"));
        messages.add(new UserMessage("I'd like you to recommend a restaurant for me."));
        messages.add(new AiMessage("Sure, what type of cuisine are you interested in?"));
        messages.add(new UserMessage("I'd like Moroccan food."));
        messages.add(new AiMessage("Sure, do you have a preference for the location?"));
        messages.add(new UserMessage("Paris, Rue Montorgueil."));

        return producerTemplate.requestBody("direct:defaultModelMultipleMessages", messages, String.class);
    }

    @Path("/named-model-simple-message")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public String namedModelSimpleMessage(String message) {
        return producerTemplate.requestBody("direct:namedModelSimpleMessage", message, String.class);
    }
}
