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

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.InspectContainerResponse;
import io.strimzi.test.container.StrimziKafkaContainer;
import org.apache.camel.quarkus.test.support.kafka.KafkaTestResource;
import org.apache.camel.util.CollectionHelper;
import org.apache.commons.io.FileUtils;
import org.apache.kafka.clients.CommonClientConfigs;
import org.testcontainers.utility.MountableFile;

public class KafkaSaslTestResource extends KafkaTestResource {
    static final String KAFKA_BROKER_HOSTNAME = "broker-1";

    private Path serviceBindingDir;
    private SaslKafkaContainer container;

    @Override
    public Map<String, String> start() {
        // Set up the service binding directory
        try {
            serviceBindingDir = Files.createTempDirectory("KafkaSaslTestResource-");
            final Path kafkaDir = serviceBindingDir.resolve("kafka");
            Files.createDirectories(kafkaDir);
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            Stream.of("password", "saslMechanism", "securityProtocol", "type", "user")
                    .forEach(fileName -> {
                        try (InputStream in = classLoader.getResourceAsStream("k8s-sb/kafka/" + fileName)) {
                            Files.copy(in, kafkaDir.resolve(fileName));
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        container = new SaslKafkaContainer(KAFKA_IMAGE_NAME);
        container.waitForRunning();
        container.start();
        return CollectionHelper.mapOf(
                "kafka." + CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, container.getBootstrapServers(),
                "quarkus.kubernetes-service-binding.root", serviceBindingDir.toString());
    }

    @Override
    public void stop() {
        if (this.container != null) {
            try {
                this.container.stop();
                FileUtils.deleteDirectory(serviceBindingDir.toFile());
            } catch (Exception e) {
                // Ignored
            }
        }
    }

    // KafkaContainer does not support SASL OOTB so we need some customizations
    static final class SaslKafkaContainer extends StrimziKafkaContainer {
        SaslKafkaContainer(final String dockerImageName) {
            super(dockerImageName);
        }

        @Override
        public String getBootstrapServers() {
            return String.format("SASL_PLAINTEXT://%s:%s", getHost(), getMappedPort(KAFKA_PORT));
        }

        @Override
        protected void configure() {
            super.configure();

            String protocolMap = "SASL_PLAINTEXT:SASL_PLAINTEXT,BROKER1:PLAINTEXT,CONTROLLER:PLAINTEXT";
            Map<String, String> config = Map.ofEntries(
                    Map.entry("inter.broker.listener.name", "BROKER1"),
                    Map.entry("listener.security.protocol.map", protocolMap),
                    Map.entry("zookeeper.sasl.enabled", "false"),
                    Map.entry("sasl.enabled.mechanisms", "PLAIN"),
                    Map.entry("sasl.mechanism.inter.broker.protocol", "PLAIN"));

            withEnv("KAFKA_OPTS", "-Djava.security.auth.login.config=/etc/kafka/kafka_server_jaas.conf");
            withCreateContainerCmdModifier(new Consumer<CreateContainerCmd>() {
                @Override
                public void accept(CreateContainerCmd createContainerCmd) {
                    createContainerCmd.withName(KAFKA_BROKER_HOSTNAME);
                    createContainerCmd.withHostName(KAFKA_BROKER_HOSTNAME);
                }
            });
            withBrokerId(1);
            withNodeId(1);
            withKafkaConfigurationMap(config);
            withLogConsumer(frame -> System.out.print(frame.getUtf8String()));
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
