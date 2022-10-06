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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.azure.core.http.HttpClient;
import com.azure.core.test.HttpClientTestsWireMockServer;
import com.azure.core.test.http.HttpClientTests;
import com.github.tomakehurst.wiremock.WireMockServer;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

public class VertxAsyncHttpClientAsyncHttpClientTests extends HttpClientTests {
    private static WireMockServer server;
    private static Vertx vertx;

    @BeforeAll
    public static void beforeAll() {
        server = HttpClientTestsWireMockServer.getHttpClientTestsServer();
        server.start();
        vertx = Vertx.vertx();
    }

    @AfterAll
    public static void afterAll() throws Exception {
        if (server != null) {
            server.shutdown();
        }

        if (vertx != null) {
            CountDownLatch latch = new CountDownLatch(1);
            vertx.close(new Handler<AsyncResult<Void>>() {
                @Override
                public void handle(AsyncResult<Void> event) {
                    latch.countDown();
                }
            });
            latch.await(5, TimeUnit.SECONDS);
        }
    }

    @Override
    protected int getWireMockPort() {
        return server.port();
    }

    @Override
    protected HttpClient createHttpClient() {
        return new VertxAsyncHttpClientBuilder().vertx(vertx).build();
    }
}
