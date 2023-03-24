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
package org.apache.camel.quarkus.component.kafka.deployment;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Stream;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.IsNormal;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.DevServicesLauncherConfigResultBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigurationDefaultBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.dev.devservices.GlobalDevServicesConfig;
import io.quarkus.kafka.client.deployment.KafkaBuildTimeConfig;
import org.apache.camel.quarkus.component.kafka.KafkaClientFactoryProducer;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

class KafkaProcessor {
    private static final String FEATURE = "camel-kafka";
    private static final String CAMEL_KAFKA_BROKERS = "camel.component.kafka.brokers";
    private static final String KAFKA_BOOTSTRAP_SERVERS = "kafka.bootstrap.servers";
    private static final DotName[] KAFKA_CLIENTS_TYPES = {
            DotName.createSimple("org.apache.kafka.clients.producer.Producer"),
            DotName.createSimple("org.apache.kafka.clients.consumer.Consumer")
    };

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    void createKafkaClientFactoryProducerBean(
            Capabilities capabilities,
            BuildProducer<AdditionalBeanBuildItem> additionalBean) {
        if (capabilities.isPresent(Capability.KUBERNETES_SERVICE_BINDING)) {
            additionalBean.produce(AdditionalBeanBuildItem.unremovableOf(KafkaClientFactoryProducer.class));
        }
    }

    @BuildStep(onlyIfNot = IsNormal.class, onlyIf = GlobalDevServicesConfig.Enabled.class)
    public void configureKafkaComponentForDevServices(
            DevServicesLauncherConfigResultBuildItem devServiceResult,
            KafkaBuildTimeConfig kafkaBuildTimeConfig,
            BuildProducer<RunTimeConfigurationDefaultBuildItem> runTimeConfig) {

        Config config = ConfigProvider.getConfig();
        Optional<String> brokers = config.getOptionalValue(CAMEL_KAFKA_BROKERS, String.class);

        if (brokers.isEmpty() && kafkaBuildTimeConfig.devservices.enabled.orElse(true)) {
            String kafkaBootstrapServers = devServiceResult.getConfig().get(KAFKA_BOOTSTRAP_SERVERS);
            if (kafkaBootstrapServers != null) {
                runTimeConfig.produce(new RunTimeConfigurationDefaultBuildItem(CAMEL_KAFKA_BROKERS, kafkaBootstrapServers));
            }
        }
    }

    @BuildStep
    public void reflectiveClasses(CombinedIndexBuildItem combinedIndex,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        IndexView index = combinedIndex.getIndex();

        Stream.of(KAFKA_CLIENTS_TYPES)
                .map(index::getAllKnownImplementors)
                .flatMap(Collection::stream)
                .map(ClassInfo::toString)
                .forEach(name -> reflectiveClass
                        .produce(ReflectiveClassBuildItem.builder(name).fields().build()));

        reflectiveClass
                .produce(ReflectiveClassBuildItem.builder("org.apache.kafka.clients.producer.internals.Sender")
                        .fields().build());
    }
}
