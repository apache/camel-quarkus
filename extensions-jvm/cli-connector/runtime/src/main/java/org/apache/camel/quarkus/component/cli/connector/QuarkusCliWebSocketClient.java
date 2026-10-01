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
import java.util.Map;
import java.util.OptionalInt;
import java.util.concurrent.CompletionStage;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.websockets.next.BasicWebSocketConnector;
import io.quarkus.websockets.next.BasicWebSocketConnector.ExecutionModel;
import io.quarkus.websockets.next.CloseReason;
import io.quarkus.websockets.next.WebSocketClientConnection;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.UpgradeRejectedException;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import org.apache.camel.cli.connector.CliWebSocketClient;
import org.apache.camel.cli.connector.CliWebSocketHandshakeException;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;

/**
 * The {@link CliWebSocketClient} of the Camel CLI connector WebSocket transport, with the Quarkus WebSockets Next
 * client: registered as a bean only when the application has {@code quarkus-websockets-next}.
 * <p/>
 * The callbacks run on the Vert.x event loop: they only hand over to the transport, which parses and sends on its own
 * threads.
 */
public class QuarkusCliWebSocketClient implements CliWebSocketClient {

    static final String NAME = "quarkus-websockets-next";
    private static final Logger LOG = Logger.getLogger(QuarkusCliWebSocketClient.class);
    private static final String MAX_MESSAGE_SIZE_KEY = "quarkus.websockets-next.client.max-message-size";
    private static final int GOING_AWAY = 1001;

    @Inject
    CamelCliConnectorRunTimeConfig config;

    @PostConstruct
    void checkMaxMessageSize() {
        // set globally, it replaces the size of every connector (see customizeOptions)
        OptionalInt size = ConfigProvider.getConfig().getOptionalValue(MAX_MESSAGE_SIZE_KEY, Integer.class)
                .map(OptionalInt::of).orElse(OptionalInt.empty());
        if (size.isPresent() && size.getAsInt() < MAX_MESSAGE_SIZE) {
            LOG.warnf("%s=%d: the Camel CLI connector cannot receive actions larger than that (up to %d supported)",
                    MAX_MESSAGE_SIZE_KEY, size.getAsInt(), MAX_MESSAGE_SIZE);
        }
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public CompletionStage<Channel> connect(URI url, Map<String, String> headers, Listener listener) {
        // a new connector for each connection, released when it is closed
        InstanceHandle<BasicWebSocketConnector> handle = Arc.container().instance(BasicWebSocketConnector.class);
        String path = url.getRawPath() != null ? url.getRawPath() : "";
        int last = path.lastIndexOf('/') + 1;
        BasicWebSocketConnector connector = handle.get()
                .baseUri(baseUri(url, path.substring(0, last)))
                // the last segment as the path: WebSockets Next adds a '/' after the base uri path otherwise
                // TODO: Remove when https://github.com/quarkusio/quarkus/issues/57081 is fixed
                .path(path.substring(last))
                .executionModel(ExecutionModel.NON_BLOCKING)
                // not the frame size: Vert.x also splits the messages it sends into frames of that size, and servers
                // commonly refuse frames over 64 KB
                .customizeOptions((connect, client) -> client.setMaxMessageSize(MAX_MESSAGE_SIZE))
                .onTextMessage((c, text) -> listener.onText(text))
                .onPong((c, data) -> listener.onPong())
                // no onClose: when a close times out (abort() on a tool that no longer answers), Vert.x reports the status
                // 1006, WebSockets Next fails to create the CloseReason passed to onClose, and then skips releasing the
                // connection. The transport notices a closed connection on its next send (every snapshot interval), or by
                // the heartbeat
                // TODO: Remove when https://github.com/quarkusio/quarkus/issues/57080 is fixed
                .onError((c, error) -> listener.onError(error));
        headers.forEach(connector::addHeader);
        config.websocket().tlsConfigurationName().ifPresent(connector::tlsConfigurationName);
        try {
            return connector.connect()
                    .<Channel> map(QuarkusChannel::new)
                    .onTermination().invoke(handle::destroy)
                    .onFailure().transform(QuarkusCliWebSocketClient::translate)
                    .subscribeAsCompletionStage();
        } catch (RuntimeException e) {
            handle.destroy();
            throw e;
        }
    }

    /**
     * The url with this path. WebSockets Next sends the decoded path and query of the base uri: the {@code %} are
     * escaped so that the decoded ones are the path and query as given (a query parameter holding {@code %26} must not
     * become a {@code &}).
     */
    static URI baseUri(URI url, String path) {
        String query = url.getRawQuery();
        return URI.create(url.getScheme() + "://" + url.getRawAuthority() + path.replace("%", "%25")
                + (query != null ? "?" + query.replace("%", "%25") : ""));
    }

    private static Throwable translate(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof UpgradeRejectedException rejected) {
                return new CliWebSocketHandshakeException(rejected.getStatus(), rejected);
            }
        }
        return e;
    }

    private record QuarkusChannel(WebSocketClientConnection connection) implements Channel {

        @Override
        public CompletionStage<?> sendText(String text) {
            return connection.sendText(text).subscribeAsCompletionStage();
        }

        @Override
        public CompletionStage<?> sendPing() {
            return connection.sendPing(Buffer.buffer()).subscribeAsCompletionStage();
        }

        @Override
        public CompletionStage<?> close(int code, String reason) {
            return connection.close(new CloseReason(code, reason)).subscribeAsCompletionStage();
        }

        @Override
        public void abort() {
            // WebSockets Next cannot drop a connection without a close frame: the connection is closed at the latest
            // after quarkus.websockets-next.client.connection-closing-timeout
            if (!connection.isClosed()) {
                connection.close(new CloseReason(GOING_AWAY, "abort")).subscribe().with(v -> {
                }, e -> LOG.debugf(e, "Error closing the Camel CLI connector WebSocket"));
            }
        }
    }
}
