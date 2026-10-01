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
package org.apache.camel.quarkus.component.cli.connector;

import java.net.URI;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeoutException;

import io.quarkus.tls.TlsConfiguration;
import io.quarkus.tls.TlsConfigurationRegistry;
import io.quarkus.tls.runtime.config.TlsConfigUtils;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.UpgradeRejectedException;
import io.vertx.core.http.WebSocket;
import io.vertx.core.http.WebSocketClient;
import io.vertx.core.http.WebSocketClientOptions;
import io.vertx.core.http.WebSocketConnectOptions;
import io.vertx.core.http.WebSocketFrame;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import org.apache.camel.cli.connector.CliWebSocketClient;
import org.apache.camel.cli.connector.CliWebSocketHandshakeException;
import org.jboss.logging.Logger;

/**
 * The {@link CliWebSocketClient} of the Camel CLI connector WebSocket transport on Camel Quarkus, with the Vert.x
 * WebSocket client of the application. {@code camel.cli.websocket.client=jdk} uses the JDK client instead.
 * <p/>
 * The handlers run on the Vert.x event loop: they only hand over to the transport, which parses and sends on its own
 * threads.
 */
public class VertxCliWebSocketClient implements CliWebSocketClient {

    static final String NAME = "vertx";
    private static final Logger LOG = Logger.getLogger(VertxCliWebSocketClient.class);
    private static final int TIMEOUT = 10000;
    // the frames sent: servers commonly refuse frames over 64 KB (Vert.x, Quarkus), and a char takes up to 3 bytes
    private static final int FRAME_CHARS = 16 * 1024;
    // the messages received, in bytes: up to 3 per char
    private static final int MAX_MESSAGE_BYTES = 3 * MAX_MESSAGE_SIZE;

    @Inject
    Vertx vertx;

    @Inject
    TlsConfigurationRegistry tlsRegistry;

    @Inject
    CamelCliConnectorRunTimeConfig config;

    private TlsConfiguration tls;

    @PostConstruct
    void resolveTlsConfiguration() {
        Optional<String> name = config.websocket().tlsConfigurationName();
        if (name.isPresent()) {
            tls = tlsRegistry.get(name.get()).orElseThrow(() -> new IllegalStateException(
                    "No TLS configuration named " + name.get() + " (quarkus.camel.cli.websocket.tls-configuration-name)"));
        }
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public CompletionStage<Channel> connect(URI url, Map<String, String> headers, Listener listener) {
        boolean ssl = "wss".equals(url.getScheme().toLowerCase(Locale.ROOT));
        WebSocketClientOptions options = new WebSocketClientOptions()
                .setConnectTimeout(TIMEOUT)
                // how long the tool has to close the connection once the close frame is sent (seconds): abort() closes
                // the client, which closes the connection like this
                .setClosingTimeout(1)
                // a tool can send a whole message in a single frame
                .setMaxFrameSize(MAX_MESSAGE_BYTES)
                .setMaxMessageSize(MAX_MESSAGE_BYTES);
        if (ssl && tls != null) {
            TlsConfigUtils.configure(options, tls);
        }
        String path = url.getRawPath() == null || url.getRawPath().isEmpty() ? "/" : url.getRawPath();
        WebSocketConnectOptions connect = new WebSocketConnectOptions()
                .setHost(url.getHost())
                .setPort(url.getPort() != -1 ? url.getPort() : ssl ? 443 : 80)
                .setSsl(ssl)
                // the path and query as given, encoded characters included
                .setURI(url.getRawQuery() != null ? path + "?" + url.getRawQuery() : path);
        headers.forEach(connect::addHeader);

        // a client for each connection, closed with it
        WebSocketClient client = vertx.createWebSocketClient(options);
        CompletableFuture<Channel> answer = new CompletableFuture<>();
        // the handshake: not WebSocketConnectOptions.setTimeout, which is also an idle timeout that can close the
        // connection once open
        long timer = vertx.setTimer(TIMEOUT, id -> {
            if (answer.completeExceptionally(new TimeoutException("WebSocket handshake timed out after " + TIMEOUT + " ms"))) {
                client.close();
            }
        });
        client.connect(connect).onComplete(ar -> {
            vertx.cancelTimer(timer);
            if (answer.isDone()) {
                // timed out
                if (ar.succeeded()) {
                    ar.result().close();
                }
                return;
            }
            if (ar.failed()) {
                client.close();
                answer.completeExceptionally(translate(ar.cause()));
                return;
            }
            WebSocket ws = ar.result();
            ws.textMessageHandler(listener::onText);
            ws.pongHandler(data -> listener.onPong());
            ws.exceptionHandler(e -> {
                LOG.debugf(e, "Camel CLI connector WebSocket error");
                listener.onError(e);
            });
            ws.closeHandler(v -> {
                client.close();
                // no status when the connection is lost without a close frame
                Short code = ws.closeStatusCode();
                LOG.debugf("Camel CLI connector WebSocket closed: %s %s", code, ws.closeReason());
                listener.onClose(code != null ? code : 1006, ws.closeReason());
            });
            answer.complete(new VertxChannel(ws, client));
        });
        return answer;
    }

    private static Throwable translate(Throwable e) {
        if (e instanceof UpgradeRejectedException rejected) {
            return new CliWebSocketHandshakeException(rejected.getStatus(), rejected);
        }
        return e;
    }

    private record VertxChannel(WebSocket ws, WebSocketClient client) implements Channel {

        @Override
        public CompletionStage<?> sendText(String text) {
            // in frames of FRAME_CHARS, never splitting a surrogate pair
            Future<Void> last = null;
            int start = 0;
            do {
                int end = Math.min(text.length(), start + FRAME_CHARS);
                if (end < text.length() && Character.isHighSurrogate(text.charAt(end - 1))) {
                    end--;
                }
                String part = text.substring(start, end);
                boolean fin = end == text.length();
                last = ws.writeFrame(start == 0
                        ? WebSocketFrame.textFrame(part, fin)
                        : WebSocketFrame.continuationFrame(Buffer.buffer(part), fin));
                start = end;
            } while (start < text.length());
            return last.toCompletionStage();
        }

        @Override
        public CompletionStage<?> sendPing() {
            return ws.writePing(Buffer.buffer()).toCompletionStage();
        }

        @Override
        public CompletionStage<?> close(int code, String reason) {
            return ws.close((short) code, reason).toCompletionStage();
        }

        @Override
        public void abort() {
            // closes the connection right away, without a close frame
            client.close();
        }
    }
}
