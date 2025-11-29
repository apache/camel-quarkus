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
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import org.apache.camel.quarkus.component.langchain4j.agent.it.util.ProcessUtils;
import org.apache.camel.quarkus.test.wiremock.WireMockTestResourceLifecycleManager;

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
            ProcessBuilder pb = new ProcessBuilder()
                    .command(ProcessUtils.getNpxExecutable(), "--version");
            Process process = pb.start();

            boolean finished = process.waitFor(20, TimeUnit.SECONDS);

            if (!finished) {
                process.destroyForcibly();
                LOG.error("Command: %s took too long.".formatted(String.join(" ", pb.command())));
                return false;
            }

            int exitCode = process.exitValue();
            String output = readStream(process.getInputStream());
            String errorOutput = readStream(process.getErrorStream());

            if (exitCode == 0) {
                LOG.info("Command: %s: %s".formatted(String.join(" ", pb.command()), output.trim()));
            } else {
                LOG.error("Command: %s failed with %s".formatted(String.join(" ", pb.command()), exitCode));
                LOG.error("Process standard output %s".formatted(output.trim()));
                LOG.error("Process error output %s".formatted(errorOutput.trim()));
            }

            return exitCode == 0;
        } catch (Exception e) {
            LOG.error("Failed detecting Node.js", e);
            return false;
        }
    }

    private String readStream(InputStream inputStream) throws IOException {
        try (inputStream) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
