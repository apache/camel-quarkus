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

import java.io.InputStream;
import java.net.URI;
import java.security.KeyStore;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import io.netty.channel.EventLoopGroup;
import io.undertow.websockets.WebsocketClientSslProvider;
import jakarta.websocket.ClientEndpointConfig;
import jakarta.websocket.Endpoint;

/**
 * Enable the Quarkus WebSocket client to handle self-signed certificates.
 */
public class VertxWebsocketClientSslProvider implements WebsocketClientSslProvider {
    private static final SSLContext SSL_CONTEXT;

    static {
        try (InputStream stream = VertxWebsocketClientSslProvider.class.getResourceAsStream("/truststore.p12")) {
            KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
            keystore.load(stream, "changeit".toCharArray());

            String defaultAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(defaultAlgorithm);
            trustManagerFactory.init(keystore);

            SSL_CONTEXT = SSLContext.getInstance("TLS");
            SSL_CONTEXT.init(null, trustManagerFactory.getTrustManagers(), null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public SSLContext getSsl(EventLoopGroup worker, Class<?> annotatedEndpoint, URI uri) {
        return SSL_CONTEXT;
    }

    @Override
    public SSLContext getSsl(EventLoopGroup worker, Object annotatedEndpointInstance, URI uri) {
        return SSL_CONTEXT;
    }

    @Override
    public SSLContext getSsl(EventLoopGroup worker, Endpoint endpoint, ClientEndpointConfig cec, URI uri) {
        return SSL_CONTEXT;
    }
}
