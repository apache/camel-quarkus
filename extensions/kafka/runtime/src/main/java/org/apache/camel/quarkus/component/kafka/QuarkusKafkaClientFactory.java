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

import java.util.Map;
import java.util.Properties;

import org.apache.camel.component.kafka.DefaultKafkaClientFactory;
import org.apache.camel.component.kafka.KafkaConfiguration;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;

/**
 * Custom {@link org.apache.camel.component.kafka.KafkaClientFactory} to enable Kafka configuration properties
 * discovered by the Quarkus Kubernetes Service Binding extension to be merged with those configured from
 * the Camel Kafka component and endpoint URI options.
 */
public class QuarkusKafkaClientFactory extends DefaultKafkaClientFactory {

    private final Map<String, Object> quarkusKafkaConfiguration;

    public QuarkusKafkaClientFactory(Map<String, Object> quarkusKafkaConfiguration) {
        this.quarkusKafkaConfiguration = quarkusKafkaConfiguration;
    }

    @Override
    public KafkaProducer getProducer(Properties camelKafkaProperties) {
        mergeConfiguration(camelKafkaProperties);
        return super.getProducer(camelKafkaProperties);
    }

    @Override
    public KafkaConsumer getConsumer(Properties camelKafkaProperties) {
        mergeConfiguration(camelKafkaProperties);
        return super.getConsumer(camelKafkaProperties);
    }

    @Override
    public String getBrokers(KafkaConfiguration configuration) {
        String brokers = (String) quarkusKafkaConfiguration.get(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG);
        return brokers != null ? brokers : super.getBrokers(configuration);
    }

    /**
     * Merges kafka configuration properties discovered by Quarkus with those provided via the
     * component & endpoint URI options. This behaviour can be suppressed via a configuration property.
     */
    public void mergeConfiguration(Properties camelKafkaProperties) {
        if (quarkusKafkaConfiguration != null) {
            for (Map.Entry<String, Object> entry : quarkusKafkaConfiguration.entrySet()) {
                camelKafkaProperties.put(entry.getKey(), entry.getValue());
            }
        }
    }
}
