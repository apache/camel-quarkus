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
package org.apache.camel.quarkus.kafka.sasl;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;

import com.github.dockerjava.api.command.InspectContainerResponse;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.apache.kafka.clients.CommonClientConfigs;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.shaded.org.apache.commons.io.FileUtils;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

public class KafkaSaslTestResource implements QuarkusTestResourceLifecycleManager {

    private static final File TMP_DIR = Paths.get(System.getProperty("java.io.tmpdir"), "k8s-sb", "kafka").toFile();
    private SaslKafkaContainer container;

    @Override
    public Map<String, String> start() {
        // Set up the service binding directory
        try {
            TMP_DIR.mkdirs();

            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            URL resource = classLoader.getResource("k8s-sb/kafka");
            File serviceBindings = new File(resource.getPath());

            for (File serviceBinding : serviceBindings.listFiles()) {
                URL serviceBindingResource = classLoader.getResource("k8s-sb/kafka/" + serviceBinding.getName());
                FileUtils.copyInputStreamToFile(serviceBindingResource.openStream(),
                        Paths.get(TMP_DIR.getPath(), serviceBinding.getName()).toFile());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        DockerImageName imageName = DockerImageName.parse("confluentinc/cp-kafka").withTag("5.4.3");
        container = new SaslKafkaContainer(imageName);
        container.start();
        return Collections.singletonMap("kafka." + CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG,
                container.getBootstrapServers());
    }

    @Override
    public void stop() {
        if (this.container != null) {
            try {
                this.container.stop();
                FileUtils.deleteDirectory(TMP_DIR.getParentFile());
            } catch (Exception e) {
                // Ignored
            }
        }
    }

    // KafkaContainer does not support SASL OOTB so we need some customizations
    static final class SaslKafkaContainer extends KafkaContainer {

        SaslKafkaContainer(final DockerImageName dockerImageName) {
            super(dockerImageName);

            String protocolMap = "SASL_PLAINTEXT:SASL_PLAINTEXT,BROKER:SASL_PLAINTEXT";
            String listeners = "SASL_PLAINTEXT://0.0.0.0:" + KAFKA_PORT + ",BROKER://0.0.0.0:9092";

            withEnv("KAFKA_OPTS", "-Djava.security.auth.login.config=/etc/kafka/kafka_server_jaas.conf");
            withEnv("KAFKA_LISTENERS", listeners);
            withEnv("KAFKA_LISTENER_SECURITY_PROTOCOL_MAP", protocolMap);
            withEnv("KAFKA_CONFLUENT_SUPPORT_METRICS_ENABLE", "false");
            withEnv("KAFKA_SASL_ENABLED_MECHANISMS", "PLAIN");
            withEnv("ZOOKEEPER_SASL_ENABLED", "false");
            withEnv("KAFKA_INTER_BROKER_LISTENER_NAME", "BROKER");
            withEnv("KAFKA_SASL_MECHANISM_INTER_BROKER_PROTOCOL", "PLAIN");
            withEmbeddedZookeeper().waitingFor(Wait.forListeningPort());
        }

        @Override
        public String getBootstrapServers() {
            return String.format("SASL_PLAINTEXT://%s:%s", getHost(), getMappedPort(KAFKA_PORT));
        }

        @Override
        protected void containerIsStarting(InspectContainerResponse containerInfo, boolean reused) {
            super.containerIsStarting(containerInfo, reused);
            copyFileToContainer(
                    MountableFile.forClasspathResource("config/kafka_server_jaas.conf"),
                    "/etc/kafka/kafka_server_jaas.conf");
        }
    }
}
