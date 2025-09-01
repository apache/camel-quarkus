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
package org.apache.camel.quarkus.component.opensearch.it;

import java.util.HashMap;
import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

public class OpensearchTestResource implements QuarkusTestResourceLifecycleManager {

    private static final Logger LOG = Logger.getLogger(OpensearchTestResource.class);

    private GenericContainer<?> container;

    private static final String OPENSEARCH_IMAGE = ConfigProvider.getConfig().getValue("opensearch.container.image",
            String.class);
    private static final int OPENSEARCH_PORT = 9200;

    @SuppressWarnings("resource")
    @Override
    public Map<String, String> start() {
        try {
            container = new GenericContainer<>(DockerImageName.parse(OPENSEARCH_IMAGE))
                    .withEnv("discovery.type", "single-node")
                    .withExposedPorts(OPENSEARCH_PORT)
                    .withEnv("OPENSEARCH_JAVA_OPTS", "-Xms512m -Xmx512m")
                    .withEnv("plugins.security.disabled", "true");
            container.start();

            String address = container.getHost() + ":" + container.getMappedPort(OPENSEARCH_PORT);
            Map<String, String> config = new HashMap<>();
            config.put("camel.component.opensearch.host-addresses", address);
            config.put("camel.component.opensearch.enable-sniffer", "false");

            return config;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void stop() {
        if (container != null) {
            container.stop();

        }
    }

}
