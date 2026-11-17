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
package org.apache.camel.quarkus.component.aws.bedrock.it;

import java.util.List;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.component.aws2.bedrock.runtime.BedrockConstants;
import software.amazon.awssdk.services.bedrockruntime.model.ContentBlock;
import software.amazon.awssdk.services.bedrockruntime.model.ConversationRole;
import software.amazon.awssdk.services.bedrockruntime.model.InferenceConfiguration;
import software.amazon.awssdk.services.bedrockruntime.model.Message;
import software.amazon.awssdk.services.bedrockruntime.model.TokenUsage;
import tools.jackson.databind.json.JsonMapper;

@Path("/aws-bedrock")
@ApplicationScoped
public class AwsBedrockResource {
    private static final int MAX_TOKENS = 50;
    private static final String ENDPOINT_URI = "aws-bedrock:%s?operation=%s&modelId={{aws-bedrock.model-id}}";

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    JsonMapper jsonMapper;

    @Path("/converse")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> converse(String prompt) {
        return converseResult(converse("converse", prompt));
    }

    @Path("/converse-stream")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> converseStream(String prompt) {
        return converseResult(converse("converseStream", prompt));
    }

    @Path("/invoke-text-model")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public String invokeTextModel(String prompt) {
        // Amazon Nova messages-v1 request schema
        String body = """
                {
                  "schemaVersion": "messages-v1",
                  "messages": [{"role": "user", "content": [{"text": %s}]}],
                  "inferenceConfig": {"maxTokens": %d}
                }
                """.formatted(jsonMapper.writeValueAsString(prompt), MAX_TOKENS);

        Exchange exchange = producerTemplate.request(ENDPOINT_URI.formatted("invoke", "invokeTextModel"), e -> {
            e.getMessage().setHeader(BedrockConstants.MODEL_CONTENT_TYPE, MediaType.APPLICATION_JSON);
            e.getMessage().setHeader(BedrockConstants.MODEL_ACCEPT_CONTENT_TYPE, MediaType.APPLICATION_JSON);
            e.getMessage().setBody(body);
        });
        return checkResult(exchange).getMessage().getBody().toString();
    }

    private Exchange converse(String operation, String prompt) {
        Message userMessage = Message.builder()
                .role(ConversationRole.USER)
                .content(ContentBlock.fromText(prompt))
                .build();

        Exchange exchange = producerTemplate.request(ENDPOINT_URI.formatted(operation, operation), e -> {
            e.getMessage().setHeader(BedrockConstants.CONVERSE_MESSAGES, List.of(userMessage));
            e.getMessage().setHeader(BedrockConstants.CONVERSE_INFERENCE_CONFIG,
                    InferenceConfiguration.builder().maxTokens(MAX_TOKENS).build());
        });
        return checkResult(exchange);
    }

    private static Map<String, Object> converseResult(Exchange exchange) {
        org.apache.camel.Message message = exchange.getMessage();
        TokenUsage usage = message.getHeader(BedrockConstants.CONVERSE_USAGE, TokenUsage.class);
        return Map.of(
                "text", message.getBody(String.class),
                "stopReason", message.getHeader(BedrockConstants.CONVERSE_STOP_REASON, String.class),
                "inputTokens", usage.inputTokens(),
                "outputTokens", usage.outputTokens());
    }

    private static Exchange checkResult(Exchange exchange) {
        if (exchange.getException() != null) {
            throw RuntimeCamelException.wrapRuntimeException(exchange.getException());
        }
        return exchange;
    }
}
