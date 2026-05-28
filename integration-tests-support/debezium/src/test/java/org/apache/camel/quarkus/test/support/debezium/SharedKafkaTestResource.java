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
package org.apache.camel.quarkus.test.support.debezium;

import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.strimzi.test.container.StrimziKafkaCluster;
import io.strimzi.test.container.StrimziKafkaContainer;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;

public class SharedKafkaTestResource implements QuarkusTestResourceLifecycleManager {

    private static final Logger LOG = Logger.getLogger(SharedKafkaTestResource.class);
    private static final String KAFKA_IMAGE_NAME = ConfigProvider.getConfig().getValue("kafka.container.image",
            String.class);

    private StrimziKafkaCluster kafkaCluster;

    @Override
    public Map<String, String> start() {
        LOG.info("Starting shared Kafka cluster");
        kafkaCluster = new StrimziKafkaCluster.StrimziKafkaClusterBuilder()
                .withImage(KAFKA_IMAGE_NAME)
                .build();
        kafkaCluster.start();
        StrimziKafkaContainer kafkaContainer = kafkaCluster.getBrokers().stream().findFirst()
                .orElseThrow(() -> new RuntimeException("No Kafka broker available"));
        LOG.infof("Shared Kafka cluster started with bootstrap servers: %s", kafkaContainer.getBootstrapServers());
        return Map.of("kafka.bootstrap.servers", kafkaContainer.getBootstrapServers());
    }

    @Override
    public void stop() {
        try {
            if (kafkaCluster != null) {
                LOG.info("Stopping shared Kafka cluster");
                kafkaCluster.stop();
            }
        } catch (Exception e) {
            LOG.warn("Error stopping shared Kafka cluster", e);
        }
    }
}
