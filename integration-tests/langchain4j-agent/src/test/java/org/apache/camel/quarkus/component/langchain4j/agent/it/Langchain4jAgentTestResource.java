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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
    protected String getStubsSubdirectory() {
        return "agent";
    }

    @Override
    protected void processRecordedStubMappings(List<StubMapping> stubMappings) {
        // Process stubs in recording order so we keep the first occurrence of each unique request body
        List<StubMapping> sorted = new ArrayList<>(stubMappings);
        sorted.sort(Comparator.comparingLong(StubMapping::getInsertionIndex));

        Set<String> seenRequestBodies = new HashSet<>();
        ObjectMapper mapper = new ObjectMapper();

        for (StubMapping mapping : sorted) {
            String fileName = mapping.getName() + "-" + mapping.getId() + ".json";
            Path mappingFilePath = Paths.get("./src/test/resources/mappings/", fileName);

            // When multiple tests send identical requests, WireMock records duplicate stubs and links them via
            // a scenario state machine to enforce recording-time ordering. During replay this breaks when tests
            // run in a different order. Deduplicate by keeping only the first stub per unique request body.
            String requestBodyKey = normalizeRequestBody(mapper, mapping);
            if (requestBodyKey != null && !seenRequestBodies.add(requestBodyKey)) {
                try {
                    Files.deleteIfExists(mappingFilePath);
                } catch (IOException e) {
                    LOG.warnf("Failed to delete duplicate stub %s: %s", fileName, e.getMessage());
                }
                continue;
            }

            try {
                ObjectNode rootNode = (ObjectNode) mapper.readTree(Files.readString(mappingFilePath));

                // Remove scenario state so stubs are not tied to recording-time test execution order
                rootNode.remove("scenarioName");
                rootNode.remove("requiredScenarioState");
                rootNode.remove("newScenarioState");

                // ignoreExtraElements can lead to WireMock matching the wrong stub; force it off
                String content = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(rootNode);
                content = content.replace("\"ignoreExtraElements\" : true", "\"ignoreExtraElements\" : false");
                Files.writeString(mappingFilePath, content);
            } catch (IOException e) {
                throw new RuntimeException("Failed to process stub mapping " + fileName, e);
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
        }
    }

    private String normalizeRequestBody(ObjectMapper mapper, StubMapping mapping) {
        var bodyPatterns = mapping.getRequest().getBodyPatterns();
        if (bodyPatterns == null || bodyPatterns.isEmpty()) {
            return null;
        }
        try {
            // Normalize to canonical JSON so whitespace differences don't prevent deduplication
            String body = String.valueOf(bodyPatterns.get(0).getValue());
            return mapper.writeValueAsString(mapper.readTree(body));
        } catch (Exception e) {
            return String.valueOf(bodyPatterns.get(0).getValue());
        }
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
