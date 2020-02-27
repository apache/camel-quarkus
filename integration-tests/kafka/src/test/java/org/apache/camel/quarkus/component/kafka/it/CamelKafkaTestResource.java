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
package org.apache.camel.quarkus.component.kafka.it;

import java.util.Collections;
import java.util.Map;

import org.apache.camel.quarkus.core.CamelMain;
import org.apache.camel.quarkus.testcontainers.ContainerResourceLifecycleManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.TestcontainersConfiguration;

public class CamelKafkaTestResource implements ContainerResourceLifecycleManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(CamelKafkaTestResource.class);
    private static final String CONFLUENT_PLATFORM_VERSION = "5.3.1";

    private KafkaContainer container;
    private CamelMain main;

    @Override
    public void inject(Object testInstance) {
        if (testInstance instanceof CamelKafkaTest) {
            this.main = ((CamelKafkaTest) testInstance).main;
        }
    }

    @Override
    public Map<String, String> start() {
        LOGGER.info(TestcontainersConfiguration.getInstance().toString());

        try {
            container = new KafkaContainer(CONFLUENT_PLATFORM_VERSION)
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
        if (main != null) {
            try {
                main.stop();
            } catch (Exception e) {
                // ignored
            }
        }
        if (container != null) {
            try {
                container.stop();
            } catch (Exception e) {
                // ignored
            }
        }
    }
}
