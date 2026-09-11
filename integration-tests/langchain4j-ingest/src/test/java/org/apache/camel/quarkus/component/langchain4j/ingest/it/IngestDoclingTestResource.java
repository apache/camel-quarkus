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
package org.apache.camel.quarkus.component.langchain4j.ingest.it;

import java.util.Map;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.apache.camel.quarkus.test.mock.backend.MockBackendUtils;
import org.eclipse.microprofile.config.ConfigProvider;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

/**
 * Provides the Docling Serve endpoint for the {@code scans} pipeline and switches the pipeline
 * on. By default the docling-serve convert API is stubbed with an in-process WireMock server —
 * the real container image is a multi-gigabyte download, about 5 GB compressed for the pinned
 * v1.29.0 — and answers every conversion with the sentence
 * {@link Langchain4jIngestDoclingTest} ingests. The real container starts only with
 * {@code -Dcamel.quarkus.start-mock-backend=false}, the same convention the docling integration
 * tests use; the test document carries the same sentence, so the assertions hold there too.
 */
public class IngestDoclingTestResource implements QuarkusTestResourceLifecycleManager {

    static final String CONVERTED_MARKDOWN = "The EPSILON-2 valve seals at 80 bar.";

    private static final int CONTAINER_PORT = 5001;

    private WireMockServer server;
    private GenericContainer<?> container;

    @Override
    public Map<String, String> start() {
        String doclingServeUrl;
        if (MockBackendUtils.startMockBackend()) {
            server = new WireMockServer(WireMockConfiguration.options().dynamicPort());
            server.stubFor(post(urlEqualTo("/v1/convert/source"))
                    .willReturn(aResponse()
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"document\":{\"filename\":\"valve.pdf\",\"md_content\":\""
                                    + CONVERTED_MARKDOWN + "\",\"json_content\":null,\"html_content\":null,"
                                    + "\"text_content\":null,\"doctags_content\":null},\"status\":\"success\","
                                    + "\"errors\":[],\"processing_time\":0.01,\"timings\":{}}")));
            server.start();
            doclingServeUrl = "http://localhost:" + server.port();
        } else {
            container = new GenericContainer<>(
                    ConfigProvider.getConfig().getValue("docling.container.image", String.class))
                    .withExposedPorts(CONTAINER_PORT)
                    .waitingFor(Wait.forListeningPort());
            container.start();
            doclingServeUrl = "http://" + container.getHost() + ":" + container.getMappedPort(CONTAINER_PORT);
        }
        return Map.of(
                "docling.serve.url", doclingServeUrl,
                "quarkus.camel.langchain4j.ingest.scans.enabled", "true");
    }

    @Override
    public void stop() {
        if (server != null) {
            server.stop();
        }
        if (container != null) {
            container.stop();
        }
    }
}
