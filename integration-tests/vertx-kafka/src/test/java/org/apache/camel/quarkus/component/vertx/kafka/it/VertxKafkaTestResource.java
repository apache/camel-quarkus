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
package org.apache.camel.quarkus.component.vertx.kafka.it;

import java.util.Collections;
import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

public class VertxKafkaTestResource implements QuarkusTestResourceLifecycleManager {

    private static final String KAFKA_IMAGE_NAME = "confluentinc/cp-kafka:5.4.3";
    private KafkaContainer container;

    @Override
    public Map<String, String> start() {
        try {
            DockerImageName imageName = DockerImageName.parse(KAFKA_IMAGE_NAME);

            container = new KafkaContainer(imageName)
                    .withEmbeddedZookeeper()
                    .waitingFor(Wait.forListeningPort());

            container.start();

            return Collections.singletonMap("camel.component.vertx-kafka.bootstrap-servers", container.getBootstrapServers());
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
}
