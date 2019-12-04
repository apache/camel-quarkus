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

import java.io.File;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;

import io.debezium.kafka.KafkaCluster;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.apache.camel.quarkus.core.CamelMain;
import org.apache.camel.quarkus.test.AvailablePortFinder;

public class CamelKafkaTestResource implements QuarkusTestResourceLifecycleManager {
    private KafkaCluster kafka;
    private CamelMain main;

    @Override
    public void inject(Object testInstance) {
        if (testInstance instanceof CamelKafkaTest) {
            this.main = ((CamelKafkaTest) testInstance).main;
        }
    }

    @Override
    public Map<String, String> start() {
        try {
            final int zkPort = AvailablePortFinder.getNextAvailable();
            final int kafkaPort = AvailablePortFinder.getNextAvailable();
            final File directory = Files.createTempDirectory("kafka-data-").toFile();

            Properties props = new Properties();
            props.setProperty("zookeeper.connection.timeout.ms", "45000");

            kafka = new KafkaCluster()
                    .withPorts(zkPort, kafkaPort)
                    .addBrokers(1)
                    .usingDirectory(directory)
                    .deleteDataUponShutdown(true)
                    .withKafkaConfiguration(props)
                    .deleteDataPriorToStartup(true)
                    .startup();

            return Collections.singletonMap("camel.component.kafka.brokers", "localhost:" + kafkaPort);
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
        if (kafka != null) {
            try {
                kafka.shutdown();
            } catch (Exception e) {
                // ignored
            }
        }
    }
}
