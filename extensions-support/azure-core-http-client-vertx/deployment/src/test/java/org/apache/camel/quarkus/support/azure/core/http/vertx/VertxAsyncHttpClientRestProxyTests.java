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

import java.io.IOException;

import com.azure.core.http.HttpClient;
import com.azure.core.test.RestProxyTestsWireMockServer;
import com.azure.core.test.implementation.RestProxyTests;
import com.azure.core.util.Context;
import com.github.tomakehurst.wiremock.WireMockServer;
import io.quarkus.test.QuarkusUnitTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class VertxAsyncHttpClientRestProxyTests extends RestProxyTests {
    private static WireMockServer server;

    @RegisterExtension
    static final QuarkusUnitTest CONFIG = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addAsResource("upload.txt", "upload.txt"));

    @BeforeAll
    public static void beforeAll() {
        server = RestProxyTestsWireMockServer.getRestProxyTestsServer();
        server.start();
    }

    @AfterAll
    public static void afterAll() throws Exception {
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
        return new VertxAsyncHttpClientBuilder().build();
    }

    /*
     * The following methods are overridden and reimplemented to work around issues with
     * parameterized tests not working properly with QuarkusUnitTest.
     */

    @Override
    @ParameterizedTest
    @MethodSource("downloadTestArgumentProvider")
    @Disabled
    public void simpleDownloadTest(Context context) {
    }

    @SuppressWarnings("unchecked")
    @Test
    public void simpleDownloadTest() {
        downloadTestArgumentProvider().forEach(arguments -> {
            Named<Context> named = (Named<Context>) arguments.get()[0];
            super.simpleDownloadTest(named.getPayload());
        });
    }

    @Override
    @ParameterizedTest
    @MethodSource("downloadTestArgumentProvider")
    @Disabled
    public void simpleDownloadTestAsync(Context context) {
    }

    @SuppressWarnings("unchecked")
    @Test
    public void simpleDownloadTestAsync() {
        downloadTestArgumentProvider().forEach(arguments -> {
            Named<Context> named = (Named<Context>) arguments.get()[0];
            super.simpleDownloadTestAsync(named.getPayload());
        });
    }

    @Override
    @ParameterizedTest
    @MethodSource("downloadTestArgumentProvider")
    @Disabled
    public void streamResponseCanTransferBody(Context context) throws IOException {
    }

    @SuppressWarnings("unchecked")
    @Test
    public void streamResponseCanTransferBody() {
        downloadTestArgumentProvider().forEach(arguments -> {
            Named<Context> named = (Named<Context>) arguments.get()[0];
            try {
                super.streamResponseCanTransferBody(named.getPayload());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    @ParameterizedTest
    @MethodSource("downloadTestArgumentProvider")
    @Disabled
    public void streamResponseCanTransferBodyAsync(Context context) throws IOException {
    }

    @SuppressWarnings("unchecked")
    @Test
    public void streamResponseCanTransferBodyAsync() {
        downloadTestArgumentProvider().forEach(arguments -> {
            Named<Context> named = (Named<Context>) arguments.get()[0];
            try {
                super.streamResponseCanTransferBodyAsync(named.getPayload());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
