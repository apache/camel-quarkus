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
package org.apache.camel.quarkus.kafka.oauth.it;

import java.util.HashMap;
import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.strimzi.test.container.StrimziKafkaContainer;
import org.apache.camel.quarkus.kafka.oauth.it.container.KeycloakContainer;
import org.jboss.logging.Logger;
import org.testcontainers.utility.MountableFile;

import static io.strimzi.test.container.StrimziKafkaContainer.KAFKA_PORT;

/**
 * Inspired from https://github.com/quarkusio/quarkus/tree/main/integration-tests/kafka-oauth-keycloak/
 */
public class KafkaKeycloakTestResource implements QuarkusTestResourceLifecycleManager {

    private static final Logger log = Logger.getLogger(KafkaKeycloakTestResource.class);
    private StrimziKafkaContainer kafka;
    private KeycloakContainer keycloak;

    @Override
    public Map<String, String> start() {
        Map<String, String> properties = new HashMap<>();

        //Start keycloak container
        keycloak = new KeycloakContainer();
        keycloak.start();
        log.info(keycloak.getLogs());
        keycloak.createHostsFile();

        //Start kafka container
        this.kafka = new StrimziKafkaContainer("quay.io/strimzi/kafka:latest-kafka-3.0.0")
                .withBrokerId(1)
                .withKafkaConfigurationMap(Map.of("listener.security.protocol.map", "JWT:SASL_PLAINTEXT,BROKER1:PLAINTEXT"))
                .withNetworkAliases("kafka")
                .withServerProperties(MountableFile.forClasspathResource("kafkaServer.properties"))
                .withBootstrapServers(
                        c -> String.format("JWT://%s:%s", c.getContainerIpAddress(), c.getMappedPort(KAFKA_PORT)));
        this.kafka.start();
        log.info(this.kafka.getLogs());
        properties.put("kafka.bootstrap.servers", this.kafka.getBootstrapServers());
        properties.put("camel.component.kafka.brokers", kafka.getBootstrapServers());

        return properties;
    }

    @Override
    public void stop() {
        if (kafka != null) {
            kafka.stop();
        }
        if (keycloak != null) {
            keycloak.stop();
        }
    }
}
