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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Stream;

import com.github.dockerjava.api.command.InspectContainerResponse;
import io.strimzi.test.container.StrimziKafkaContainer;
import org.apache.camel.quarkus.test.support.kafka.KafkaTestResource;
import org.apache.camel.quarkus.test.support.kafka.KafkaTestSupport;
import org.apache.camel.util.CollectionHelper;
import org.apache.commons.io.FileUtils;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.utility.MountableFile;

import static io.strimzi.test.container.StrimziZookeeperContainer.ZOOKEEPER_PORT;

public class KafkaSaslSslTestResource extends KafkaTestResource {
    private static final String KAFKA_KEYSTORE_FILE = "kafka-keystore.p12";
    private static final String KAFKA_KEYSTORE_PASSWORD = "kafkas3cret";
    private static final String KAFKA_KEYSTORE_TYPE = "PKCS12";
    private static final String KAFKA_TRUSTSTORE_FILE = "kafka-truststore.p12";
    private static final String KAFKA_CERTIFICATE_SCRIPT = "generate-certificates.sh";
    private static Path configDir;
    private SaslSslKafkaContainer container;

    @Override
    public Map<String, String> start() {
        try {
            configDir = Files.createTempDirectory("KafkaSaslSslTestResource-");
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            Stream.of("kafka_server_jaas.conf", KAFKA_KEYSTORE_FILE, KAFKA_TRUSTSTORE_FILE)
                    .forEach(fileName -> {
                        try (InputStream in = classLoader.getResourceAsStream("config/" + fileName)) {
                            Files.copy(in, configDir.resolve(fileName));
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        KafkaTestSupport.regenerateCertificatesForDockerHost(configDir, KAFKA_CERTIFICATE_SCRIPT, KAFKA_KEYSTORE_FILE,
                KAFKA_TRUSTSTORE_FILE);

        container = new SaslSslKafkaContainer(KAFKA_IMAGE_NAME);
        container.waitForRunning();
        container.start();

        String jaasConfig = "org.apache.kafka.common.security.scram.ScramLoginModule required "
                + "username=\"alice\" "
                + "password=\"alice-secret\";";

        return CollectionHelper.mapOf(
                "camel.component.kafka.brokers", container.getBootstrapServers(),
                "camel.component.kafka.sasl-mechanism", "SCRAM-SHA-512",
                "camel.component.kafka.sasl-jaas-config", jaasConfig,
                "camel.component.kafka.security-protocol", "SASL_SSL",
                "camel.component.kafka.ssl-key-password", KAFKA_KEYSTORE_PASSWORD,
                "camel.component.kafka.ssl-keystore-location", configDir.resolve(KAFKA_KEYSTORE_FILE).toString(),
                "camel.component.kafka.ssl-keystore-password", KAFKA_KEYSTORE_PASSWORD,
                "camel.component.kafka.ssl-keystore-type", KAFKA_KEYSTORE_TYPE,
                "camel.component.kafka.ssl-truststore-location", configDir.resolve(KAFKA_TRUSTSTORE_FILE).toString(),
                "camel.component.kafka.ssl-truststore-password", KAFKA_KEYSTORE_PASSWORD,
                "camel.component.kafka.ssl-truststore-type", KAFKA_KEYSTORE_TYPE);
    }

    @Override
    public void stop() {
        if (this.container != null) {
            try {
                this.container.stop();
                FileUtils.deleteDirectory(configDir.toFile());
            } catch (Exception e) {
                // Ignored
            }
        }
    }

    // KafkaContainer does not support SASL SSL OOTB so we need some customizations
    static final class SaslSslKafkaContainer extends StrimziKafkaContainer {
        SaslSslKafkaContainer(final String dockerImageName) {
            super(dockerImageName);
        }

        @Override
        public String getBootstrapServers() {
            return String.format("SASL_SSL://%s:%s", getHost(), getMappedPort(KAFKA_PORT));
        }

        @Override
        protected void configure() {
            super.configure();

            String protocolMap = "SASL_SSL:SASL_SSL,BROKER1:PLAINTEXT";
            Map<String, String> config = Map.ofEntries(
                    Map.entry("inter.broker.listener.name", "BROKER1"),
                    Map.entry("listener.security.protocol.map", protocolMap),
                    Map.entry("zookeeper.sasl.enabled", "false"),
                    Map.entry("sasl.enabled.mechanisms", "SCRAM-SHA-512"),
                    Map.entry("sasl.mechanism.inter.broker.protocol", "SCRAM-SHA-512"),
                    Map.entry("ssl.keystore.location", "/etc/kafka/secrets/" + KAFKA_KEYSTORE_FILE),
                    Map.entry("ssl.keystore.password", KAFKA_KEYSTORE_PASSWORD),
                    Map.entry("ssl.keystore.type", KAFKA_KEYSTORE_TYPE),
                    Map.entry("ssl.truststore.location", "/etc/kafka/secrets/" + KAFKA_TRUSTSTORE_FILE),
                    Map.entry("ssl.truststore.password", KAFKA_KEYSTORE_PASSWORD),
                    Map.entry("ssl.truststore.type", KAFKA_KEYSTORE_TYPE),
                    Map.entry("ssl.endpoint.identification.algorithm", ""));

            withEnv("KAFKA_OPTS", "-Djava.security.auth.login.config=/etc/kafka/kafka_server_jaas.conf");
            withBrokerId(1);
            withKafkaConfigurationMap(config);
            withLogConsumer(frame -> System.out.print(frame.getUtf8String()));
        }

        @Override
        protected void containerIsStarting(InspectContainerResponse containerInfo, boolean reused) {
            super.containerIsStarting(containerInfo, reused);
            copyFileToContainer(
                    MountableFile.forClasspathResource("config/kafka_server_jaas.conf"),
                    "/etc/kafka/kafka_server_jaas.conf");

            Stream.of(KAFKA_KEYSTORE_FILE, KAFKA_TRUSTSTORE_FILE)
                    .forEach(keyStoreFile -> {
                        try {
                            copyFileToContainer(Transferable.of(Files.readAllBytes(configDir.resolve(keyStoreFile))),
                                    "/etc/kafka/secrets/" + keyStoreFile);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });

            String setupUsersScript = "#!/bin/bash\n"
                    + "KAFKA_OPTS= /opt/kafka/bin/kafka-configs.sh --zookeeper localhost:" + ZOOKEEPER_PORT
                    + " --alter --add-config 'SCRAM-SHA-512=[iterations=8192,password=alice-secret]' --entity-type users --entity-name alice";

            copyFileToContainer(
                    Transferable.of(setupUsersScript.getBytes(StandardCharsets.UTF_8), 0775),
                    "/setup-users.sh");
        }

        @Override
        protected void containerIsStarted(InspectContainerResponse containerInfo) {
            super.containerIsStarted(containerInfo);
            try {
                execInContainer("/setup-users.sh");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
