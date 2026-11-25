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
package org.apache.camel.quarkus.component.ai.tool.langchain4j.it;

import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.langchain4j.agent.api.AgentConfiguration;
import org.apache.camel.component.langchain4j.agent.api.AgentWithMemory;
import org.apache.camel.component.langchain4j.agent.api.Headers;
import org.apache.camel.quarkus.component.ai.tool.langchain4j.it.model.ScriptedChatModel;

public class AiToolLangchain4jRoutes extends RouteBuilder {
    @Override
    public void configure() {
        from("ai-tool:getWeather?"
                + "tags=weatherTag"
                + "&description=Get the current weather for a city"
                + "&parameter.city=string"
                + "&parameter.city.required=true"
                + "&parameter.city.description=The city name")
                .setBody(simple("Sunny in ${header.city}, 1111 celsius"));

        from("ai-tool:getNews?"
                + "tags=adminTag"
                + "&description=Get the latest news about a topic"
                + "&parameter.topic=string"
                + "&parameter.topic.required=true"
                + "&parameter.topic.description=The news topic")
                .setBody(simple("Latest news about ${header.topic}"));

        from("ai-tool:failingTool?tags=failTag&description=A tool that always fails")
                .throwException(new IllegalStateException("Internal failure detail"));

        from("ai-tool:createOrder?tags=orderTag&description=Create an order&argSchema=classpath:ai-tool/order-schema.json")
                .setBody(simple(
                        "${header.customer[name]},${header.items[0][sku]},${header.items[0][quantity]},${header.priority}"));

        // A Camel agent selects its tools by the tags parameter only, CamelAiToolProvider must not add the others
        bindToRegistry("camelAgent", new AgentWithMemory(new AgentConfiguration()
                .withChatModel(new ScriptedChatModel().get())
                .withChatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(10))));
        from("direct:camelAgent")
                .setHeader(Headers.MEMORY_ID, constant(1))
                .to("langchain4j-agent:camelAgent?agent=#camelAgent&tags=weatherTag");
    }
}
