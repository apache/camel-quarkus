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
package org.apache.camel.quarkus.component.openai.it;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.apache.camel.CamelContext;
import org.apache.camel.ConsumerTemplate;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.openai.OpenAIConstants;
import org.apache.camel.util.ObjectHelper;

@Path("/openai")
@ApplicationScoped
public class OpenaiResource {
    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    ConsumerTemplate consumerTemplate;

    @Inject
    CamelContext context;

    @Path("/chat")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String chat(
            @QueryParam("userMessage") String userMessage,
            String chatMessageContent) {
        return doChat(userMessage, chatMessageContent);
    }

    @Path("/chat/streaming")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    public void chat(String chatMessageContent) {
        producerTemplate.sendBody("direct:chatStreaming", chatMessageContent);
    }

    @Path("/chat/image")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String chatWithImage(@QueryParam("userMessage") String userMessage, String imagePath) {
        return doChat(userMessage, new File(imagePath));
    }

    @Path("/chat/memory")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String chatWithMemory(String chatMessageContent) {
        return producerTemplate.requestBody("direct:chatWithMemory", chatMessageContent, String.class);
    }

    @Path("/chat/structured/schema")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public String chatWithStructuredOutputWithSchema(String chatMessageContent) {
        return producerTemplate.requestBody("direct:chatStructuredOutputWithSchema", chatMessageContent, String.class);
    }

    @Path("/chat/structured/class")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public String chatWithStructuredOutputWithClass(String chatMessageContent) {
        return producerTemplate.requestBody("direct:chatStructuredOutputWithClass", chatMessageContent, String.class);
    }

    @Path("/chat/results")
    @GET
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String chatResults(@QueryParam("endpointUri") String endpointUri) {
        return consumerTemplate.receiveBody(endpointUri, 10000, String.class);
    }

    @Path("/embeddings")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public List<?> embeddings(String embeddingContent) {
        Object body = embeddingContent;
        String endpointUriSuffix = "embed";
        String[] content = embeddingContent.split(",");
        if (content.length > 1) {
            endpointUriSuffix = "batchEmbed";
            body = Arrays.asList(content);
        }

        return producerTemplate.requestBody("direct:" + endpointUriSuffix, body, List.class);
    }

    @Path("/vector/similarity")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Double vectorSimilarity(@QueryParam("embeddingContent") String embeddingContent, List<Float> vector) {
        Exchange exchange = producerTemplate.request("direct:vectorSimilarity", new Processor() {
            @Override
            public void process(Exchange exchange) throws Exception {
                Message message = exchange.getMessage();
                message.setHeader(OpenAIConstants.REFERENCE_EMBEDDING, vector);
                message.setBody(embeddingContent);
            }
        });
        return exchange.getMessage().getHeader(OpenAIConstants.SIMILARITY_SCORE, Double.class);
    }

    @Path("/routes/{routeId}/{operation}")
    @POST
    public void routeOperations(
            @PathParam("routeId") String routeId,
            @PathParam("operation") String operation) throws Exception {

        if (operation.equals("start")) {
            context.getRouteController().startRoute(routeId);
        } else if (operation.equals("stop")) {
            context.getRouteController().stopRoute(routeId);
        } else {
            throw new IllegalArgumentException("Unknown operation: " + operation);
        }
    }

    private String doChat(String userMessage, Object chatMessageContent) {
        Map<String, Object> headers = new HashMap<>();

        if (ObjectHelper.isNotEmpty(userMessage)) {
            headers.put(OpenAIConstants.USER_MESSAGE, userMessage);
        }

        return producerTemplate.requestBodyAndHeaders("direct:chat", chatMessageContent, headers, String.class);
    }
}
