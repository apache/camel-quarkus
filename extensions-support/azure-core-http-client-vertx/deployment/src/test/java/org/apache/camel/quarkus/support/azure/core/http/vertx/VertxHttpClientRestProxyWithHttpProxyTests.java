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
package org.apache.camel.quarkus.support.azure.core.http.vertx;

import java.net.InetSocketAddress;

import javax.inject.Inject;

import com.azure.core.http.HttpClient;
import com.azure.core.http.ProxyOptions;
import com.azure.core.test.RestProxyTestsWireMockServer;
import com.azure.core.test.implementation.RestProxyTests;
import com.github.tomakehurst.wiremock.WireMockServer;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.QuarkusTestResource;
import io.vertx.core.Vertx;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.apache.camel.quarkus.support.azure.core.http.vertx.VertxHttpClientTestResource.PROXY_PASSWORD;
import static org.apache.camel.quarkus.support.azure.core.http.vertx.VertxHttpClientTestResource.PROXY_USER;

@QuarkusTestResource(VertxHttpClientTestResource.class)
public class VertxHttpClientRestProxyWithHttpProxyTests extends RestProxyTests {
    private static WireMockServer server;

    @RegisterExtension
    static final QuarkusUnitTest CONFIG = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addAsResource("upload.txt", "upload.txt"));

    @Inject
    Vertx vertx;

    @BeforeAll
    public static void getWireMockServer() {
        server = RestProxyTestsWireMockServer.getRestProxyTestsServer();
        server.start();
    }

    @AfterAll
    public static void shutdownWireMockServer() {
        if (server != null) {
            server.shutdown();
        }
    }

    @Override
    protected int getWireMockPort() {
        return server.port();
    }

    @Override
    protected HttpClient createHttpClient() {
        Config config = ConfigProvider.getConfig();
        String proxyHost = config.getValue("tiny.proxy.host", String.class);
        int proxyPort = config.getValue("tiny.proxy.port", int.class);

        InetSocketAddress address = new InetSocketAddress(proxyHost, proxyPort);
        ProxyOptions proxyOptions = new ProxyOptions(ProxyOptions.Type.HTTP, address);
        proxyOptions.setCredentials(PROXY_USER, PROXY_PASSWORD);

        return new VertxHttpClientBuilder(vertx)
                .proxy(proxyOptions)
                .build();
    }
}
