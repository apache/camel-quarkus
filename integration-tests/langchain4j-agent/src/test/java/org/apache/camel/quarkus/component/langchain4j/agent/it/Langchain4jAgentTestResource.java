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
package org.apache.camel.quarkus.component.langchain4j.agent.it;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import org.apache.camel.quarkus.component.langchain4j.agent.it.util.ProcessUtils;
import org.apache.camel.quarkus.test.wiremock.WireMockTestResourceLifecycleManager;
import org.junit.jupiter.api.condition.OS;

public class Langchain4jAgentTestResource extends WireMockTestResourceLifecycleManager {
    private static final String OLLAMA_ENV_URL = "LANGCHAIN4J_OLLAMA_BASE_URL";

    @Override
    public Map<String, String> start() {
        Map<String, String> properties = super.start();
        String wiremockUrl = properties.get("wiremock.url");
        String url = wiremockUrl != null ? wiremockUrl : getRecordTargetBaseUrl();
        properties.put("langchain4j.ollama.base-url", url);
        properties.put("nodejs.installed", isNodeJSInstallationExists().toString());
        return properties;
    }

    @Override
    protected String getRecordTargetBaseUrl() {
        return System.getenv(OLLAMA_ENV_URL);
    }

    @Override
    protected boolean isMockingEnabled() {
        return !envVarsPresent(OLLAMA_ENV_URL);
    }

    @Override
    protected void processRecordedStubMappings(List<StubMapping> stubMappings) {
        stubMappings.forEach(mapping -> {
            String fileName = mapping.getName() + "-" + mapping.getId() + ".json";
            Path mappingFilePath = Paths.get("./src/test/resources/mappings/", fileName);

            // ignoreExtraElements directive can lead to WireMock getting confused about which stub to use on request matching.
            // Force disabling it manually since there's no specific WireMock config option to tune it
            try {
                String mappingContent = Files.readString(mappingFilePath);
                mappingContent = mappingContent.replace("\"ignoreExtraElements\" : true", "\"ignoreExtraElements\" : false");
                Files.writeString(mappingFilePath, mappingContent);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            // RetrievalAugmentor bean setup causes /api/embed stubs to be recorded on every test run.
            // So clean up superfluous recordings unless the RAG test actually ran
            if (!Langchain4jTestWatcher.isRagTestExecuted() && mapping.getName().startsWith("api_embed")) {
                Path mappingBodyFilePath = Paths.get("./src/test/resources/__files", mapping.getResponse().getBodyFileName());
                try {
                    Files.deleteIfExists(mappingFilePath);
                    Files.deleteIfExists(mappingBodyFilePath);
                } catch (IOException e) {
                    // Ignored
                }
            }
        });
    }

    @Override
    public void stop() {
        try {
            super.stop();
        } finally {
            Langchain4jTestWatcher.reset();
        }
    }

    private Boolean isNodeJSInstallationExists() {
        try {
            // TODO: Suppress MCP tests in GitHub Actions for windows - https://github.com/apache/camel-quarkus/issues/8007
            if (OS.current().equals(OS.WINDOWS) && System.getenv("CI") != null) {
                return false;
            }

            Process process = new ProcessBuilder()
                    .command(ProcessUtils.getNpxExecutable(), "--version")
                    .start();

            process.waitFor(10, TimeUnit.SECONDS);
            return process.exitValue() == 0;
        } catch (Exception e) {
            LOG.error("Failed detecting Node.js", e);
        }
        return false;
    }
}
