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
package org.apache.camel.quarkus.component.mcp.server.nosecurity.it;

import java.net.URI;
import java.util.Map;
import java.util.stream.Collectors;

import io.quarkiverse.mcp.server.TextContent;
import io.quarkiverse.mcp.server.ToolResponse;
import io.quarkiverse.mcp.server.test.McpAssured;
import io.quarkiverse.mcp.server.test.McpAssured.McpStreamableTestClient;
import io.quarkus.test.junit.QuarkusTest;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class McpServerNoSecurityTest {

    private McpStreamableTestClient client;

    @AfterEach
    void closeClient() {
        if (client != null) {
            client.disconnect();
            client = null;
        }
    }

    @Test
    void anonymousMcpToolWorksWithoutQuarkusSecurityExtension() {
        int port = ConfigProvider.getConfig().getValue("quarkus.http.test-port", Integer.class);
        client = McpAssured.newStreamableClient()
                .setBaseUri(URI.create("http://localhost:%d".formatted(port)))
                .build()
                .connect();

        client.when()
                .toolsCall("whoami", Map.of(), response -> assertThat(textOf(response)).isEqualTo("anonymous"))
                .thenAssertResults();
    }

    private static String textOf(ToolResponse response) {
        return response.content().stream()
                .filter(TextContent.class::isInstance)
                .map(content -> ((TextContent) content).text())
                .collect(Collectors.joining());
    }
}
