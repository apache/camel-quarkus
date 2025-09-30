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
package org.apache.camel.quarkus.component.elasticsearch.it;

import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.apache.camel.util.CollectionHelper;
import org.eclipse.microprofile.config.ConfigProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;

public class ElasticsearchTestResource implements QuarkusTestResourceLifecycleManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ElasticsearchTestResource.class);
    private static final String ELASTICSEARCH_IMAGE = ConfigProvider.getConfig().getValue("elasticsearch.container.image",
            String.class);
    private static final String ELASTICSEARCH_USERNAME = "elastic";
    private static final String ELASTICSEARCH_PASSWORD = "changeme";
    private static final int ELASTICSEARCH_PORT = 9200;

    private GenericContainer<?> container;

    @Override
    public Map<String, String> start() {
        try {
            container = new GenericContainer<>(ELASTICSEARCH_IMAGE)
                    .withExposedPorts(ELASTICSEARCH_PORT)
                    .withLogConsumer(new Slf4jLogConsumer(LOGGER))
                    .withEnv("cluster.routing.allocation.disk.threshold_enabled", "false")
                    .withEnv("discovery.type", "single-node")
                    .withEnv("xpack.security.enabled", "true")
                    .withEnv("action.destructive_requires_name", "false") // needed for deleting all indexes after each test (allowing _all wildcard)
                    .withEnv("ELASTIC_USERNAME", ELASTICSEARCH_USERNAME)
                    .withEnv("ELASTIC_PASSWORD", ELASTICSEARCH_PASSWORD)
                    .withEnv("ES_JAVA_OPTS", "-Xms512m -Xmx512m")
                    .waitingFor(Wait.forListeningPort());

            container.start();

            String hostAddresses = String.format("%s:%d", container.getHost(), container.getMappedPort(ELASTICSEARCH_PORT));

            // Component configuration where the ElasticSearch client is managed by Camel (E.g autowiring disabled)
            return CollectionHelper.mapOf(
                    // camel
                    "camel.component.elasticsearch.host-addresses", hostAddresses,
                    "camel.component.elasticsearch.user", ELASTICSEARCH_USERNAME,
                    "camel.component.elasticsearch.password", ELASTICSEARCH_PASSWORD);

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
            // Ignored
        }
    }
}
