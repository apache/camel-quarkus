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

import java.net.URI;
import java.util.concurrent.CompletionException;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.UpgradeRejectedException;
import io.vertx.core.http.WebSocket;
import io.vertx.core.http.WebSocketClient;
import io.vertx.core.http.WebSocketConnectOptions;

final class WebSocketHandshakes {

    static final int SWITCHING_PROTOCOLS = 101;

    private WebSocketHandshakes() {
    }

    /**
     * The origin that the Quarkus HTTP server is serving the given URI from.
     */
    static String originOf(URI uri) {
        return "http://" + uri.getHost() + ":" + uri.getPort();
    }

    /**
     * Attempts a WebSocket upgrade with the given {@code Origin} request header, or with no {@code Origin} header at
     * all when {@code origin} is {@code null}.
     *
     * @return {@value #SWITCHING_PROTOCOLS} if the upgrade succeeded, else the HTTP status the handshake was rejected
     *         with
     */
    static int upgradeStatus(URI uri, String origin) {
        Vertx vertx = Vertx.vertx();
        try {
            WebSocketClient client = vertx.createWebSocketClient();
            WebSocketConnectOptions options = new WebSocketConnectOptions()
                    .setHost(uri.getHost())
                    .setPort(uri.getPort())
                    .setURI(uri.getPath())
                    // Vert.x strips any Origin header when allowOriginHeader is false, so it can only be disabled
                    // when the handshake is meant to carry no Origin at all. Otherwise leave it enabled and set the
                    // header explicitly, which Vert.x and Netty both leave untouched.
                    .setAllowOriginHeader(origin != null);

            if (origin != null) {
                options.putHeader(HttpHeaders.ORIGIN, origin);
            }

            try {
                WebSocket webSocket = client.connect(options).toCompletionStage().toCompletableFuture().join();
                webSocket.close().toCompletionStage().toCompletableFuture().join();
                return SWITCHING_PROTOCOLS;
            } catch (CompletionException e) {
                if (e.getCause() instanceof UpgradeRejectedException rejected) {
                    return rejected.getStatus();
                }
                throw e;
            }
        } finally {
            vertx.close().toCompletionStage().toCompletableFuture().join();
        }
    }
}
