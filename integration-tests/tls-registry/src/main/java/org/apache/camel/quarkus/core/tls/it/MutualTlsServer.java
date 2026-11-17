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
package org.apache.camel.quarkus.core.tls.it;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.tls.TlsConfiguration;
import io.quarkus.tls.TlsConfigurationRegistry;
import io.vertx.core.Vertx;
import io.vertx.core.http.ClientAuth;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * An HTTPS server requiring a client certificate, started when the {@value #TLS_CONFIGURATION_NAME} TLS configuration
 * is present. quarkus.http.ssl.client-auth is fixed at build time, so the Quarkus HTTP server cannot require client
 * authentication for one test profile only.
 */
@ApplicationScoped
public class MutualTlsServer {
    static final String TLS_CONFIGURATION_NAME = "mtls-server";

    @Inject
    Vertx vertx;

    @Inject
    TlsConfigurationRegistry tlsRegistry;

    @ConfigProperty(name = "tls-registry.mtls.port", defaultValue = "8543")
    int port;

    private HttpServer server;

    void start(@Observes StartupEvent event) throws Exception {
        Optional<TlsConfiguration> tlsConfiguration = tlsRegistry.get(TLS_CONFIGURATION_NAME);
        if (tlsConfiguration.isEmpty()) {
            return;
        }

        HttpServerOptions options = new HttpServerOptions()
                .setHost("localhost")
                .setPort(port)
                .setSsl(true)
                .setKeyCertOptions(tlsConfiguration.get().getKeyStoreOptions())
                .setTrustOptions(tlsConfiguration.get().getTrustStoreOptions())
                .setClientAuth(ClientAuth.REQUIRED);

        server = vertx.createHttpServer(options)
                .requestHandler(request -> request.response().end("pong"))
                .listen()
                .toCompletionStage()
                .toCompletableFuture()
                .get(30, TimeUnit.SECONDS);
    }

    void stop(@Observes ShutdownEvent event) {
        if (server != null) {
            server.close();
        }
    }
}
