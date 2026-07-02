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

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.apache.camel.component.a2a.A2AConstants;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@QuarkusTestResource(A2aKeycloakTestResource.class)
class A2aTest {

    @Test
    void agentCardServed() {
        RestAssured.get("/.well-known/agent-card.json")
                .then()
                .statusCode(200)
                .body(
                        "name", is("Test Agent"),
                        "version", is("1.0.0"),
                        "description", is("A Quarkus test agent"));
    }

    @Test
    void sendMessageJsonRpc() {
        RestAssured.given()
                .contentType("application/json")
                .body(jsonRpcSendMessage("msg-1", "req-1", "{\"text\":\"Hello A2A\"}"))
                .post("/")
                .then()
                .statusCode(200)
                .body(
                        "jsonrpc", is("2.0"),
                        "result.task.id", notNullValue(),
                        "result.task.contextId", notNullValue(),
                        "id", is("req-1"));
    }

    @Test
    void producerSendMessagePojo() {
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body("Hello from producer")
                .post("/a2a/send")
                .then()
                .statusCode(200)
                .body(
                        "taskId", notNullValue(),
                        "contextId", notNullValue(),
                        "state", is("COMPLETED"));
    }

    @Test
    void producerSendMessagePayloadDataFormat() {
        String response = RestAssured.given()
                .contentType(ContentType.TEXT)
                .body("Hello payload")
                .post("/a2a/send-payload")
                .then()
                .statusCode(200)
                .extract().asString();

        assertTrue(
                response.contains("Echo:"),
                "Expected PAYLOAD response to contain echoed text, got: " + response);
    }

    @Test
    void producerSendMessageRawDataFormat() {
        String response = RestAssured.given()
                .contentType(ContentType.TEXT)
                .body("Hello raw")
                .post("/a2a/send-raw")
                .then()
                .statusCode(200)
                .extract().asString();

        assertTrue(
                response.contains("\"task\"") || response.contains("\"result\""),
                "Expected RAW response to contain JSON structure, got: " + response);
    }

    @Test
    void producerGetTask() {
        String taskId = RestAssured.given()
                .contentType(ContentType.TEXT)
                .body("Create task for get")
                .post("/a2a/send")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getString("taskId");

        RestAssured.given()
                .queryParam("taskId", taskId)
                .get("/a2a/get-task")
                .then()
                .statusCode(200)
                .body(
                        "taskId", is(taskId),
                        "state", is("COMPLETED"));
    }

    @Test
    void cancelCompletedTaskReturnsError() {
        // The echo agent completes tasks immediately, so the task is already in COMPLETED state
        // when we attempt to cancel it — the A2A protocol rejects canceling a completed task
        String taskId = RestAssured.given()
                .contentType(ContentType.TEXT)
                .body("Create task for cancel")
                .post("/a2a/send")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getString("taskId");

        RestAssured.given()
                .queryParam("taskId", taskId)
                .post("/a2a/cancel-task")
                .then()
                .statusCode(200)
                .body("error", notNullValue());
    }

    @Test
    void sendStreamingMessageJsonRpc() {
        String response = RestAssured.given()
                .contentType("application/json")
                .body(jsonRpcStreamMessage("msg-stream", "req-stream", "Stream this"))
                .post("/streaming/")
                .then()
                .statusCode(200)
                .header("Content-Type", "text/event-stream")
                .extract().asString();

        String[] events = response.split("\n\n");
        assertTrue(events.length >= 3,
                "Expected at least 3 SSE events (submitted + 2 progress + completed), got "
                        + events.length + ": " + response);

        for (String event : events) {
            assertTrue(event.startsWith("data: "),
                    "SSE event should start with 'data: ', got: " + event);
        }

        assertTrue(response.contains("Step 1 complete"),
                "Expected progress event 'Step 1 complete' in response: " + response);
        assertTrue(response.contains("Step 2 complete"),
                "Expected progress event 'Step 2 complete' in response: " + response);
    }

    @Test
    void restProtocolBindingRejected() {
        RestAssured.get("/a2a/create-rest-endpoint")
                .then()
                .statusCode(200)
                .body(containsString("REST (HTTP+JSON) protocol binding is not supported on Quarkus"));
    }

    @Test
    void sendMessageWithDataPart() {
        RestAssured.given()
                .contentType("application/json")
                .body(jsonRpcSendMessage("msg-data", "req-data",
                        "{\"kind\":\"data\",\"data\":{\"key\":\"value\",\"count\":42}}"))
                .post("/")
                .then()
                .statusCode(200)
                .body(
                        "jsonrpc", is("2.0"),
                        "result.task.id", notNullValue(),
                        "result.task.status.state", is("TASK_STATE_COMPLETED"),
                        "id", is("req-data"));
    }

    @Test
    void sendMessageWithFilePart() {
        RestAssured.given()
                .contentType("application/json")
                .body(jsonRpcSendMessage("msg-file", "req-file",
                        "{\"kind\":\"file\",\"raw\":\"aGVsbG8=\",\"mediaType\":\"text/plain\",\"filename\":\"test.txt\"}"))
                .post("/")
                .then()
                .statusCode(200)
                .body(
                        "jsonrpc", is("2.0"),
                        "result.task.id", notNullValue(),
                        "result.task.status.state", is("TASK_STATE_COMPLETED"),
                        "id", is("req-file"));
    }

    @Test
    void fullAgentCardServed() {
        RestAssured.get("/full/.well-known/agent-card.json")
                .then()
                .statusCode(200)
                .body(
                        "name", is("Full Feature Agent"),
                        "version", is("2.0.0"),
                        "description", is("A Quarkus test agent with all features"),
                        "provider.name", is("Test Provider"),
                        "provider.url", is("https://example.com/provider"),
                        "capabilities.streaming", is(true),
                        "capabilities.pushNotifications", is(true),
                        "skills[0].id", is("echo"),
                        "skills[0].name", is("Echo Skill"),
                        "skills[0].tags[0]", is("test"),
                        "skills[0].inputModes[0]", is("text/plain"),
                        "skills[1].id", is("transform"),
                        "defaultInputModes[0]", is("text/plain"),
                        "defaultOutputModes[0]", is("text/plain"));
    }

    @Test
    void producerListTasks() {
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body("Task for list 1")
                .post("/a2a/send")
                .then()
                .statusCode(200);
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body("Task for list 2")
                .post("/a2a/send")
                .then()
                .statusCode(200);

        RestAssured.get("/a2a/list-tasks")
                .then()
                .statusCode(200)
                .body("count", greaterThanOrEqualTo(2));
    }

    @Test
    void multiTurnConversation() {
        String contextId = RestAssured.given()
                .contentType(ContentType.TEXT)
                .body("Turn 1")
                .post("/a2a/send-context")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getString("contextId");
        assertNotNull(contextId, "First turn should return a contextId");

        String contextId2 = RestAssured.given()
                .contentType(ContentType.TEXT)
                .body("Turn 2")
                .queryParam("contextId", contextId)
                .post("/a2a/send-context")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getString("contextId");

        assertEquals(contextId, contextId2, "contextId should be preserved across turns");
    }

    @Test
    void pushNotificationConfigCrud() {
        String taskId = RestAssured.given()
                .contentType("application/json")
                .body(jsonRpcSendMessage("msg-push", "req-push", "{\"text\":\"Push test\"}"))
                .post("/push/")
                .then()
                .statusCode(200)
                .body("result.task.id", notNullValue())
                .extract()
                .jsonPath()
                .getString("result.task.id");
        assertNotNull(taskId, "Task ID should be returned");

        String configId = RestAssured.given()
                .contentType("application/json")
                .body(jsonRpcRequest("CreateTaskPushNotificationConfig", "req-pc-create",
                        "\"taskId\":\"" + taskId + "\",\"url\":\"http://localhost:9999/webhook\""))
                .post("/push/")
                .then()
                .statusCode(200)
                .body("result.url", is("http://localhost:9999/webhook"))
                .extract()
                .jsonPath()
                .getString("result.id");
        assertNotNull(configId, "Push config ID should be returned");

        String listRequest = jsonRpcRequest("ListTaskPushNotificationConfigs", "req-pc-list",
                "\"taskId\":\"" + taskId + "\"");

        RestAssured.given()
                .contentType("application/json")
                .body(listRequest)
                .post("/push/")
                .then()
                .statusCode(200)
                .body("result.configs.size()", is(1));

        RestAssured.given()
                .contentType("application/json")
                .body(jsonRpcRequest("GetTaskPushNotificationConfig", "req-pc-get",
                        "\"taskId\":\"" + taskId + "\",\"id\":\"" + configId + "\""))
                .post("/push/")
                .then()
                .statusCode(200)
                .body("result.url", is("http://localhost:9999/webhook"));

        RestAssured.given()
                .contentType("application/json")
                .body(jsonRpcRequest("DeleteTaskPushNotificationConfig", "req-pc-delete",
                        "\"taskId\":\"" + taskId + "\",\"id\":\"" + configId + "\""))
                .post("/push/")
                .then()
                .statusCode(200);

        RestAssured.given()
                .contentType("application/json")
                .body(listRequest)
                .post("/push/")
                .then()
                .statusCode(200)
                .body("result.configs.size()", is(0));
    }

    @Test
    void streamingWithArtifactEmission() {
        String response = RestAssured.given()
                .contentType("application/json")
                .body(jsonRpcStreamMessage("msg-art", "req-art", "Stream with artifact"))
                .post("/streaming/")
                .then()
                .statusCode(200)
                .header("Content-Type", "text/event-stream")
                .extract().asString();

        assertTrue(response.contains("artifactUpdate"),
                "Expected artifact update event in SSE response: " + response);
        assertTrue(response.contains("test-artifact"),
                "Expected artifact name 'test-artifact' in SSE response: " + response);
    }

    @Test
    void producerStreamMessage() {
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body("Stream from producer")
                .post("/a2a/send-stream")
                .then()
                .statusCode(200)
                .body(
                        "statusUpdates", greaterThanOrEqualTo(2),
                        "artifactUpdates", greaterThanOrEqualTo(1));
    }

    @Test
    void consumerPojoDataFormat() {
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body("Hello POJO")
                .post("/a2a/send-pojo")
                .then()
                .statusCode(200)
                .body(
                        "state", is("COMPLETED"),
                        "response", containsString("role=ROLE_USER"));
    }

    @Test
    void sendMessageAsync() {
        RestAssured.given()
                .contentType("application/json")
                .body("{\"jsonrpc\":\"2.0\",\"method\":\"SendMessage\","
                        + "\"params\":{\"message\":{\"messageId\":\"msg-async\","
                        + "\"role\":\"user\",\"parts\":[{\"text\":\"Async request\"}]},"
                        + "\"configuration\":{\"returnImmediately\":true}},"
                        + "\"id\":\"req-async\"}")
                .post("/")
                .then()
                .statusCode(200)
                .body(
                        "jsonrpc", is("2.0"),
                        "result.task.id", notNullValue(),
                        "result.task.status.state", is("TASK_STATE_SUBMITTED"),
                        "id", is("req-async"));
    }

    @Test
    void cardFromParametersServed() {
        RestAssured.get("/pojo/.well-known/agent-card.json")
                .then()
                .statusCode(200)
                .body(
                        "name", is("POJO Agent"),
                        "description", is("POJO data format test"),
                        "version", is("1.0.0"));
    }

    @Test
    void simpleLanguageEmitFunction() {
        String response = RestAssured.given()
                .contentType("application/json")
                .body(jsonRpcStreamMessage("msg-emit", "req-emit", "Test emit"))
                .post("/emit/")
                .then()
                .statusCode(200)
                .header("Content-Type", "text/event-stream")
                .extract().asString();

        assertTrue(response.contains("Simple emit step 1"),
                "Expected 'Simple emit step 1' in SSE response: " + response);
        assertTrue(response.contains("Simple emit step 2"),
                "Expected 'Simple emit step 2' in SSE response: " + response);
    }

    @Test
    void apiKeyAuthenticationRequired() {
        RestAssured.given()
                .contentType("application/json")
                .body(jsonRpcSendMessage("msg-noauth", "req-noauth", "{\"text\":\"No auth\"}"))
                .post("/apikey/")
                .then()
                .statusCode(200)
                .body("error", notNullValue());
    }

    @Test
    void apiKeyAuthenticationSuccess() {
        RestAssured.given()
                .contentType("application/json")
                .header("X-API-Key", "test-secret-key")
                .body(jsonRpcSendMessage("msg-auth", "req-auth", "{\"text\":\"With auth\"}"))
                .post("/apikey/")
                .then()
                .statusCode(200)
                .body(
                        "result.task.id", notNullValue(),
                        "id", is("req-auth"));
    }

    @Test
    void a2aExtensionNegotiation() {
        RestAssured.given()
                .contentType("application/json")
                .header(A2AConstants.HEADER_A2A_EXTENSIONS, "urn:test:tracking")
                .body(jsonRpcSendMessage("msg-ext", "req-ext", "{\"text\":\"Extension test\"}"))
                .post("/ext/")
                .then()
                .statusCode(200)
                .header(A2AConstants.HEADER_A2A_EXTENSIONS, containsString("urn:test:tracking"))
                .body("result.task.id", notNullValue());
    }

    @Test
    void oauthAuthenticationRequired() {
        RestAssured.given()
                .contentType("application/json")
                .body(jsonRpcSendMessage("msg-notoken", "req-notoken", "{\"text\":\"No token\"}"))
                .post("/oauth/")
                .then()
                .statusCode(200)
                .body("error", notNullValue());
    }

    @Test
    void oauthAuthenticationSuccess() {
        String keycloakUrl = ConfigProvider.getConfig().getValue("cq.a2a.test.keycloak.url", String.class);
        String tokenEndpoint = keycloakUrl + "/protocol/openid-connect/token";

        String accessToken = RestAssured.given()
                .formParam("grant_type", "client_credentials")
                .formParam("client_id", "camel-client")
                .formParam("client_secret", "camel-client-secret")
                .post(tokenEndpoint)
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getString("access_token");
        assertNotNull(accessToken, "Should obtain access token from Keycloak");

        RestAssured.given()
                .contentType("application/json")
                .header("Authorization", "Bearer " + accessToken)
                .body(jsonRpcSendMessage("msg-oauth", "req-oauth", "{\"text\":\"With OAuth\"}"))
                .post("/oauth/")
                .then()
                .statusCode(200)
                .body(
                        "result.task.id", notNullValue(),
                        "id", is("req-oauth"));
    }

    private static String jsonRpcSendMessage(String messageId, String requestId, String partJson) {
        return jsonRpcRequest("SendMessage", requestId,
                "\"message\":{\"messageId\":\"" + messageId + "\","
                        + "\"role\":\"user\",\"parts\":[" + partJson + "]}");
    }

    private static String jsonRpcStreamMessage(String messageId, String requestId, String text) {
        return jsonRpcRequest("SendStreamingMessage", requestId,
                "\"message\":{\"messageId\":\"" + messageId + "\","
                        + "\"role\":\"user\",\"parts\":[{\"text\":\"" + text + "\"}]}");
    }

    private static String jsonRpcRequest(String method, String requestId, String params) {
        return "{\"jsonrpc\":\"2.0\",\"method\":\"" + method + "\","
                + "\"params\":{" + params + "},\"id\":\"" + requestId + "\"}";
    }
}
