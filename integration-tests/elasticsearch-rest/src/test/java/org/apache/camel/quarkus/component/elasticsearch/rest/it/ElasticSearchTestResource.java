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
package org.apache.camel.quarkus.component.elasticsearch.rest.it;

import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.apache.camel.util.CollectionHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.TestcontainersConfiguration;

public class ElasticSearchTestResource implements QuarkusTestResourceLifecycleManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ElasticSearchTestResource.class);
    private static final String ELASTICSEARCH_IMAGE = "elasticsearch:7.8.0";
    private static final int ELASTICSEARCH_PORT = 9200;

    private GenericContainer container;

    @Override
    public Map<String, String> start() {
        LOGGER.info(TestcontainersConfiguration.getInstance().toString());

        try {
            container = new GenericContainer(ELASTICSEARCH_IMAGE)
                    .withExposedPorts(ELASTICSEARCH_PORT)
                    .withLogConsumer(new Slf4jLogConsumer(LOGGER))
                    .withEnv("discovery.type", "single-node")
                    .waitingFor(Wait.forListeningPort());

            container.start();

            String hostAddresses = String.format("localhost:%s", container.getMappedPort(ELASTICSEARCH_PORT));

            // Create 2 sets of configuration to test scenarios:
            // - Quarkus ElasticSearch REST client bean being autowired into the Camel ElasticSearch REST component
            // - Component configuration where the ElasticSearch REST client is managed by Camel (E.g autowiring disabled)
            return CollectionHelper.mapOf(
                    // quarkus
                    "quarkus.elasticsearch.hosts", hostAddresses,
                    // camel
                    "camel.component.elasticsearch-rest.autowired-enabled", "false",
                    "camel.component.elasticsearch-rest.host-addresses", hostAddresses);

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
