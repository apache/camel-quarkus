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
package org.apache.camel.quarkus.test.wiremock;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.common.ClasspathFileSource;
import com.github.tomakehurst.wiremock.common.FileSource;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.recording.RecordingStatus;
import com.github.tomakehurst.wiremock.recording.SnapshotRecordResult;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.apache.camel.quarkus.test.AvailablePortFinder;
import org.apache.camel.quarkus.test.mock.backend.MockBackendUtils;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;

import static com.github.tomakehurst.wiremock.client.WireMock.recordSpec;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

public abstract class WireMockTestResourceLifecycleManager implements QuarkusTestResourceLifecycleManager {

    protected static final Logger LOG = Logger.getLogger(WireMockTestResourceLifecycleManager.class);
    protected WireMockServer server;

    /**
     * Starts the {@link WireMockServer} and configures request / response recording if it has been enabled
     */
    @Override
    public Map<String, String> start() {
        Map<String, String> properties = new HashMap<>();

        if (isMockingEnabled() || isRecordingEnabled()) {
            server = createServer();
            server.start();

            if (isRecordingEnabled()) {
                String recordTargetBaseUrl = getRecordTargetBaseUrl();
                if (recordTargetBaseUrl != null) {
                    LOG.infof("Enabling WireMock recording for %s", recordTargetBaseUrl);
                    server.startRecording(recordSpec()
                            .forTarget(recordTargetBaseUrl)
                            .allowNonProxied(false));
                } else {
                    throw new IllegalStateException(
                            "Must return a non-null value from getRecordTargetBaseUrl() in order to support WireMock recording");
                }
            }

            String wireMockUrl = "http://localhost:" + server.port();
            LOG.infof("WireMock started on %s", wireMockUrl);
            properties.put("wiremock.url", wireMockUrl);

            // if https enabled
            if (server.getOptions().httpsSettings().enabled()) {
                properties.put("wiremock.url.ssl", "https://localhost:" + server.httpsPort());
            }
        }

        return properties;
    }

    /**
     * Stops the {@link WireMockServer} instance if it was started and stops recording if record mode was enabled
     */
    @Override
    public void stop() {
        if (server != null) {
            LOG.info("Stopping WireMockServer");
            if (server.getRecordingStatus().getStatus().equals(RecordingStatus.Recording)) {
                LOG.info("Stopping recording");
                SnapshotRecordResult recordResult = server.stopRecording();

                List<StubMapping> stubMappings = recordResult.getStubMappings();
                if (isDeleteRecordedMappingsOnError()) {
                    for (StubMapping mapping : stubMappings) {
                        int status = mapping.getResponse().getStatus();
                        if (status >= 300 && mapping.shouldBePersisted()) {
                            try {
                                String fileName = mapping.getName() + "-" + mapping.getId() + ".json";
                                Path mappingFilePath = Paths.get("./src/test/resources/mappings/", fileName);
                                Files.deleteIfExists(mappingFilePath);
                                LOG.infof("Deleted mapping file %s as status code was %d", fileName, status);
                            } catch (IOException e) {
                                LOG.errorf("Failed to delete mapping file %s", e, mapping.getName());
                            }
                        }
                    }
                }
            }
            server.stop();
            AvailablePortFinder.releaseReservedPorts();
        }
    }

    /**
     * If mocking is enabled, inject an instance of {@link WireMockServer} into any fields
     * annotated with {@link MockServer}. This gives full control over creating recording rules
     * and some aspects of the server lifecycle.
     *
     * The server instance is not injected if mocking is explicitly disabled, and therefore the resulting
     * {@link MockServer} annotated field value will be null.
     */
    @Override
    public void inject(Object testInstance) {
        if (isMockingEnabled() || isRecordingEnabled()) {
            Class<?> testClass = testInstance.getClass();
            while (testClass != Object.class) {
                for (Field field : testClass.getDeclaredFields()) {
                    if (field.getAnnotation(MockServer.class) != null) {
                        if (!WireMockServer.class.isAssignableFrom(field.getType())) {
                            throw new RuntimeException("@MockServer can only be used on fields of type WireMockServer");
                        }

                        field.setAccessible(true);
                        try {
                            if (server == null) {
                                LOG.info("Starting WireMockServer");
                                server = createServer();
                                server.start();
                            }
                            LOG.infof("Injecting WireMockServer for field %s", field.getName());
                            field.set(testInstance, server);
                            return;
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
                testClass = testClass.getSuperclass();
            }
        }
    }

    /**
     * Determines whether each of the given environment variable names is set
     */
    protected boolean envVarsPresent(String... envVarNames) {
        if (envVarNames.length == 0) {
            throw new IllegalArgumentException("envVarNames must not be empty");
        }

        boolean present = true;
        for (String envVar : envVarNames) {
            if (System.getenv(envVar) == null) {
                present = false;
                break;
            }
        }

        return present;
    }

    /**
     * Get the value of a given environment variable or a default value if it does not exist
     */
    protected String envOrDefault(String envVarName, String defaultValue) {
        return ConfigProvider.getConfig().getOptionalValue(envVarName, String.class).orElse(defaultValue);
    }

    /**
     * Whether recorded stub mapping files should be deleted if the HTTP response was an error code (>= 400).
     *
     * By default this returns true. Can be overridden if an error response is desired / expected from the HTTP request.
     */
    protected boolean isDeleteRecordedMappingsOnError() {
        return true;
    }

    /**
     * The target base URL that WireMock should watch for when recording requests.
     *
     * For example, if a test triggers an HTTP call on an external endpoint like https://api.foo.com/some/resource.
     * Then the base URL would be https://api.foo.com
     */
    protected abstract String getRecordTargetBaseUrl();

    /**
     * Conditions under which the {@link WireMockServer} should be started.
     */
    protected abstract boolean isMockingEnabled();

    /**
     * Customizes the {@link WireMockConfiguration} that will be used to create the next {@Link WireMockServer}.
     */
    protected void customizeWiremockConfiguration(WireMockConfiguration config) {
    }

    /**
     * Creates and starts a {@link WireMockServer} on a random port. {@link MockBackendUtils} triggers the log
     * message that signifies mocking is in use.
     */
    private WireMockServer createServer() {
        LOG.info("Starting WireMockServer");

        MockBackendUtils.startMockBackend(true);
        WireMockConfiguration configuration = options().port(AvailablePortFinder.getNextAvailable());
        customizeWiremockConfiguration(configuration);

        if (!isRecordingEnabled()) {
            // Read mapping resources from the classpath in playback mode
            configuration.fileSource(new CamelQuarkusFileSource());
        }
        return new WireMockServer(configuration);
    }

    /**
     * Determine whether to enable WireMock record mode:
     *
     * http://wiremock.org/docs/record-playback/
     */
    private boolean isRecordingEnabled() {
        String recordEnabled = System.getProperty("wiremock.record", System.getenv("WIREMOCK_RECORD"));
        return recordEnabled != null && recordEnabled.equals("true");
    }

    /**
     * A custom ClasspathFileSource so that WireMock mapping files can be resolved in the quarkus-platform build
     */
    private static class CamelQuarkusFileSource extends ClasspathFileSource {
        private CamelQuarkusFileSource() {
            super("");
        }

        @Override
        public FileSource child(String subDirectoryName) {
            return new ClasspathFileSource(subDirectoryName);
        }
    }
}
