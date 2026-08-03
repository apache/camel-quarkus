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
package org.apache.camel.quarkus.component.mcp.server.it;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.apache.camel.CamelContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * The engine conformance scenarios (CAMEL-24313) against the quarkus-mcp-server engine, driven by the official MCP SDK
 * client over streamable HTTP. Mirrors {@code McpServerConformanceTestSupport} from camel-mcp-server-api, which cannot
 * be reused as-is here because it manages its own CamelContext outside Quarkus.
 */
@QuarkusTest
class McpServerTest {

    @Inject
    CamelContext camelContext;

    private McpSyncClient client;

    private McpSyncClient client() {
        if (client == null) {
            client = McpClient
                    .sync(HttpClientStreamableHttpTransport.builder("http://localhost:8081").build())
                    .requestTimeout(Duration.ofSeconds(10))
                    .initializationTimeout(Duration.ofSeconds(10))
                    .build();
            client.initialize();
        }
        return client;
    }

    @AfterEach
    void closeClient() {
        if (client != null) {
            client.closeGracefully();
            client = null;
        }
    }

    @Test
    void testListToolsExposesOnlySelectedTags() {
        List<McpSchema.Tool> tools = client().listTools().tools();

        assertThat(tools).extracting(McpSchema.Tool::name)
                .contains("say_hello", "fail_tool", "slow_tool")
                .doesNotContain("hidden_tool", "other_tool");
    }

    @Test
    void testCallToolSuccess() {
        McpSchema.CallToolResult result = client()
                .callTool(new McpSchema.CallToolRequest("say_hello", Map.of("name", "World")));

        assertThat(result.isError()).isNotEqualTo(Boolean.TRUE);
        assertThat(textOf(result)).isEqualTo("Hello World");
    }

    @Test
    void testCallToolExecutionErrorIsSanitized() {
        McpSchema.CallToolResult result = client().callTool(new McpSchema.CallToolRequest("fail_tool", Map.of()));

        assertThat(result.isError()).isEqualTo(Boolean.TRUE);
        assertThat(textOf(result))
                .doesNotContain("secret internal detail")
                .isEqualTo("Tool execution failed");
    }

    @Test
    void testCallToolTimeout() {
        McpSchema.CallToolResult result = client().callTool(new McpSchema.CallToolRequest("slow_tool", Map.of()));

        assertThat(result.isError()).isEqualTo(Boolean.TRUE);
        assertThat(textOf(result)).contains("timed out");
    }

    @Test
    void testToolsListReflectsRouteStopAndStart() throws Exception {
        assertThat(client().listTools().tools()).extracting(McpSchema.Tool::name).contains("say_hello");

        camelContext.getRouteController().stopRoute("say-hello-route");
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> assertThat(client().listTools().tools())
                .extracting(McpSchema.Tool::name).doesNotContain("say_hello"));

        camelContext.getRouteController().startRoute("say-hello-route");
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> assertThat(client().listTools().tools())
                .extracting(McpSchema.Tool::name).contains("say_hello"));
    }

    private static String textOf(McpSchema.CallToolResult result) {
        return result.content().stream()
                .filter(McpSchema.TextContent.class::isInstance)
                .map(c -> ((McpSchema.TextContent) c).text())
                .collect(Collectors.joining());
    }
}
