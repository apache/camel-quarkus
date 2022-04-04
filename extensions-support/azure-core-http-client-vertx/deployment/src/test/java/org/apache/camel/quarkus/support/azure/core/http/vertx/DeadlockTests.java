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

import java.security.SecureRandom;

import com.azure.core.http.HttpClient;
import com.azure.core.http.HttpMethod;
import com.azure.core.http.HttpRequest;
import com.azure.core.util.FluxUtil;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.quarkus.test.QuarkusUnitTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

public class DeadlockTests {

    @RegisterExtension
    static final QuarkusUnitTest CONFIG = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class));

    private static final String GET_ENDPOINT = "/get";

    private WireMockServer server;
    private byte[] expectedGetBytes;

    @BeforeEach
    public void configureWireMockServer() {
        expectedGetBytes = new byte[10 * 1024 * 1024];
        new SecureRandom().nextBytes(expectedGetBytes);

        server = new WireMockServer(WireMockConfiguration.options()
                .dynamicPort()
                .disableRequestJournal()
                .gzipDisabled(true));

        server.stubFor(WireMock.get(GET_ENDPOINT).willReturn(WireMock.aResponse().withBody(expectedGetBytes)));

        server.start();
    }

    @AfterEach
    public void shutdownWireMockServer() {
        if (server != null) {
            server.shutdown();
        }
    }

    @Test
    public void attemptToDeadlock() {
        HttpClient httpClient = new VertxHttpClientProvider().createInstance();

        String endpoint = server.baseUrl() + GET_ENDPOINT;

        for (int i = 0; i < 100; i++) {
            StepVerifier.create(httpClient.send(new HttpRequest(HttpMethod.GET, endpoint))
                    .flatMap(response -> FluxUtil.collectBytesInByteBufferStream(response.getBody())
                            .zipWith(Mono.just(response.getStatusCode()))))
                    .assertNext(responseTuple -> {
                        Assertions.assertEquals(200, responseTuple.getT2());
                        Assertions.assertArrayEquals(expectedGetBytes, responseTuple.getT1());
                    })
                    .verifyComplete();
        }
    }
}
