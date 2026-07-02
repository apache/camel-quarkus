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
package org.apache.camel.quarkus.component.a2a.it;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.a2a.A2AConstants;
import org.apache.camel.component.a2a.A2AProgress;
import org.apache.camel.component.a2a.model.Artifact;
import org.apache.camel.component.a2a.model.Message;
import org.apache.camel.component.a2a.model.TextPart;

@ApplicationScoped
public class A2aRoutes extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        // Consumer: expose an A2A agent using JSON-RPC binding (recommended for platform-http)
        from("a2a:test-agent?name=Test Agent&description=A Quarkus test agent&version=1.0.0"
                + "&protocolBinding=JSONRPC&validateAuth=false")
                .setBody(simple("Echo: ${body}"));

        // Producer routes that call the local consumer agent
        from("direct:send-message")
                .toD("a2a:http://localhost:${header.CamelA2APort}"
                        + "?protocolBinding=JSONRPC&dataFormat=POJO");

        from("direct:send-message-payload")
                .toD("a2a:http://localhost:${header.CamelA2APort}"
                        + "?protocolBinding=JSONRPC&dataFormat=PAYLOAD");

        from("direct:send-message-raw")
                .toD("a2a:http://localhost:${header.CamelA2APort}"
                        + "?protocolBinding=JSONRPC&dataFormat=RAW");

        from("direct:get-task")
                .setHeader(A2AConstants.OPERATION, constant("TASK_GET"))
                .toD("a2a:http://localhost:${header.CamelA2APort}"
                        + "?protocolBinding=JSONRPC&dataFormat=POJO");

        from("direct:cancel-task")
                .setHeader(A2AConstants.OPERATION, constant("TASK_CANCEL"))
                .toD("a2a:http://localhost:${header.CamelA2APort}"
                        + "?protocolBinding=JSONRPC&dataFormat=POJO");

        // Streaming consumer: emits SSE progress events and an artifact (separate basePath to avoid route collision)
        from("a2a:classpath:cards/streaming-agent-card.json?protocolBinding=JSONRPC&validateAuth=false&basePath=/streaming")
                .process(exchange -> {
                    A2AProgress.emit(exchange, "Step 1 complete");
                    A2AProgress.emit(exchange, "Step 2 complete");
                    Artifact artifact = Artifact.builder()
                            .artifactId("art-1")
                            .name("test-artifact")
                            .parts(List.of(new TextPart("artifact content")))
                            .build();
                    A2AProgress.emitArtifact(exchange, artifact, false, true);
                })
                .setBody(constant("Streaming done"));

        // Push notification consumer: enables push config CRUD testing
        from("a2a:classpath:cards/push-agent-card.json?protocolBinding=JSONRPC&validateAuth=false"
                + "&basePath=/push&allowLocalWebhookUrls=true")
                .setBody(simple("Push Echo: ${body}"));

        // Full-feature consumer: rich agent card with all fields for serialization testing
        from("a2a:classpath:cards/full-agent-card.json?protocolBinding=JSONRPC&validateAuth=false&basePath=/full")
                .setBody(simple("Full Echo: ${body}"));

        // POJO data format consumer: receives Message object body
        from("a2a:pojo-agent?name=POJO Agent&description=POJO data format test&version=1.0.0"
                + "&protocolBinding=JSONRPC&validateAuth=false&basePath=/pojo&dataFormat=POJO")
                .process(exchange -> {
                    Message msg = exchange.getMessage().getBody(Message.class);
                    exchange.getMessage().setBody(
                            String.format("role=%s,parts=%d", msg.role().getValue(), msg.parts().size()));
                });

        // Streaming consumer using ${a2a:emit()} Simple language function
        from("a2a:classpath:cards/emit-agent-card.json?protocolBinding=JSONRPC&validateAuth=false&basePath=/emit")
                .script(simple("${a2a:emit('Simple emit step 1')}"))
                .script(simple("${a2a:emit('Simple emit step 2')}"))
                .setBody(constant("Emit done"));

        // API key authenticated consumer
        from("a2a:classpath:cards/apikey-agent-card.json?protocolBinding=JSONRPC"
                + "&basePath=/apikey&apiKey=test-secret-key&apiKeyHeader=X-API-Key")
                .setBody(simple("Authenticated: ${body}"));

        // OAuth authenticated consumer
        from("a2a:classpath:cards/oauth-agent-card.json?protocolBinding=JSONRPC&basePath=/oauth&oauthProfile=a2a-test")
                .setBody(simple("OAuth: ${body}"));

        // Extension-aware consumer
        from("a2a:classpath:cards/extension-agent-card.json?protocolBinding=JSONRPC&validateAuth=false&basePath=/ext")
                .setBody(simple("Extension: ${header.X-Extension-Tracking} ${body}"));

        from("direct:list-tasks")
                .setHeader(A2AConstants.OPERATION, constant("TASK_LIST"))
                .toD("a2a:http://localhost:${header.CamelA2APort}"
                        + "?protocolBinding=JSONRPC&dataFormat=POJO");

        from("direct:send-to-push")
                .toD("a2a:http://localhost:${header.CamelA2APort}/push"
                        + "?protocolBinding=JSONRPC&dataFormat=POJO");

        from("direct:send-to-pojo")
                .toD("a2a:http://localhost:${header.CamelA2APort}/pojo"
                        + "?protocolBinding=JSONRPC&dataFormat=POJO");

        from("direct:push-config-create")
                .setHeader(A2AConstants.OPERATION, constant("PUSH_CONFIG_CREATE"))
                .toD("a2a:http://localhost:${header.CamelA2APort}/push"
                        + "?protocolBinding=JSONRPC&dataFormat=POJO");

        from("direct:push-config-get")
                .setHeader(A2AConstants.OPERATION, constant("PUSH_CONFIG_GET"))
                .toD("a2a:http://localhost:${header.CamelA2APort}/push"
                        + "?protocolBinding=JSONRPC&dataFormat=POJO");

        from("direct:push-config-list")
                .setHeader(A2AConstants.OPERATION, constant("PUSH_CONFIG_LIST"))
                .toD("a2a:http://localhost:${header.CamelA2APort}/push"
                        + "?protocolBinding=JSONRPC&dataFormat=POJO");

        from("direct:push-config-delete")
                .setHeader(A2AConstants.OPERATION, constant("PUSH_CONFIG_DELETE"))
                .toD("a2a:http://localhost:${header.CamelA2APort}/push"
                        + "?protocolBinding=JSONRPC&dataFormat=POJO");

        from("direct:send-message-stream")
                .setHeader(A2AConstants.OPERATION, constant("MESSAGE_STREAM"))
                .toD("a2a:http://localhost:${header.CamelA2APort}/streaming"
                        + "?protocolBinding=JSONRPC&dataFormat=POJO");
    }
}
