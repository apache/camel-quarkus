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

import com.azure.core.http.HttpClient;
import com.azure.core.test.HttpClientTestsWireMockServer;
import com.azure.core.test.http.HttpClientTests;
import com.github.tomakehurst.wiremock.WireMockServer;
import io.vertx.core.Vertx;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

public class VertxHttpClientHttpClientTests extends HttpClientTests {
    private static final WireMockServer server = HttpClientTestsWireMockServer.getHttpClientTestsServer();
    private static final Vertx vertx = Vertx.vertx();

    @BeforeAll
    public static void getWireMockServer() {
        server.start();
    }

    @AfterAll
    public static void afterAll() throws InterruptedException {
        server.shutdown();
        CountDownLatch latch = new CountDownLatch(1);
        vertx.close(x -> latch.countDown());
        latch.await();
    }

    @Override
    protected int getWireMockPort() {
        return server.port();
    }

    @Override
    protected HttpClient createHttpClient() {
        return new VertxHttpClientBuilder(vertx).build();
    }
}
