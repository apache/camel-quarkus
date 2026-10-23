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
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import io.quarkiverse.mcp.server.BlobResourceContents;
import io.quarkiverse.mcp.server.JsonRpcErrorCodes;
import io.quarkiverse.mcp.server.TextContent;
import io.quarkiverse.mcp.server.TextResourceContents;
import io.quarkiverse.mcp.server.ToolResponse;
import io.quarkiverse.mcp.server.test.McpAssured;
import io.quarkiverse.mcp.server.test.McpAssured.McpStreamableTestClient;
import io.quarkiverse.mcp.server.test.McpAssured.ResourceInfo;
import io.quarkiverse.mcp.server.test.McpAssured.ResourcesPage;
import io.quarkiverse.mcp.server.test.McpAssured.ServerCapability;
import io.quarkiverse.mcp.server.test.McpAssured.ToolInfo;
import io.quarkiverse.mcp.server.test.McpAssured.ToolsPage;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
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
    private McpStreamableTestClient otherClient;

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

    // a client of the other MCP server configured in application.properties
    private McpStreamableTestClient otherClient() {
        if (otherClient == null) {
            otherClient = McpAssured.newStreamableClient()
                    .setBaseUri(URI.create("http://localhost:%d".formatted(
                            ConfigProvider.getConfig().getValue("quarkus.http.test-port", Integer.class))))
                    .setMcpPath("/other-mcp")
                    .build()
                    .connect();
        }
        return otherClient;
    }

    @AfterEach
    void closeClient() {
        if (client != null) {
            client.disconnect();
            client = null;
        }
        if (otherClient != null) {
            otherClient.disconnect();
            otherClient = null;
        }
    }

    @Test
    void testListToolsExposesOnlySelectedTags() {
        client().when()
                .toolsList(page -> assertThat(toolNames(page))
                        .contains("say_hello", "fail_tool", "slow_tool")
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
    void testOutputSchemaIsPublished() {
        client().when()
                .toolsList(page -> {
                    // outputSchema resource, published as-is with its nested structure
                    JsonObject orderStatus = toolByName(page, "order_status").outputSchema();
                    assertThat(orderStatus).isNotNull();
                    assertThat(orderStatus.getJsonObject("properties").getJsonObject("items")
                            .getJsonObject("items").getString("type")).isEqualTo("string");
                    assertThat(orderStatus.getJsonArray("required")).containsExactly("orderId", "status", "items");

                    // flat outputParameter.* options, published as the generated schema
                    JsonObject weather = toolByName(page, "get_weather").outputSchema();
                    assertThat(weather).isNotNull();
                    assertThat(weather.getJsonObject("properties").getJsonObject("temperature")
                            .getString("type")).isEqualTo("number");
                    assertThat(weather.getJsonObject("properties").getJsonObject("unit").getJsonArray("enum"))
                            .containsExactly("celsius", "fahrenheit");
                    assertThat(weather.getJsonArray("required")).containsExactly("temperature");

                    // tools without an output declaration are unchanged
                    assertThat(toolByName(page, "say_hello").outputSchema()).isNull();
                })
                .thenAssertResults();
    }

    @Test
    void testStructuredContentIsReturned() {
        client().when()
                .toolsCall("order_status", response -> {
                    assertThat(response.isError()).isFalse();
                    assertThat(response.structuredContent()).isEqualTo(new JsonObject()
                            .put("orderId", "O-1")
                            .put("status", "shipped")
                            .put("items", new JsonArray().add("BOOK").add("PEN")));
                    // the JSON text is still returned for clients without structured output support
                    assertThat(textOf(response))
                            .isEqualTo("{\"orderId\":\"O-1\",\"status\":\"shipped\",\"items\":[\"BOOK\",\"PEN\"]}");
                })
                .toolsCall("get_weather", response -> {
                    assertThat(response.isError()).isFalse();
                    assertThat(response.structuredContent())
                            .isEqualTo(new JsonObject().put("temperature", 21.5).put("unit", "celsius"));
                    assertThat(textOf(response)).isEqualTo("{\"temperature\":21.5,\"unit\":\"celsius\"}");
                })
                .thenAssertResults();
    }

    @Test
    void testQuarkusAnnotatedToolsCoexistWithCamelTools() {
        // both tool sources are served by the same MCP server; the add_numbers ai-tool route is refused
        client().when()
                .toolsList(page -> assertThat(toolNames(page)).contains("add_numbers", "say_hello"))
                .toolsCall("add_numbers", Map.of("a", 17, "b", 25), response -> {
                    assertThat(response.isError()).isNotEqualTo(Boolean.TRUE);
                    assertThat(textOf(response)).isEqualTo("42");
                })
                .thenAssertResults();
    }

    @Test
    void testCallToolSuccess() {
        client().when()
                .toolsCall("say_hello", Map.of("name", "World"), response -> {
                    assertThat(response.isError()).isNotEqualTo(Boolean.TRUE);
                    assertThat(textOf(response)).isEqualTo("Hello World");
                    assertThat(response.structuredContent()).isNull();
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
    void testStoppingRefusedToolKeepsApplicationTool() {
        // the route is refused, its name is taken by a tool the application registered itself
        controlRoute("clashing-tool-route", "start");
        assertThat(routeStatus("clashing-tool-route")).isEqualTo("Started");
        controlRoute("clashing-tool-route", "stop");
        assertThat(routeStatus("clashing-tool-route")).isEqualTo("Stopped");

        client().when()
                .toolsCall("user_tool", response -> assertThat(textOf(response)).isEqualTo("user"))
                .thenAssertResults();
    }

    @Test
    void testStoppingToolRouteKeepsToolOfOtherServer() {
        // the application registered a tool with the same name on another MCP server
        controlRoute("say-hello-route", "stop");
        try {
            otherClient().when()
                    .toolsCall("say_hello", response -> assertThat(textOf(response)).isEqualTo("other"))
                    .thenAssertResults();
        } finally {
            controlRoute("say-hello-route", "start");
        }
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

    // The resource scenarios below mirror McpServerResourceConformanceTestSupport from camel-mcp-server-api

    @Test
    void testServerAdvertisesResourcesCapability() {
        assertThat(client().initResult().capabilities()).extracting(ServerCapability::name).contains("resources");
    }

    @Test
    void testServerAdvertisesListChangedForToolsAndResources() {
        // both lists follow the route lifecycle
        assertThat(client().initResult().capabilities())
                .filteredOn(capability -> List.of("tools", "resources").contains(capability.name()))
                .hasSize(2)
                .allSatisfy(capability -> assertThat(capability.properties()).containsEntry("listChanged", true));
    }

    @Test
    void testListResourcesExposesOnlySelectedTags() {
        client().when()
                .resourcesList(page -> {
                    assertThat(resourceUris(page))
                            .contains("camel:///config/app.json", "camel:///reports/latest.pdf", "camel:///fail",
                                    "camel:///slow")
                            .doesNotContain("camel:///hidden", "camel:///other");

                    ResourceInfo config = page.findByUri("camel:///config/app.json");
                    assertThat(config.name()).isEqualTo("app_config");
                    assertThat(config.title()).isEqualTo("App config");
                    assertThat(config.description()).isEqualTo("Application configuration");
                    assertThat(config.mimeType()).isEqualTo("application/json");
                })
                .thenAssertResults();
    }

    @Test
    void testReadTextResource() {
        client().when()
                .resourcesRead("camel:///config/app.json", response -> {
                    assertThat(response.contents()).hasSize(1);
                    TextResourceContents contents = response.contents().get(0).asText();
                    assertThat(contents.uri()).isEqualTo("camel:///config/app.json");
                    assertThat(contents.mimeType()).isEqualTo("application/json");
                    assertThat(contents.text()).isEqualTo("{\"env\":\"test\"}");
                })
                .thenAssertResults();
    }

    @Test
    void testReadBinaryResource() {
        client().when()
                .resourcesRead("camel:///reports/latest.pdf", response -> {
                    assertThat(response.contents()).hasSize(1);
                    BlobResourceContents contents = response.contents().get(0).asBlob();
                    assertThat(contents.mimeType()).isEqualTo("application/pdf");
                    // the basic decoder rejects the line breaks of a MIME encoder
                    assertThat(Base64.getDecoder().decode(contents.blob())).isEqualTo(McpServerRoutes.REPORT);
                })
                .thenAssertResults();
    }

    @Test
    void testReadFileResourceOnEveryRead() {
        writeNotes("first version");
        client().when()
                .resourcesRead("camel:///notes.txt", response -> {
                    TextResourceContents contents = response.contents().get(0).asText();
                    assertThat(contents.mimeType()).isEqualTo("text/plain");
                    assertThat(contents.text()).isEqualTo("first version");
                })
                .thenAssertResults();

        // the route reads the file again, nothing is cached
        writeNotes("second version");
        client().when()
                .resourcesRead("camel:///notes.txt",
                        response -> assertThat(response.contents().get(0).asText().text()).isEqualTo("second version"))
                .thenAssertResults();
    }

    @Test
    void testReadJsonResourceMarshalledByJackson() {
        client().when()
                .resourcesRead("camel:///orders/latest.json", response -> {
                    TextResourceContents contents = response.contents().get(0).asText();
                    assertThat(contents.mimeType()).isEqualTo("application/json");
                    JsonObject order = new JsonObject(contents.text());
                    assertThat(order.getString("id")).isEqualTo("A-1");
                    assertThat(order.getInteger("quantity")).isEqualTo(2);
                })
                .thenAssertResults();
    }

    @Test
    void testReadResourceErrorIsSanitized() {
        client().when()
                .resourcesRead("camel:///fail")
                .withErrorAssert(error -> {
                    assertThat(error.code()).isEqualTo(JsonRpcErrorCodes.INTERNAL_ERROR);
                    assertThat(error.message())
                            .doesNotContain("secret internal detail")
                            .isEqualTo("Resource read failed");
                })
                .send()
                .thenAssertResults();
    }

    @Test
    void testReadResourceTimeout() {
        client().when()
                .resourcesRead("camel:///slow")
                .withErrorAssert(error -> assertThat(error.message()).contains("timed out"))
                .send()
                .thenAssertResults();
    }

    @Test
    void testReadResourceWithinResourceTimeout() {
        // exceeds tool-timeout, so it times out if the two options are mixed up
        client().when()
                .resourcesRead("camel:///delayed",
                        response -> assertThat(response.contents().get(0).asText().text()).isEqualTo("delayed"))
                .thenAssertResults();
    }

    @Test
    void testResourceWithDuplicateNameIsRefused() {
        // quarkus-mcp-server requires unique resource names; the application must still start
        client().when()
                .resourcesList(page -> assertThat(resourceUris(page))
                        .contains("camel:///same/first")
                        .doesNotContain("camel:///same/second"))
                .thenAssertResults();
    }

    @Test
    void testStoppingRefusedResourceKeepsApplicationResource() {
        // the route is refused, its uri is taken by a resource the application registered itself
        controlRoute("clashing-resource-route", "start");
        assertThat(routeStatus("clashing-resource-route")).isEqualTo("Started");
        controlRoute("clashing-resource-route", "stop");
        assertThat(routeStatus("clashing-resource-route")).isEqualTo("Stopped");

        client().when()
                .resourcesList(page -> assertThat(resourceUris(page)).contains("camel:///user/clash"))
                .resourcesRead("camel:///user/clash",
                        response -> assertThat(response.contents().get(0).asText().text()).isEqualTo("user"))
                .thenAssertResults();
    }

    @Test
    void testStoppingResourceRouteKeepsResourceOfOtherServer() {
        // the application registered a resource at the same uri on another MCP server
        controlRoute("app-config-route", "stop");
        try {
            otherClient().when()
                    .resourcesRead("camel:///config/app.json",
                            response -> assertThat(response.contents().get(0).asText().text()).isEqualTo("other"))
                    .thenAssertResults();
        } finally {
            controlRoute("app-config-route", "start");
        }
    }

    @Test
    void testResourcesListReflectsRouteStopAndStart() {
        client().when()
                .resourcesList(page -> assertThat(resourceUris(page)).contains("camel:///config/app.json"))
                .thenAssertResults();

        controlRoute("app-config-route", "stop");
        assertThat(routeStatus("app-config-route")).isEqualTo("Stopped");
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> client().when()
                .resourcesList(page -> assertThat(resourceUris(page)).doesNotContain("camel:///config/app.json"))
                .thenAssertResults());

        controlRoute("app-config-route", "start");
        assertThat(routeStatus("app-config-route")).isEqualTo("Started");
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> client().when()
                .resourcesList(page -> assertThat(resourceUris(page)).contains("camel:///config/app.json"))
                .thenAssertResults());
    }

    private static void controlRoute(String routeId, String action) {
        RestAssured.given()
                .when()
                .post("/mcp-server/route/" + routeId + "/" + action)
                .then()
                .statusCode(204);
    }

    private static void writeNotes(String content) {
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(content)
                .put("/mcp-server/notes")
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

    private static List<String> resourceUris(ResourcesPage page) {
        return page.resources().stream().map(ResourceInfo::uri).toList();
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
