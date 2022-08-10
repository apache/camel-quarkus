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

package org.apache.camel.quarkus.test.support.google;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.apache.camel.quarkus.test.mock.backend.MockBackendUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;

public class GoogleCloudTestResource implements QuarkusTestResourceLifecycleManager {
    public static final String GOOGLE_EMULATOR_IMAGE = "gcr.io/google.com/cloudsdktool/cloud-sdk:390.0.0-emulators";
    public static final String PARAM_PROJECT_ID = "google.project.id";
    public static final String PARAM_CREDENTIALS_PATH = "google.credentialsPath";

    private static final Logger LOGGER = LoggerFactory.getLogger(GoogleCloudTestResource.class);

    private final GoogleCloudContext envContext = new GoogleCloudContext();

    /**
     * Method usable by dependant modules.
     *
     * @return True if mock backend should be started.
     */
    public static boolean isUsingMockBackend() {
        return isUsingMockBackend(getGoogleProjectId(), getGoogleApplicationCredentials());
    }

    private static boolean isUsingMockBackend(String realProjectId, String realCredentials) {
        final boolean realCredentialsProvided = realCredentials != null && realProjectId != null;
        return MockBackendUtils.startMockBackend(false) && !realCredentialsProvided;
    }

    private static String getGoogleProjectId() {
        return System.getenv("GOOGLE_PROJECT_ID");
    }

    private static String getGoogleApplicationCredentials() {
        return System.getenv("GOOGLE_APPLICATION_CREDENTIALS");
    }

    @Override
    public Map<String, String> start() {
        final String realCredentials = getGoogleApplicationCredentials();
        final String realProjectId = getGoogleProjectId();
        final boolean realCredentialsProvided = realCredentials != null && realProjectId != null;
        final boolean usingMockBackend = isUsingMockBackend(realProjectId, realCredentials);
        envContext.setUsingMockBackend(usingMockBackend);

        ServiceLoader<GoogleTestEnvCustomizer> loader = ServiceLoader.load(GoogleTestEnvCustomizer.class);
        List<GoogleTestEnvCustomizer> customizers = new ArrayList<>();
        for (GoogleTestEnvCustomizer customizer : loader) {
            LOGGER.info("Loaded GoogleTestEnvCustomizer " + customizer.getClass().getName());
            customizers.add(customizer);
        }

        if (!usingMockBackend) {
            if (!MockBackendUtils.startMockBackend(false) && !realCredentialsProvided) {
                throw new IllegalStateException(
                        "Set GOOGLE_APPLICATION_CREDENTIALS and GOOGLE_PROJECT_ID env vars if you set CAMEL_QUARKUS_START_MOCK_BACKEND=false");
            }

            envContext.property(PARAM_PROJECT_ID, realProjectId);
            envContext.property(PARAM_CREDENTIALS_PATH, realCredentials);

        } else {

            for (GoogleTestEnvCustomizer customizer : customizers) {
                GenericContainer container = customizer.createContainer();
                if (container != null) {
                    container.start();
                    envContext.closeable(container);
                }
            }
        }

        for (GoogleTestEnvCustomizer customizer : customizers) {
            customizer.customize(envContext);
        }

        return envContext.getProperties();
    }

    @Override
    public void inject(TestInjector testInjector) {
        for (Map.Entry<String, String> entry : envContext.getProperties().entrySet()) {
            testInjector.injectIntoFields(entry.getValue(), f -> {
                GoogleProperty gp = f.getAnnotation(GoogleProperty.class);
                return gp != null && entry.getKey().equals(gp.name());
            });
        }
    }

    public void stop() {
        if (envContext != null) {
            envContext.close();
        }
    }

}
