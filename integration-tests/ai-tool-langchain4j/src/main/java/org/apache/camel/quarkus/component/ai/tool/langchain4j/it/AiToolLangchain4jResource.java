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

import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.apache.camel.CamelContext;
import org.apache.camel.component.ai.tool.AiToolRegistry;
import org.apache.camel.component.ai.tool.AiToolSpec;
import org.apache.camel.quarkus.component.ai.tool.langchain4j.it.service.AdminAiService;
import org.apache.camel.quarkus.component.ai.tool.langchain4j.it.service.WeatherAiService;
import org.apache.camel.quarkus.component.ai.tool.langchain4j.it.service.WeatherAiServiceWrongTag;

@Path("/ai-tool-langchain4j")
@ApplicationScoped
public class AiToolLangchain4jResource {

    @Inject
    CamelContext camelContext;

    @Inject
    WeatherAiService weatherAiService;

    @Inject
    AdminAiService adminAiService;

    @Inject
    WeatherAiServiceWrongTag weatherAiServiceWrongTag;

    @Path("/weather/chat")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String weatherChat(String message) {
        return weatherAiService.chat(message);
    }

    @Path("/admin/chat")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String adminChat(String message) {
        return adminAiService.chat(message);
    }

    @Path("/weather-wrong-tag/chat")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String weatherChatWrongTag(String message) {
        return weatherAiServiceWrongTag.chat(message);
    }

    @Path("/tools/{tag}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String listToolsByTag(@PathParam("tag") String tag) {
        AiToolRegistry registry = AiToolRegistry.getOrCreate(camelContext);
        Set<AiToolSpec> tools = registry.getToolsByTag(tag);
        return tools.stream()
                .map(AiToolSpec::getName)
                .sorted()
                .collect(Collectors.joining(","));
    }

}
