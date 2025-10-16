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
import java.util.function.Function;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.strimzi.test.container.StrimziKafkaContainer;
import org.eclipse.microprofile.config.ConfigProvider;

public class KafkaTestResource implements QuarkusTestResourceLifecycleManager {
    protected static final String KAFKA_IMAGE_NAME = ConfigProvider.getConfig().getValue("kafka.container.image", String.class);
    private StrimziKafkaContainer container;

    @Override
    public Map<String, String> start() {
        try {
            start(name -> new StrimziKafkaContainer(name));
            return Collections.singletonMap("camel.component.kafka.brokers", container.getBootstrapServers());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String start(Function<String, StrimziKafkaContainer> containerSupplier) {
        container = containerSupplier.apply(KAFKA_IMAGE_NAME);
        container.withLogConsumer(frame -> System.out.print(frame.getUtf8String()))
                .waitForRunning()
                .start();
        return container.getBootstrapServers();
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
                new TestInjector.AnnotatedAndMatchesType(InjectKafka.class, StrimziKafkaContainer.class));
    }

}
