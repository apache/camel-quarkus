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
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import io.quarkiverse.mcp.server.TextContent;
import io.quarkiverse.mcp.server.ToolResponse;
import io.quarkiverse.mcp.server.test.McpAssured;
import io.quarkiverse.mcp.server.test.McpAssured.McpStreamableTestClient;
import io.quarkiverse.mcp.server.test.McpAssured.ToolInfo;
import io.quarkiverse.mcp.server.test.McpAssured.ToolsPage;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * The engine conformance scenarios (CAMEL-24313) against the quarkus-mcp-server engine, driven by McpAssured over
 * streamable HTTP. Mirrors {@code McpServerConformanceTestSupport} from camel-mcp-server-api, which cannot be reused
 * as-is here because it manages its own CamelContext outside Quarkus.
 */
@QuarkusTest
class McpServerTest {

    private McpStreamableTestClient client;

    private McpStreamableTestClient client() {
        if (client == null) {
            client = McpAssured.newStreamableClient()
                    .setBaseUri(URI.create("http://localhost:%d".formatted(
                            ConfigProvider.getConfig().getValue("quarkus.http.test-port", Integer.class))))
                    .build()
                    .connect();
        }
        return client;
    }

    @AfterEach
    void closeClient() {
        if (client != null) {
            client.disconnect();
            client = null;
        }
    }

    @Test
    void testListToolsExposesOnlySelectedTags() {
        client().when()
                .toolsList(page -> assertThat(toolNames(page))
                        .contains("say_hello", "fail_tool", "slow_tool", "annotated_tool", "unannotated_tool",
                                "create_order")
                        .doesNotContain("hidden_tool", "other_tool"))
                .thenAssertResults();
    }

    @Test
    void testToolAnnotationHintsArePublished() {
        client().when()
                .toolsList(page -> {
                    ToolInfo tool = toolByName(page, "annotated_tool");
                    assertThat(tool.title()).isEqualTo("Annotated tool");
                    assertThat(tool.annotations()).isPresent();
                    assertThat(tool.annotations().orElseThrow().readOnlyHint()).isTrue();
                    assertThat(tool.annotations().orElseThrow().idempotentHint()).isTrue();
                })
                .thenAssertResults();
    }

    /**
     * The negative counterpart of {@link #testToolAnnotationHintsArePublished()}: a tool setting no hint at all
     * publishes no annotations and no title, rather than the MCP defaults.
     */
    @Test
    void testToolWithoutAnnotationHintsPublishesNone() {
        client().when()
                .toolsList(page -> {
                    ToolInfo tool = toolByName(page, "unannotated_tool");
                    assertThat(tool.title()).isNull();
                    assertThat(tool.annotations()).isEmpty();
                })
                .thenAssertResults();
    }

    @Test
    void testArgSchemaIsPublishedAsInputSchema() {
        client().when()
                .toolsList(page -> {
                    ToolInfo tool = toolByName(page, "create_order");
                    assertThat(tool.inputSchema().getJsonObject("properties").getJsonObject("customer")
                            .getString("type")).isEqualTo("object");
                    assertThat(tool.inputSchema().getJsonObject("properties").getJsonObject("items")
                            .getString("type")).isEqualTo("array");
                    assertThat(tool.inputSchema().getJsonArray("required")).contains("customer", "items");
                })
                .thenAssertResults();
    }

    @Test
    void testArgSchemaToolExecutesWithNestedArguments() {
        client().when()
                .toolsCall("create_order",
                        Map.of("customer", Map.of("id", "C-1"),
                                "items", List.of(Map.of("sku", "BOOK", "qty", 2))),
                        response -> {
                            assertThat(response.isError()).isFalse();
                            assertThat(textOf(response)).isEqualTo("order for customer C-1 with 1 item(s)");
                        })
                .thenAssertResults();
    }

    @Test
    void testQuarkusAnnotatedToolsCoexistWithCamelTools() {
        // both tool sources are served by the same MCP server
        client().when()
                .toolsList(page -> assertThat(toolNames(page)).contains("add_numbers", "say_hello"))
                .toolsCall("add_numbers", Map.of("a", 17, "b", 25), response -> {
                    assertThat(response.isError()).isFalse();
                    assertThat(textOf(response)).isEqualTo("42");
                })
                .thenAssertResults();
    }

    /**
     * MCP has a flat tool namespace: when an ai-tool route asks for a name an annotated tool already owns, the
     * annotated tool keeps the name and the route is refused. The route is started with the application, which
     * is the case that used to fail the startup.
     */
    @Test
    void testCamelToolCollidingWithAnnotatedToolIsRefused() {
        assertThat(routeStatus("annotated-clash-route")).isEqualTo("Started");
        client().when()
                .toolsCall("clashing_tool", response -> {
                    assertThat(response.isError()).isFalse();
                    // the answer tells which of the two implementations served the call
                    assertThat(textOf(response))
                            .isEqualTo(QuarkusDefinedTools.CLASHING_TOOL_ANSWER);
                })
                .thenAssertResults();

        controlRoute("annotated-clash-route", "stop");
        assertThat(routeStatus("annotated-clash-route")).isEqualTo("Stopped");

        client().when()
                .toolsCall("clashing_tool", response -> {
                    assertThat(response.isError()).isFalse();
                    // the answer tells which of the two implementations served the call
                    assertThat(textOf(response))
                            .isEqualTo(QuarkusDefinedTools.CLASHING_TOOL_ANSWER);
                })
                .thenAssertResults();

        controlRoute("annotated-clash-route", "start");
        assertThat(routeStatus("annotated-clash-route")).isEqualTo("Started");

        client().when()
                .toolsCall("clashing_tool", response -> {
                    assertThat(response.isError()).isFalse();
                    // the answer tells which of the two implementations served the call
                    assertThat(textOf(response))
                            .isEqualTo(QuarkusDefinedTools.CLASHING_TOOL_ANSWER);
                })
                .thenAssertResults();
    }

    @Test
    void testCallToolSuccess() {
        client().when()
                .toolsCall("say_hello", Map.of("name", "World"), response -> {
                    assertThat(response.isError()).isFalse();
                    assertThat(textOf(response)).isEqualTo("Hello World");
                })
                .thenAssertResults();
    }

    /**
     * An argument error is not sanitized the way a route failure is: the client gets the validation message.
     */
    @Test
    void testCallToolMissingRequiredArgument() {
        client().when()
                .toolsCall("missing_required_arg_tool", response -> {
                    assertThat(response.isError()).isTrue();
                    // The error message is expected to come from camel when validating missing required args for an ai-tool route
                    assertThat(textOf(response))
                            .isEqualTo("Missing required argument 'name' for tool 'missing_required_arg_tool'");
                })
                .thenAssertResults();
    }

    @Test
    void testCallToolExecutionErrorIsSanitized() {
        client().when()
                .toolsCall("fail_tool", response -> {
                    assertThat(response.isError()).isTrue();
                    assertThat(textOf(response))
                            .doesNotContain("secret internal detail")
                            .isEqualTo("Tool execution failed");
                })
                .thenAssertResults();
    }

    @Test
    void testCallToolTimeout() {
        client().when()
                .toolsCall("slow_tool", response -> {
                    assertThat(response.isError()).isTrue();
                    assertThat(textOf(response)).contains("timed out");
                })
                .thenAssertResults();
    }

    @Test
    void testToolsListReflectsRouteStopAndStart() {
        client().when()
                .toolsList(page -> assertThat(toolNames(page)).contains("say_hello"))
                .thenAssertResults();

        controlRoute("say-hello-route", "stop");
        assertThat(routeStatus("say-hello-route")).isEqualTo("Stopped");
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> client().when()
                .toolsList(page -> assertThat(toolNames(page)).doesNotContain("say_hello"))
                .thenAssertResults());

        controlRoute("say-hello-route", "start");
        assertThat(routeStatus("say-hello-route")).isEqualTo("Started");
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> client().when()
                .toolsList(page -> assertThat(toolNames(page)).contains("say_hello"))
                .thenAssertResults());
    }

    private static void controlRoute(String routeId, String action) {
        RestAssured.given()
                .when()
                .post("/mcp-server/route/" + routeId + "/" + action)
                .then()
                .statusCode(204);
    }

    private static String routeStatus(String routeId) {
        return RestAssured.given()
                .when()
                .get("/mcp-server/route/" + routeId + "/status")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .asString();
    }

    private static List<String> toolNames(ToolsPage page) {
        return page.tools().stream().map(ToolInfo::name).toList();
    }

    private static ToolInfo toolByName(ToolsPage page, String name) {
        return page.tools().stream()
                .filter(t -> name.equals(t.name()))
                .findFirst()
                .orElseThrow();
    }

    private static String textOf(ToolResponse response) {
        return response.content().stream()
                .filter(TextContent.class::isInstance)
                .map(content -> ((TextContent) content).text())
                .collect(Collectors.joining());
    }
}
