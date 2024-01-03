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
package org.apache.camel.quarkus.kafka.ssl;

import java.io.IOException;
import java.io.InputStream;
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
import org.apache.kafka.clients.CommonClientConfigs;
import org.testcontainers.images.builder.Transferable;

public class KafkaSslTestResource extends KafkaTestResource {
    private static final String KAFKA_KEYSTORE_FILE = "kafka-keystore.p12";
    private static final String KAFKA_KEYSTORE_PASSWORD = "kafkas3cret";
    private static final String KAFKA_KEYSTORE_TYPE = "PKCS12";
    private static final String KAFKA_TRUSTSTORE_FILE = "kafka-truststore.p12";
    private static final String KAFKA_CERTIFICATE_SCRIPT = "generate-certificates.sh";
    private static Path configDir;
    private SSLKafkaContainer container;

    @Override
    public Map<String, String> start() {
        try {
            configDir = Files.createTempDirectory("KafkaSaslSslTestResource-");
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            Stream.of(KAFKA_KEYSTORE_FILE, KAFKA_TRUSTSTORE_FILE)
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

        container = new SSLKafkaContainer(KAFKA_IMAGE_NAME);
        container.waitForRunning();
        container.start();

        return CollectionHelper.mapOf(
                "kafka." + CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, container.getBootstrapServers(),
                "camel.component.kafka.brokers", container.getBootstrapServers(),
                "camel.component.kafka.security-protocol", "SSL",
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

    // KafkaContainer does not support SSL OOTB so we need some customizations
    static final class SSLKafkaContainer extends StrimziKafkaContainer {
        SSLKafkaContainer(final String dockerImageName) {
            super(dockerImageName);
        }

        @Override
        public String getBootstrapServers() {
            return String.format("SSL://%s:%s", getHost(), getMappedPort(KAFKA_PORT));
        }

        @Override
        protected void configure() {
            super.configure();

            String protocolMap = "SSL:SSL,BROKER1:PLAINTEXT";
            Map<String, String> config = Map.ofEntries(
                    Map.entry("inter.broker.listener.name", "BROKER1"),
                    Map.entry("listener.security.protocol.map", protocolMap),
                    Map.entry("ssl.keystore.location", "/etc/kafka/secrets/" + KAFKA_KEYSTORE_FILE),
                    Map.entry("ssl.keystore.password", KAFKA_KEYSTORE_PASSWORD),
                    Map.entry("ssl.keystore.type", KAFKA_KEYSTORE_TYPE),
                    Map.entry("ssl.truststore.location", "/etc/kafka/secrets/" + KAFKA_TRUSTSTORE_FILE),
                    Map.entry("ssl.truststore.password", KAFKA_KEYSTORE_PASSWORD),
                    Map.entry("ssl.truststore.type", KAFKA_KEYSTORE_TYPE),
                    Map.entry("ssl.endpoint.identification.algorithm", ""));

            withBrokerId(1);
            withKafkaConfigurationMap(config);
            withLogConsumer(frame -> System.out.print(frame.getUtf8String()));
        }

        @Override
        protected void containerIsStarting(InspectContainerResponse containerInfo, boolean reused) {
            super.containerIsStarting(containerInfo, reused);

            Stream.of(KAFKA_KEYSTORE_FILE, KAFKA_TRUSTSTORE_FILE)
                    .forEach(keyStoreFile -> {
                        try {
                            copyFileToContainer(Transferable.of(Files.readAllBytes(configDir.resolve(keyStoreFile))),
                                    "/etc/kafka/secrets/" + keyStoreFile);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
        }
    }
}
