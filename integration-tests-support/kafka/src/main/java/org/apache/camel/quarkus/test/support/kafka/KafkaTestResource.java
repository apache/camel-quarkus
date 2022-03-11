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
package org.apache.camel.quarkus.test.support.kafka;

import java.util.Collections;
import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.TestcontainersConfiguration;

public class KafkaTestResource implements QuarkusTestResourceLifecycleManager {

    protected static final DockerImageName KAFKA_IMAGE_NAME = DockerImageName.parse("confluentinc/cp-kafka").withTag("5.4.3");
    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaTestResource.class);

    private KafkaContainer container;

    @Override
    public Map<String, String> start() {
        LOGGER.info(TestcontainersConfiguration.getInstance().toString());

        try {
            container = new KafkaContainer(KAFKA_IMAGE_NAME)
                    /* Added container startup logging because of https://github.com/apache/camel-quarkus/issues/2461 */
                    .withLogConsumer(frame -> System.out.print(frame.getUtf8String()))
                    .withEmbeddedZookeeper()
                    .waitingFor(Wait.forListeningPort());

            container.start();

            return Collections.singletonMap("camel.component.kafka.brokers", container.getBootstrapServers());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void stop() {
        if (container != null) {
            try {
                container.stop();
            } catch (Exception e) {
                // ignored
            }
        }
    }

    @Override
    public void inject(TestInjector testInjector) {
        testInjector.injectIntoFields(container,
                new TestInjector.AnnotatedAndMatchesType(InjectKafka.class, KafkaContainer.class));
    }

}
