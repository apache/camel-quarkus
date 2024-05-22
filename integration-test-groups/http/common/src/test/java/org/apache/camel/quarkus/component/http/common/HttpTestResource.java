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
package org.apache.camel.quarkus.component.http.common;

import java.util.Base64;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetSocket;
import io.vertx.core.streams.Pump;
import org.apache.camel.quarkus.test.AvailablePortFinder;
import org.jboss.logging.Logger;

import static org.apache.camel.quarkus.component.http.common.AbstractHttpResource.USER_ADMIN;
import static org.apache.camel.quarkus.component.http.common.AbstractHttpResource.USER_ADMIN_PASSWORD;

public class HttpTestResource implements QuarkusTestResourceLifecycleManager {
    private static final Logger LOG = Logger.getLogger(HttpTestResource.class);

    public static final String KEYSTORE_NAME = "localhost";
    public static final String KEYSTORE_PASSWORD = "localhost-keystore-password";

    private ProxyServer server;

    @Override
    public Map<String, String> start() {
        Map<String, String> options = AvailablePortFinder.reserveNetworkPorts(
                Objects::toString,
                "proxy.port",
                "camel.netty-http.test-port",
                "camel.netty-http.https-test-port",
                "camel.netty-http.compression-test-port");
        options.put("proxy.host", "localhost");
        options.put("proxy.connection.timeout", "10000");

        server = new ProxyServer(Integer.parseInt(options.get("proxy.port")), USER_ADMIN, USER_ADMIN_PASSWORD);
        server.start();

        return options;
    }

    @Override
    public void stop() {
        AvailablePortFinder.releaseReservedPorts();
        if (server != null) {
            server.stop();
        }
    }

    /**
     * Bare-bones HTTP proxy server implementation that supports authentication.
     */
    static final class ProxyServer implements Handler<HttpServerRequest> {
        private final int port;
        private final String proxyUser;
        private final String proxyPassword;
        private final Vertx vertx;
        private final HttpServer proxyServer;

        ProxyServer(int port, String proxyUser, String proxyPassword) {
            this.port = port;
            this.proxyUser = proxyUser;
            this.proxyPassword = proxyPassword;
            this.vertx = Vertx.vertx(new VertxOptions().setWorkerPoolSize(1).setEventLoopPoolSize(1));
            this.proxyServer = vertx.createHttpServer();
        }

        void start() {
            CountDownLatch startLatch = new CountDownLatch(1);
            proxyServer.requestHandler(this);
            proxyServer.listen(port).onComplete(result -> {
                LOG.infof("HTTP proxy server started on port %d", port);
                startLatch.countDown();
            });
            try {
                startLatch.await(1, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        void stop() {
            if (proxyServer != null) {
                LOG.info("HTTP proxy server shutting down");
                proxyServer.close();
            }
            if (vertx != null) {
                vertx.close();
            }
        }

        @Override
        public void handle(HttpServerRequest httpServerRequest) {
            String authorization = httpServerRequest.getHeader("Proxy-Authorization");
            HttpServerResponse response = httpServerRequest.response();
            if (httpServerRequest.method().equals(HttpMethod.CONNECT) && authorization == null) {
                response.putHeader("Proxy-Authenticate", "Basic")
                        .setStatusCode(407)
                        .end();
                return;
            }

            String[] authParts = authorization.split(" ");
            String[] credentials = new String(Base64.getDecoder().decode(authParts[1])).split(":");
            if (credentials.length != 2) {
                response.setStatusCode(400).end();
            } else {
                if (credentials[0].equals(proxyUser) && credentials[1].equals(proxyPassword)) {
                    String host = httpServerRequest.getHeader("Host");
                    String[] hostParts = host.split(":");

                    // Deal with the result of the CONNECT tunnel and proxy the request / response
                    NetClient netClient = vertx.createNetClient();
                    netClient.connect(Integer.parseInt(hostParts[1]), hostParts[0], result -> {
                        if (result.succeeded()) {
                            NetSocket clientSocket = result.result();
                            NetSocket serverSocket = httpServerRequest.toNetSocket().result();
                            serverSocket.closeHandler(v -> clientSocket.close());
                            clientSocket.closeHandler(v -> serverSocket.close());
                            Pump.pump(serverSocket, clientSocket).start();
                            Pump.pump(clientSocket, serverSocket).start();
                        } else {
                            response.setStatusCode(403).end();
                        }
                    });
                } else {
                    response.setStatusCode(401).end();
                }
            }
        }
    }
}
