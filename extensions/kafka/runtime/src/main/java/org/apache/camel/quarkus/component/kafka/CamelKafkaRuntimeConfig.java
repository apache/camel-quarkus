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

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "camel.kafka", phase = ConfigPhase.RUN_TIME)
public final class CamelKafkaRuntimeConfig {

    /**
     * Kafka Kubernetes Service Binding configuration options
     */
    @ConfigItem(defaultValue = "true")
    public KafkaServiceBindingConfig kubernetesServiceBinding;

    @ConfigGroup
    public static final class KafkaServiceBindingConfig {

        /**
         * If {@code true} then any Kafka configuration properties discovered by the Quarkus Kubernetes Service Binding
         * extension (if configured) will be merged with those set via Camel Kafka component or endpoint options.
         *
         * If {@code false} then any Kafka configuration properties discovered by the Quarkus Kubernetes Service Binding
         * extension are ignored, and all of the Kafka component configuration is driven by Camel.
         */
        @ConfigItem(defaultValue = "true")
        public boolean mergeConfiguration;
    }
}
