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

package org.apache.camel.quarkus.component.fhir.it;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.apache.camel.quarkus.component.fhir.it.util.FhirTestHelper;
import org.apache.camel.util.CollectionHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

public class FhirTestResource implements QuarkusTestResourceLifecycleManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(FhirTestResource.class);
    private static final String FHIR_DSTU_CONTAINER_TAG = "v4.2.0";
    private static final String FHIR_DSTU_CONTEXT_PATH = "/hapi-fhir-jpaserver/fhir";
    private static final String FHIR_R_CONTAINER_TAG = "v6.2.2";
    private static final String FHIR_R_CONTEXT_PATH = "/fhir";
    private static final int CONTAINER_PORT = 8080;

    private FhirVersion fhirVersion;
    private GenericContainer<?> container;

    @Override
    public void init(Map<String, String> initArgs) {
        this.fhirVersion = FhirVersion.valueOf(initArgs.get("fhirVersion"));
    }

    @Override
    public Map<String, String> start() {
        if (this.fhirVersion == null) {
            return Collections.emptyMap();
        }

        if (!FhirTestHelper.isFhirVersionEnabled(fhirVersion.name())) {
            LOGGER.info("FHIR version {} is disabled. No hapi test container will be started for it.",
                    fhirVersion.simpleVersion());
            return Collections.emptyMap();
        }

        try {
            LOGGER.info("FHIR version {} is enabled. Starting hapi test container for it.", fhirVersion.simpleVersion());
            String imageName = fhirVersion.getContainerImageName();
            container = new GenericContainer<>(imageName)
                    .withExposedPorts(CONTAINER_PORT)
                    .withEnv(fhirVersion.getFhirContainerVersionEnvVarName(), fhirVersion.getFhirContainerVersionEnvVarValue())
                    .withEnv("hapi.fhir.allow_multiple_delete", "true")
                    .withEnv("hapi.fhir.reuse_cached_search_results_millis", "-1")
                    .waitingFor(Wait.forHttp(fhirVersion.getHealthEndpointPath()).withStartupTimeout(Duration.ofMinutes(5)));

            container.start();

            return CollectionHelper.mapOf(
                    String.format("camel.fhir.%s.test-url", fhirVersion.simpleVersion()),
                    fhirVersion.getServerUrl(container.getHost(), container.getMappedPort(CONTAINER_PORT)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void stop() {
        try {
            if (container != null) {
                container.stop();
            }
        } catch (Exception e) {
            // ignored
        }
    }

    enum FhirVersion {
        DSTU2(FHIR_DSTU_CONTAINER_TAG, FHIR_DSTU_CONTEXT_PATH),
        DSTU2_HL7ORG(FHIR_DSTU_CONTAINER_TAG, FHIR_DSTU_CONTEXT_PATH),
        DSTU2_1(FHIR_DSTU_CONTAINER_TAG, FHIR_DSTU_CONTEXT_PATH),
        DSTU3(FHIR_DSTU_CONTAINER_TAG, FHIR_DSTU_CONTEXT_PATH),
        R4(FHIR_R_CONTAINER_TAG, FHIR_R_CONTEXT_PATH),
        R5(FHIR_R_CONTAINER_TAG, FHIR_R_CONTEXT_PATH);

        private final String fhirImageTag;
        private final String contextPath;

        FhirVersion(String fhirImageTag, String contextPath) {
            this.fhirImageTag = fhirImageTag;
            this.contextPath = contextPath;
        }

        public String simpleVersion() {
            return this.name().toLowerCase();
        }

        public String getFhirContainerImageTag() {
            return fhirImageTag;
        }

        public String getFhirContainerVersionEnvVarName() {
            if (contextPath.equals(FHIR_DSTU_CONTEXT_PATH)) {
                return "HAPI_FHIR_VERSION";
            }
            return "hapi.fhir.fhir_version";
        }

        public String getFhirContainerVersionEnvVarValue() {
            // Cannot pass DSTU2_HL7ORG as the version to the mock server. However, it is analogous to DSTU2 anyway
            if (this == DSTU2_HL7ORG) {
                return DSTU2.name();
            }
            return this.name();
        }

        public String getContextPath() {
            return contextPath;
        }

        public String getContainerImageName() {
            return String.format("hapiproject/hapi:%s", getFhirContainerImageTag());
        }

        public String getServerUrl(String host, int port) {
            return String.format("http://%s:%d%s", host, port, getContextPath());
        }

        public String getHealthEndpointPath() {
            return String.format("%s/metadata", getContextPath());
        }
    }
}
