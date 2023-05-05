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
package org.apache.camel.quarkus.component.vertx.websocket.it;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import jakarta.websocket.ClientEndpointConfig;
import jakarta.websocket.ContainerProvider;
import jakarta.websocket.Endpoint;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.MessageHandler;
import jakarta.websocket.Session;
import org.apache.camel.component.vertx.websocket.VertxWebsocketConstants;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class VertxWebsocketTest {

    @TestHTTPResource("/")
    URI root;

    @TestHTTPResource("/echo")
    URI echo;

    @TestHTTPResource("/test/default/host/port/applied")
    URI defaultHostPortApplied;

    @TestHTTPResource("/client/consumer")
    URI clientConsumer;

    @TestHTTPResource("/parameterized/path")
    URI parameterizedPath;

    @TestHTTPResource("/query/params")
    URI queryParamsPath;

    @TestHTTPResource("/events")
    URI events;

    @Test
    public void testEchoWithShortFormUri() throws Exception {
        String message = "From Short From URI";

        try (WebSocketConnection connection = new WebSocketConnection(echo, message)) {
            connection.connect();

            List<String> messages = connection.getMessages();
            assertEquals(1, messages.size());
            assertEquals("Hello " + message, messages.get(0));
        }
    }

    @Test
    public void testEchoWithIgnoredHostPortConfig() throws Exception {
        String message = "From Ignored Host Port Config";

        try (WebSocketConnection connection = new WebSocketConnection(defaultHostPortApplied, message)) {
            connection.connect();

            List<String> messages = connection.getMessages();
            assertEquals(1, messages.size());
            assertEquals("Hello " + message, messages.get(0));
        }
    }

    @Test
    public void testRootPath() throws Exception {
        String message = "From Root Path";

        try (WebSocketConnection connection = new WebSocketConnection(root, message)) {
            connection.connect();

            List<String> messages = connection.getMessages();
            assertEquals(1, messages.size());
            assertEquals("Hello " + message, messages.get(0));
        }
    }

    @ParameterizedTest
    @MethodSource("getHosts")
    public void testProduceToUnmanagedPath(String host) {
        try {
            String message = "Message for externally managed path on host " + host;
            RestAssured.given()
                    .queryParam("endpointUri", "direct:produceToExternalEndpoint")
                    .queryParam("camelHeader", "port:" + RestAssured.port)
                    .queryParam("camelHeader", "host:" + host)
                    .body(message)
                    .post("/vertx-websocket/run")
                    .then()
                    .statusCode(204);

            Awaitility.await().pollDelay(Duration.ofMillis(100)).atMost(Duration.ofSeconds(5)).until(() -> {
                return RestAssured.given()
                        .get("/vertx-websocket/messages")
                        .then()
                        .statusCode(200)
                        .extract()
                        .body()
                        .asString()
                        .equals("Received message: " + message);
            });
        } finally {
            RestAssured.given()
                    .delete("/vertx-websocket/messages")
                    .then()
                    .statusCode(204);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = { "", "ws:", "wss:" })
    public void testDefaultPortAssignment(String scheme) {
        int expectedPort;
        if (scheme.startsWith("wss:")) {
            expectedPort = VertxWebsocketConstants.DEFAULT_VERTX_CLIENT_WSS_PORT;
        } else {
            expectedPort = VertxWebsocketConstants.DEFAULT_VERTX_CLIENT_WS_PORT;
        }

        RestAssured.given()
                .body(scheme)
                .get("/vertx-websocket/default/port")
                .then()
                .statusCode(200)
                .body(is(String.valueOf(expectedPort)));
    }

    @Test
    public void sendToAll() throws Exception {
        String message = "Message Broadcast To All Connected Peers";

        try (WebSocketConnection connectionA = new WebSocketConnection(root, null);
                WebSocketConnection connectionB = new WebSocketConnection(root, null)) {

            // Connect some peers to the WebSocket
            connectionA.connect();
            connectionB.connect();

            // Broadcast a message to all connected peers
            RestAssured.given()
                    .queryParam("endpointUri", "vertx-websocket:/?sendToAll=true")
                    .body(message)
                    .post("/vertx-websocket/run")
                    .then()
                    .statusCode(204);

            // Verify both connections got the expected message
            List<String> messagesA = connectionA.getMessages();
            assertEquals(1, messagesA.size());
            assertEquals(message, messagesA.get(0));

            List<String> messagesB = connectionB.getMessages();
            assertEquals(1, messagesB.size());
            assertEquals(message, messagesB.get(0));
        }
    }

    @Test
    public void sendToAllUsingHeader() throws Exception {
        String message = "Message Broadcast To All Connected Peers Using Header";

        try (WebSocketConnection connectionA = new WebSocketConnection(root, null);
                WebSocketConnection connectionB = new WebSocketConnection(root, null)) {

            // Connect some peers to the WebSocket
            connectionA.connect();
            connectionB.connect();

            // Broadcast a message to all connected peers
            RestAssured.given()
                    .queryParam("endpointUri", "vertx-websocket:/")
                    .queryParam("camelHeader", VertxWebsocketConstants.SEND_TO_ALL + ":" + "true")
                    .body(message)
                    .post("/vertx-websocket/run")
                    .then()
                    .statusCode(204);

            // Verify both connections got the expected message
            List<String> messagesA = connectionA.getMessages();
            assertEquals(1, messagesA.size());
            assertEquals(message, messagesA.get(0));

            List<String> messagesB = connectionB.getMessages();
            assertEquals(1, messagesB.size());
            assertEquals(message, messagesB.get(0));
        }
    }

    @Test
    public void sendToSpecificPeer() throws Exception {
        String message = "Message Broadcast To A Specific Peer";

        try (WebSocketConnection connectionA = new WebSocketConnection(root, "ping", 2);
                WebSocketConnection connectionB = new WebSocketConnection(root, null)) {

            // Connect first connection and get the associated connection key
            connectionA.connect();
            List<String> pingResponses = connectionA.getMessages();
            assertEquals(1, pingResponses.size());
            String connectionKey = pingResponses.get(0);

            // Connect second connection
            connectionB.connect();

            // Broadcast a message to the first connected peers
            RestAssured.given()
                    .queryParam("endpointUri", "vertx-websocket:/")
                    .queryParam("camelHeader", VertxWebsocketConstants.CONNECTION_KEY + ":" + connectionKey)
                    .body(message)
                    .post("/vertx-websocket/run")
                    .then()
                    .statusCode(204);

            // Verify only the first connection got the expected message
            List<String> messagesA = connectionA.getMessages();
            assertEquals(2, messagesA.size());
            assertEquals(message, messagesA.get(1));

            List<String> messagesB = connectionB.getMessages(500, TimeUnit.MILLISECONDS);
            assertTrue(messagesB.isEmpty());
        }
    }

    @Test
    public void consumeAsClient() throws Exception {
        String message = "From Consume As Client";

        RestAssured.given()
                .post("/vertx-websocket/manageClientConsumer/enable/true")
                .then()
                .statusCode(204);

        try (WebSocketConnection connection = new WebSocketConnection(clientConsumer, message)) {
            connection.connect();

            RestAssured.given()
                    .get("/vertx-websocket/seda/seda:consumeAsClientResult")
                    .then()
                    .statusCode(200)
                    .body(is("Hello " + message));
        } finally {
            RestAssured.given()
                    .post("/vertx-websocket/manageClientConsumer/enable/false")
                    .then()
                    .statusCode(204);
        }
    }

    @Test
    public void parameterizedPath() throws Exception {
        URI uri = URI.create(parameterizedPath.toString() + "/Hello/World");
        try (WebSocketConnection connection = new WebSocketConnection(uri, "Test")) {
            connection.connect();

            RestAssured.given()
                    .get("/vertx-websocket/seda/seda:parameterizedPathResult")
                    .then()
                    .statusCode(200)
                    .body(is("Hello World"));
        }
    }

    @Test
    public void queryParameters() throws Exception {
        URI uri = URI.create(queryParamsPath.toString() + "?paramA=Hello&paramB=World");
        try (WebSocketConnection connection = new WebSocketConnection(uri, "Test")) {
            connection.connect();

            RestAssured.given()
                    .get("/vertx-websocket/seda/seda:queryParamsResult")
                    .then()
                    .statusCode(200)
                    .body(is("Hello World"));
        }
    }

    @Test
    public void events() throws Exception {
        try (WebSocketConnection connection = new WebSocketConnection(events, "Test")) {
            connection.connect();
            connection.close();

            List<String> eventsReceived = new ArrayList<>();
            List<String> expectedEvents = List.of("OPEN", "MESSAGE", "CLOSE");

            Awaitility.await().pollDelay(Duration.ofMillis(100)).atMost(Duration.ofSeconds(5)).until(() -> {
                String event = RestAssured.given()
                        .get("/vertx-websocket/seda/seda:eventsResult")
                        .then()
                        .extract()
                        .body()
                        .asString();

                eventsReceived.add(event);
                return eventsReceived.containsAll(expectedEvents);
            });
        }
    }

    static String[] getHosts() {
        return new String[] {
                "localhost",
                "0.0.0.0"
        };
    }

    static final class WebSocketConnection implements Closeable {
        private final List<String> messages = new ArrayList<>();
        private final CountDownLatch latch;
        private final URI webSocketUri;
        private final String payload;
        private Session session;

        public WebSocketConnection(URI webSocketUri, String payload) {
            this(webSocketUri, payload, 1);
        }

        public WebSocketConnection(URI webSocketUri, String payload, int expectedMessageCount) {
            this.webSocketUri = webSocketUri;
            this.payload = payload;
            this.latch = new CountDownLatch(expectedMessageCount);
        }

        public void connect() throws Exception {
            Endpoint endpoint = new Endpoint() {
                @Override
                public void onOpen(Session session, EndpointConfig endpointConfig) {
                    session.addMessageHandler(new MessageHandler.Whole<String>() {
                        @Override
                        public void onMessage(String message) {
                            messages.add(message);
                            latch.countDown();
                        }
                    });

                    if (payload != null) {
                        session.getAsyncRemote().sendText(payload);
                    }
                }
            };

            ClientEndpointConfig config = ClientEndpointConfig.Builder.create().build();
            this.session = ContainerProvider.getWebSocketContainer().connectToServer(endpoint, config, webSocketUri);
        }

        public List<String> getMessages() throws InterruptedException {
            return getMessages(5, TimeUnit.SECONDS);
        }

        public List<String> getMessages(long timeout, TimeUnit timeUnit) throws InterruptedException {
            latch.await(timeout, timeUnit);
            return messages;
        }

        @Override
        public void close() throws IOException {
            if (session != null) {
                session.close();
            }
        }
    }
}
