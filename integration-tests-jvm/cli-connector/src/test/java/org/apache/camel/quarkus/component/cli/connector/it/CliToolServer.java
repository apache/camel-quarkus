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
package org.apache.camel.quarkus.component.cli.connector.it;

import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.http.WebSocketFrame;
import org.apache.camel.util.json.JsonObject;
import org.apache.camel.util.json.Jsoner;

import static org.awaitility.Awaitility.await;

/**
 * Plays the developer tool: a WebSocket server the Camel CLI connector of the application dials out to.
 */
public class CliToolServer implements QuarkusTestResourceLifecycleManager {

    static final String TOKEN = "it-s3cret";
    // a query parameter holding an encoded '&': it must reach the tool as given
    static final String REQUEST_URI = "/v1/connect?executionId=it%261";
    private static final int MAX_MESSAGE_SIZE = 32 * 1024 * 1024;

    static final BlockingQueue<JsonObject> FRAMES = new LinkedBlockingQueue<>();
    static final List<ServerWebSocket> SOCKETS = new CopyOnWriteArrayList<>();
    static final List<String> REQUEST_URIS = new CopyOnWriteArrayList<>();
    static final AtomicInteger REJECTED = new AtomicInteger();
    static final AtomicInteger CONNECTIONS = new AtomicInteger();
    static final AtomicInteger UNANSWERED = new AtomicInteger();
    static volatile boolean silent;
    static final AtomicInteger LARGEST_TRACE_SNAPSHOT = new AtomicInteger();
    static volatile boolean reject;

    private Vertx vertx;

    @Override
    public Map<String, String> start() {
        vertx = Vertx.vertx();
        try {
            // frames of 64 KB at most (the Vert.x default), in both directions, as Vert.x and Quarkus servers
            HttpServer server = vertx.createHttpServer(new HttpServerOptions()
                    .setMaxWebSocketMessageSize(MAX_MESSAGE_SIZE))
                    .webSocketHandshakeHandler(handshake -> {
                        REQUEST_URIS.add(handshake.uri());
                        if (silent) {
                            // never answers the upgrade
                            UNANSWERED.incrementAndGet();
                            return;
                        }
                        if (reject || !("Bearer " + TOKEN).equals(handshake.headers().get("Authorization"))) {
                            REJECTED.incrementAndGet();
                            handshake.reject(401);
                        } else {
                            handshake.accept();
                        }
                    })
                    .webSocketHandler(ws -> {
                        SOCKETS.add(ws);
                        CONNECTIONS.incrementAndGet();
                        ws.textMessageHandler(text -> {
                            try {
                                JsonObject frame = (JsonObject) Jsoner.deserialize(text);
                                if ("trace".equals(frame.getString("kind"))) {
                                    LARGEST_TRACE_SNAPSHOT.accumulateAndGet(text.length(), Math::max);
                                }
                                FRAMES.add(frame);
                            } catch (Exception e) {
                                throw new IllegalStateException(e);
                            }
                        });
                        ws.closeHandler(v -> SOCKETS.remove(ws));
                    })
                    .listen(0, "127.0.0.1").toCompletionStage().toCompletableFuture().get(10, TimeUnit.SECONDS);
            return Map.of(
                    "camel.cli.websocket.url", "ws://127.0.0.1:" + server.actualPort() + REQUEST_URI,
                    "camel.cli.websocket.token", TOKEN);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void stop() {
        if (vertx != null) {
            vertx.close();
        }
    }

    /**
     * Closes the connection, and waits for the application to connect again and say hello. The old sockets are not
     * waited for: they are closed once the application answers the close, or after the Vert.x closing timeout.
     */
    static JsonObject reconnect() throws InterruptedException {
        int connections = CONNECTIONS.get();
        FRAMES.clear();
        SOCKETS.forEach(ServerWebSocket::close);
        await().atMost(20, TimeUnit.SECONDS).until(() -> CONNECTIONS.get() > connections);
        return awaitFrame(f -> "hello".equals(f.getString("type")));
    }

    static void send(JsonObject frame) {
        await().atMost(10, TimeUnit.SECONDS).until(() -> !SOCKETS.isEmpty());
        // in frames of 64 KB at most, as Vert.x and Quarkus servers
        SOCKETS.get(SOCKETS.size() - 1).writeTextMessage(frame.toJson());
    }

    static void sendInASingleFrame(JsonObject frame) {
        await().atMost(10, TimeUnit.SECONDS).until(() -> !SOCKETS.isEmpty());
        // as tools that never split messages, whatever the size
        SOCKETS.get(SOCKETS.size() - 1).writeFrame(WebSocketFrame.textFrame(frame.toJson(), true));
    }

    static JsonObject action(String requestId, String name, String... keyValues) {
        JsonObject action = new JsonObject();
        action.put("action", name);
        for (int i = 0; i < keyValues.length; i += 2) {
            action.put(keyValues[i], keyValues[i + 1]);
        }
        return new JsonObject(Map.of("v", 1, "type", "action", "requestId", requestId, "action", action));
    }

    /**
     * Waits for a frame matching the predicate, skipping the others (mostly snapshots).
     */
    static JsonObject awaitFrame(Predicate<JsonObject> predicate) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 20000;
        while (System.currentTimeMillis() < deadline) {
            JsonObject frame = FRAMES.poll(100, TimeUnit.MILLISECONDS);
            if (frame != null && predicate.test(frame)) {
                return frame;
            }
        }
        throw new AssertionError("No matching frame received within 20 seconds");
    }

    static JsonObject awaitResult(String requestId) throws InterruptedException {
        return awaitFrame(f -> "result".equals(f.getString("type")) && requestId.equals(f.getString("requestId")));
    }
}
