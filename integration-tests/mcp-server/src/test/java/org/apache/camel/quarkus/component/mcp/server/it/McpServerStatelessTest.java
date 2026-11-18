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

import java.net.URI;
import java.util.Map;
import java.util.stream.Collectors;

import io.quarkiverse.mcp.server.TextContent;
import io.quarkiverse.mcp.server.test.McpAssured;
import io.quarkiverse.mcp.server.test.McpAssured.McpStreamableTestClient;
import io.quarkiverse.mcp.server.test.McpAssured.ToolInfo;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;

/**
 * Camel ai-tool routes served to a client of MCP protocol version 2026-07-28 (CAMEL-27145): the quarkus-mcp-server
 * extension serves that stateless protocol next to the protocol versions with sessions, on the same endpoint, so the
 * Camel routes reach both kinds of client without any Camel configuration.
 */
@QuarkusTest
class McpServerStatelessTest {

    private McpStreamableTestClient client;

    private McpStreamableTestClient client() {
        if (client == null) {
            client = McpAssured.newStreamableClient()
                    .setBaseUri(URI.create("http://localhost:%d".formatted(testPort())))
                    .setStateless()
                    .build()
                    .connect();
        }
        return client;
    }

    private static int testPort() {
        return ConfigProvider.getConfig().getValue("quarkus.http.test-port", Integer.class);
    }

    @AfterEach
    void closeClient() {
        if (client != null) {
            client.disconnect();
            client = null;
        }
    }

    @Test
    void testStatelessClientHasNoSession() {
        assertThat(client().mcpSessionId()).isNull();
    }

    @Test
    void testListToolsExposesOnlySelectedTags() {
        client().when()
                .toolsList(page -> assertThat(page.tools().stream().map(ToolInfo::name).toList())
                        .contains("say_hello", "create_order")
                        .doesNotContain("hidden_tool", "other_tool"))
                .thenAssertResults();
    }

    @Test
    void testCallTool() {
        client().when()
                .toolsCall("say_hello", Map.of("name", "Camel"), response -> {
                    assertThat(response.isError()).isFalse();
                    assertThat(response.content().stream()
                            .filter(TextContent.class::isInstance)
                            .map(content -> ((TextContent) content).text())
                            .collect(Collectors.joining())).isEqualTo("Hello Camel");
                })
                .thenAssertResults();
    }

    @Test
    void testRequestWithoutHandshakeCreatesNoSession() {
        // a raw request of protocol version 2026-07-28: no initialize before it, and no session after it
        RestAssured.given()
                .contentType(ContentType.JSON)
                .accept("application/json, text/event-stream")
                .header("MCP-Protocol-Version", "2026-07-28")
                .header("Mcp-Method", "tools/call")
                .header("Mcp-Name", "say_hello")
                .body("""
                        {"jsonrpc":"2.0","id":1,"method":"tools/call","params":{"name":"say_hello",
                        "arguments":{"name":"stateless"},"_meta":{
                        "io.modelcontextprotocol/protocolVersion":"2026-07-28",
                        "io.modelcontextprotocol/clientInfo":{"name":"test","version":"1"},
                        "io.modelcontextprotocol/clientCapabilities":{}}}}""")
                .post("http://localhost:%d/mcp".formatted(testPort()))
                .then()
                .statusCode(200)
                .header("Mcp-Session-Id", (String) null)
                .body(containsString("Hello stateless"));
    }
}
