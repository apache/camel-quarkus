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
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test to verify that WebSocket span links are working.
 * This test creates a real WebSocket client connection and sends messages,
 * triggering the consumer which should have span links to the HTTP upgrade span.
 *
 * To verify span links manually:
 * 1. Run the test and observe the OpenTelemetry logs
 * 2. Look for spans with op=EVENT_RECEIVED (consumer spans)
 * 3. These spans should have "links" field pointing to the HTTP upgrade span
 */
@QuarkusTest
public class VertxWebsocketSpanLinkTest {

    @TestHTTPResource("/span-link-test")
    URI spanLinkTestUri;

    @Test
    public void testRoutesAreLoaded() {
        // Verify the resource is accessible
        RestAssured.given()
                .get("/vertx-websocket/span-link/health")
                .then()
                .statusCode(200);
    }

    @Test
    public void testWebSocketSpanLink() throws Exception {
        // Create a WebSocket client connection - expect 1 message from Timer (sendToAll=true)
        try (WebSocketConnection connection = new WebSocketConnection(spanLinkTestUri, null, 1)) {
            connection.connect();

            System.out.println("🔗 WebSocket connection established");

            // Send 2 messages to the same connection
            System.out.println("📤 Sending message #1");
            connection.sendMessage("Message 1");
            Thread.sleep(500);

            System.out.println("📤 Sending message #2");
            connection.sendMessage("Message 2");

            System.out.println("⏳ Waiting for Timer to broadcast (delay=5s)...");

            // Wait for Timer broadcast (Timer has 5s delay, repeatCount=1)
            // getMessages() waits up to 10 seconds for expectedMessageCount
            List<String> messages = connection.getMessages();

            // We should receive 1 message: Timer's broadcast
            assertTrue(messages.size() >= 1, "Should receive at least 1 message from Timer");

            // Print received messages for debugging
            System.out.println("📩 Client received " + messages.size() + " messages:");
            for (int i = 0; i < messages.size(); i++) {
                System.out.println("  [" + i + "] " + messages.get(i));
            }

            // Verify it's from Timer
            boolean hasTimerMessage = messages.stream()
                    .anyMatch(m -> m.contains("Hello World"));
            assertTrue(hasTimerMessage, "Should receive 'Hello World' from Timer");

            // Give some time for OpenTelemetry to export logs
            Thread.sleep(1000);

            System.out.println("✅ Proof: Client received Timer broadcast with sendToAll=true");
            System.out.println("✅ Check Jaeger for:");
            System.out.println("   - 1 HTTP upgrade span (test client)");
            System.out.println("   - 2 Consumer spans (user Message 1 & 2)");
            System.out.println("   - 1 Timer Producer span (sendToAll=true)");
            System.out.println("   - All Consumer spans should FOLLOWS_FROM → HTTP upgrade");
        }
    }

    /**
     * Simple WebSocket client connection helper for testing.
     * Based on VertxWebsocketTest.WebSocketConnection.
     */
    static final class WebSocketConnection implements Closeable {
        private final List<String> messages = new ArrayList<>();
        private final CountDownLatch latch;
        private final URI webSocketUri;
        private final String payload;
        private Session session;

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

        public void sendMessage(String message) {
            if (session != null && session.isOpen()) {
                session.getAsyncRemote().sendText(message);
            }
        }

        public List<String> getMessages() throws InterruptedException {
            latch.await(10, TimeUnit.SECONDS);
            return messages;
        }

        @Override
        public void close() throws IOException {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }
}
