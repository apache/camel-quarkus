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
package org.apache.camel.quarkus.component.vertx.websocket;

import java.net.URI;
import java.util.concurrent.ExecutionException;

import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.vertx.http.runtime.CertificateConfig;
import io.quarkus.vertx.http.runtime.HttpConfiguration;
import io.quarkus.vertx.http.runtime.ServerSslConfig;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.WebSocketConnectOptions;
import io.vertx.ext.web.Router;
import org.apache.camel.CamelContext;
import org.apache.camel.component.vertx.websocket.VertxWebsocketComponent;
import org.apache.camel.component.vertx.websocket.VertxWebsocketConfiguration;
import org.apache.camel.component.vertx.websocket.VertxWebsocketConstants;
import org.apache.camel.component.vertx.websocket.VertxWebsocketEndpoint;
import org.apache.camel.component.vertx.websocket.VertxWebsocketHost;
import org.apache.camel.component.vertx.websocket.VertxWebsocketHostConfiguration;
import org.apache.camel.component.vertx.websocket.VertxWebsocketHostKey;
import org.apache.camel.spi.annotations.Component;
import org.apache.camel.util.ObjectHelper;

@Recorder
public class VertxWebsocketRecorder {
    private static volatile int PORT;
    private static volatile String HOST;

    public RuntimeValue<VertxWebsocketComponent> createVertxWebsocketComponent(
            RuntimeValue<Vertx> vertx,
            RuntimeValue<Router> router,
            LaunchMode launchMode,
            HttpConfiguration httpConfig) {

        boolean sslEnabled = false;
        int httpPort = httpConfig.determinePort(launchMode);
        int httpsPort = httpConfig.determineSslPort(launchMode);

        ServerSslConfig ssl = httpConfig.ssl;
        if (ssl != null) {
            CertificateConfig certificate = ssl.certificate;
            if (certificate != null) {
                if (certificate.files.isPresent() && certificate.keyFiles.isPresent()) {
                    sslEnabled = true;
                }

                if (certificate.keyStoreFile.isPresent() && certificate.keyStorePassword.isPresent()) {
                    sslEnabled = true;
                }
            }
        }

        HOST = httpConfig.host;
        PORT = sslEnabled ? httpsPort : httpPort;

        QuarkusVertxWebsocketComponent component = new QuarkusVertxWebsocketComponent();
        component.setVertx(vertx.getValue());
        component.setRouter(router.getValue());
        component.setDefaultHost(HOST);
        component.setDefaultPort(PORT);
        return new RuntimeValue<>(component);
    }

    @Component("vertx-websocket")
    static final class QuarkusVertxWebsocketComponent extends VertxWebsocketComponent {
        @Override
        protected VertxWebsocketHost createVertxWebsocketHost(VertxWebsocketHostConfiguration hostConfiguration,
                VertxWebsocketHostKey hostKey) {
            return new QuarkusVertxWebsocketHost(getCamelContext(), hostConfiguration, hostKey);
        }

        @Override
        protected VertxWebsocketEndpoint createEndpointInstance(String uri, VertxWebsocketConfiguration configuration) {
            return new QuarkusVertxWebsocketEndpoint(uri, this, configuration);
        }
    }

    public static final class QuarkusVertxWebsocketEndpoint extends VertxWebsocketEndpoint {
        public QuarkusVertxWebsocketEndpoint(String uri, VertxWebsocketComponent component,
                VertxWebsocketConfiguration configuration) {
            super(uri, component, configuration);
        }

        @Override
        public WebSocketConnectOptions getWebSocketConnectOptions(HttpClientOptions options) {
            WebSocketConnectOptions connectOptions = super.getWebSocketConnectOptions(options);
            URI uri = URI.create(getEndpointUri().replaceFirst("wss?:/*", ""));
            if (ObjectHelper.isNotEmpty(uri.getHost()) && uri.getPort() == -1) {
                connectOptions.setPort(connectOptions.isSsl() ? VertxWebsocketConstants.DEFAULT_VERTX_CLIENT_WSS_PORT
                        : VertxWebsocketConstants.DEFAULT_VERTX_CLIENT_WS_PORT);
            }
            return connectOptions;
        }
    }

    static final class QuarkusVertxWebsocketHost extends VertxWebsocketHost {
        public QuarkusVertxWebsocketHost(CamelContext camelContext, VertxWebsocketHostConfiguration websocketHostConfiguration,
                VertxWebsocketHostKey key) {
            super(camelContext, websocketHostConfiguration, key);
        }

        @Override
        public void start() throws InterruptedException, ExecutionException {
            // Noop as quarkus-vertx-http handles the server lifecycle
        }

        @Override
        public int getPort() {
            return PORT;
        }

        @Override
        public boolean isManagedHost(String host) {
            return HOST.equals(host);
        }

        @Override
        public boolean isManagedPort(int port) {
            return port == getPort();
        }
    }
}
