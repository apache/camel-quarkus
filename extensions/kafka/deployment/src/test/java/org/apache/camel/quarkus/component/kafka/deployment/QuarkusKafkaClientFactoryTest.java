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

import java.util.Arrays;
import java.util.Properties;

import javax.inject.Inject;

import io.quarkus.bootstrap.model.AppArtifact;
import io.quarkus.builder.Version;
import io.quarkus.test.QuarkusUnitTest;
import org.apache.camel.component.kafka.KafkaClientFactory;
import org.apache.camel.component.kafka.KafkaConfiguration;
import org.apache.camel.quarkus.component.kafka.QuarkusKafkaClientFactory;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class QuarkusKafkaClientFactoryTest {

    @RegisterExtension
    static final QuarkusUnitTest CONFIG = new QuarkusUnitTest()
            .setForcedDependencies(Arrays.asList(
                    new AppArtifact("io.quarkus", "quarkus-kubernetes-service-binding", Version.getVersion())))
            .withConfigurationResource("application-configuration-merging.properties")
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class));

    @Inject
    KafkaClientFactory factory;

    //@Test
    public void testMergeConfiguration() {
        assertNotNull(factory);

        QuarkusKafkaClientFactory quarkusKafkaClientFactory = (QuarkusKafkaClientFactory) factory;

        KafkaConfiguration configuration = new KafkaConfiguration();
        configuration.setBrokers("camelhost:9999");
        assertEquals("localhost:9092", quarkusKafkaClientFactory.getBrokers(configuration));

        Properties properties = new Properties();
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, "camel-quarkus-group");
        properties.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, "2000");
        quarkusKafkaClientFactory.mergeConfiguration(properties);

        assertEquals("camel-quarkus-group", properties.getProperty(ConsumerConfig.GROUP_ID_CONFIG));
        assertEquals("1000",
                properties.getProperty(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG));
    }
}
