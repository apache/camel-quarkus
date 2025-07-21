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
package org.apache.camel.quarkus.component.kafka;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import org.apache.camel.component.kafka.KafkaComponent;
import org.apache.camel.component.kafka.KafkaConfiguration;
import org.apache.camel.quarkus.core.events.ComponentAddEvent;
import org.apache.camel.util.ObjectHelper;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

@ApplicationScoped
public class KafkaComponentObserver {
    private static final String CAMEL_KAFKA_BROKERS = "camel.component.kafka.brokers";
    private static final String KAFKA_BOOTSTRAP_SERVERS = "kafka.bootstrap.servers";

    void initKafkaBrokerConfiguration(@Observes ComponentAddEvent event) {
        if (event.getComponent() instanceof KafkaComponent kafkaComponent) {
            Config config = ConfigProvider.getConfig();
            KafkaConfiguration configuration = kafkaComponent.getConfiguration();

            Optional<String> camelKafkaBrokers = config.getOptionalValue(CAMEL_KAFKA_BROKERS, String.class);
            Optional<String> kafkaBootstrapServers = config.getOptionalValue(KAFKA_BOOTSTRAP_SERVERS, String.class);
            if (camelKafkaBrokers.isEmpty() && ObjectHelper.isEmpty(configuration.getBrokers())
                    && kafkaBootstrapServers.isPresent()) {
                configuration.setBrokers(kafkaBootstrapServers.get());
            }
        }
    }
}
