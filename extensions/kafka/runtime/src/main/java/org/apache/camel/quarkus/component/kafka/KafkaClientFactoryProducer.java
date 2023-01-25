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
import java.util.Optional;

import io.quarkus.arc.DefaultBean;
import io.smallrye.common.annotation.Identifier;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.apache.camel.component.kafka.KafkaClientFactory;
import org.eclipse.microprofile.config.Config;

@Singleton
public class KafkaClientFactoryProducer {

    @Inject
    @Identifier("default-kafka-broker")
    Map<String, Object> kafkaConfig;

    @Inject
    CamelKafkaRuntimeConfig camelKafkaRuntimeConfig;

    @Inject
    Config config;

    @Produces
    @Singleton
    @DefaultBean
    public KafkaClientFactory kafkaClientFactory() {
        if (isQuarkusKafkaClientFactoryRequired()) {
            return new QuarkusKafkaClientFactory(kafkaConfig);
        }
        return null;
    }

    private boolean isQuarkusKafkaClientFactoryRequired() {
        Optional<Boolean> serviceBindingEnabled = config.getOptionalValue(
                "quarkus.kubernetes-service-binding.enabled",
                Boolean.class);
        return kafkaConfig != null
                && !kafkaConfig.isEmpty()
                && camelKafkaRuntimeConfig.kubernetesServiceBinding.mergeConfiguration
                && serviceBindingEnabled.isPresent()
                && serviceBindingEnabled.get();
    }
}
