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
package org.apache.camel.quarkus.component.langchain4j.agent.it;

import java.util.HashMap;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.camel.Exchange;
import org.apache.camel.FluentProducerTemplate;
import org.apache.camel.component.langchain4j.agent.api.AiAgentBody;
import org.apache.camel.quarkus.component.langchain4j.agent.it.guardrail.ValidationSuccessInputGuardrail;
import org.apache.camel.quarkus.component.langchain4j.agent.it.guardrail.ValidationSuccessOutputGuardrail;
import org.apache.camel.quarkus.component.langchain4j.agent.it.tool.AdditionTool;

import static org.apache.camel.component.langchain4j.agent.api.Headers.MEMORY_ID;
import static org.apache.camel.component.langchain4j.agent.api.Headers.SYSTEM_MESSAGE;

@Path("/langchain4j-agent")
@ApplicationScoped
public class Langchain4jAgentResource {
    @Inject
    FluentProducerTemplate producerTemplate;

    @Path("/simple")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public Response chatWithSimpleUserMessage(
            @QueryParam("bodyAsBean") boolean isBodyAsBean,
            @QueryParam("systemMessage") String systemMessage,
            String userMessage) {

        Map<String, Object> headers = new HashMap<>();
        if (!isBodyAsBean && systemMessage != null) {
            headers.put(SYSTEM_MESSAGE, systemMessage);
        }

        Object body;
        if (isBodyAsBean) {
            body = new AiAgentBody()
                    .withSystemMessage(systemMessage)
                    .withUserMessage(userMessage);
        } else {
            body = userMessage;
        }

        String result = producerTemplate.to("direct:simple-agent")
                .withBody(body)
                .withHeaders(headers)
                .request(String.class);

        return Response.ok(result.trim()).build();
    }

    @Path("/memory")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public Response chatWithAgentMemory(
            @QueryParam("memoryId") String memoryId,
            String userMessage) {

        Map<String, Object> headers = new HashMap<>();
        headers.put(MEMORY_ID, memoryId);

        String result = producerTemplate.to("direct:agent-with-memory")
                .withBody(userMessage)
                .withHeaders(headers)
                .request(String.class);

        return Response.ok(result.trim()).build();
    }

    @Path("/input/guardrail/success")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public Response chatWithSuccessInputGuardrail(String userMessage) {
        producerTemplate.to("direct:agent-with-success-input-guardrail")
                .withBody(userMessage)
                .request(String.class);

        return Response.ok(ValidationSuccessInputGuardrail.isValidateCalled()).build();
    }

    @Path("/input/guardrail/failure")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public Response chatWithFailingInputGuardrail(String userMessage) {
        Exchange result = producerTemplate.to("direct:agent-with-failing-input-guardrail")
                .withBody(userMessage)
                .send();

        if (result.getException() != null) {
            return Response.serverError()
                    .entity(result.getException().getMessage())
                    .build();
        }

        return Response.ok().build();
    }

    @Path("/output/guardrail/success")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public Response chatWithSuccessOutputGuardrail(String userMessage) {
        producerTemplate.to("direct:agent-with-success-output-guardrail")
                .withBody(userMessage)
                .request(String.class);

        return Response.ok(ValidationSuccessOutputGuardrail.isValidateCalled()).build();
    }

    @Path("/output/guardrail/failure")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public Response chatWithFailingOutputGuardrail(String userMessage) {
        Exchange result = producerTemplate.to("direct:agent-with-failing-output-guardrail")
                .withBody(userMessage)
                .send();

        if (result.getException() != null) {
            return Response.serverError()
                    .entity(result.getException().getMessage())
                    .build();
        }

        return Response.ok().build();
    }

    @Path("/output/guardrail/json/extractor")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public Response chatWithJsonExtractorOutputGuardrail(String userMessage) {
        Exchange result = producerTemplate.to("direct:agent-with-json-extractor-output-guardrail")
                .withBody(userMessage)
                .send();

        if (result.getException() != null) {
            return Response.serverError()
                    .entity(result.getException().getMessage())
                    .build();
        }

        return Response.ok(result.getMessage().getBody()).build();
    }

    @Path("/rag")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public Response chatWithRag(String userMessage) {
        String result = producerTemplate.to("direct:agent-with-rag")
                .withBody(userMessage)
                .request(String.class);

        return Response.ok(result.trim()).build();
    }

    @Path("/tools")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public Response chatWithTools(String userMessage) {
        String result = producerTemplate.to("direct:agent-with-tools")
                .withBody(userMessage)
                .request(String.class);

        return Response.ok(result.trim()).build();
    }

    @Path("/custom/service")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public Response chatWithCustomAiService(String userMessage) {
        String result = producerTemplate.to("direct:agent-with-custom-service")
                .withBody(userMessage)
                .request(String.class);

        return Response.ok(result.trim()).build();
    }

    @Path("/custom/tools")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public Response chatWithCustomTools(String userMessage) {
        try {
            String result = producerTemplate.to("direct:agent-with-custom-tools")
                    .withBody(userMessage)
                    .request(String.class);

            return Response.ok(
                    Map.of("result", result.trim(), "toolWasInvoked", AdditionTool.isToolWasInvoked()))
                    .build();
        } finally {
            AdditionTool.reset();
        }
    }
}
