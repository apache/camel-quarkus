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
package org.apache.camel.quarkus.component.weaviate.it;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.apache.camel.quarkus.test.mock.backend.MockBackendUtils;
import org.eclipse.microprofile.config.ConfigProvider;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.weaviate.WeaviateContainer;

public class WeaviateTestResource implements QuarkusTestResourceLifecycleManager {

    private static final DockerImageName WEAVIATE_IMAGE = DockerImageName
            .parse(ConfigProvider.getConfig().getValue("weaviate.container.image", String.class))
            .asCompatibleSubstituteFor("semitechnologies/weaviate");

    private final WeaviateContainer container = new WeaviateContainer(WEAVIATE_IMAGE)
            .withStartupTimeout(Duration.ofMinutes(3L));

    @Override
    public Map<String, String> start() {
        //detect real/mock backend
        Optional<String> apiKey = ConfigProvider.getConfig().getOptionalValue(WeaviateResource.WEAVIATE_API_KEY_ENV,
                String.class);
        Optional<String> hostKey = ConfigProvider.getConfig().getOptionalValue(WeaviateResource.WEAVIATE_HOST_ENV,
                String.class);

        final boolean startMockBackend = MockBackendUtils.startMockBackend(false);
        final boolean realApiProvided = apiKey.isPresent() && hostKey.isPresent();
        final boolean usingMockBackend = startMockBackend && !realApiProvided;

        if (usingMockBackend) {
            MockBackendUtils.logMockBackendUsed();
            container.start();

            return Map.of(
                    WeaviateResource.WEAVIATE_CONTAINER_ADDRESS, container.getHttpHostAddress());
        } else if (!startMockBackend && !realApiProvided) {
            throw new IllegalStateException(
                    "Set %s and %s env vars if you set CAMEL_QUARKUS_START_MOCK_BACKEND=false"
                            .formatted(WeaviateResource.WEAVIATE_API_KEY_ENV, WeaviateResource.WEAVIATE_HOST_ENV));
        } else {
            MockBackendUtils.logRealBackendUsed();
        }

        return Collections.emptyMap();
    }

    @Override
    public void stop() {
        if (container.isRunning()) {
            container.stop();
        }
    }
}
