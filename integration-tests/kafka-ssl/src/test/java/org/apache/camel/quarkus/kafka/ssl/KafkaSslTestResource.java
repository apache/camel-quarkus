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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Stream;

import com.github.dockerjava.api.command.InspectContainerResponse;
import org.apache.camel.quarkus.test.support.kafka.KafkaTestResource;
import org.apache.camel.util.CollectionHelper;
import org.apache.commons.io.FileUtils;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

public class KafkaSslTestResource extends KafkaTestResource {

    private static final String KAFKA_KEYSTORE_FILE = "kafka-keystore.p12";
    private static final String KAFKA_KEYSTORE_PASSWORD = "kafkas3cret";
    private static final String KAFKA_KEYSTORE_TYPE = "PKCS12";
    private static final String KAFKA_SSL_CREDS_FILE = "broker-creds";
    private static final String KAFKA_TRUSTSTORE_FILE = "kafka-truststore.p12";
    private Path configDir;
    private SSLKafkaContainer container;

    @Override
    public Map<String, String> start() {
        // Set up the SSL key / trust store directory
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

        container = new SSLKafkaContainer(KAFKA_IMAGE_NAME);
        container.start();

        return CollectionHelper.mapOf(
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
    static final class SSLKafkaContainer extends KafkaContainer {

        SSLKafkaContainer(final DockerImageName dockerImageName) {
            super(dockerImageName);

            String protocolMap = "SSL:SSL,BROKER:PLAINTEXT";
            String listeners = "SSL://0.0.0.0:" + KAFKA_PORT + ",BROKER://0.0.0.0:9092";

            withEnv("KAFKA_LISTENERS", listeners);
            withEnv("KAFKA_LISTENER_SECURITY_PROTOCOL_MAP", protocolMap);
            withEnv("KAFKA_INTER_BROKER_LISTENER_NAME", "BROKER");
            withEnv("KAFKA_SSL_KEY_CREDENTIALS", KAFKA_SSL_CREDS_FILE);
            withEnv("KAFKA_SSL_KEYSTORE_FILENAME", KAFKA_KEYSTORE_FILE);
            withEnv("KAFKA_SSL_KEYSTORE_CREDENTIALS", KAFKA_SSL_CREDS_FILE);
            withEnv("KAFKA_SSL_KEYSTORE_TYPE", KAFKA_KEYSTORE_TYPE);
            withEnv("KAFKA_SSL_TRUSTSTORE_FILENAME", KAFKA_TRUSTSTORE_FILE);
            withEnv("KAFKA_SSL_TRUSTSTORE_CREDENTIALS", KAFKA_SSL_CREDS_FILE);
            withEnv("KAFKA_SSL_TRUSTSTORE_TYPE", KAFKA_KEYSTORE_TYPE);
            withEnv("KAFKA_SSL_ENDPOINT_IDENTIFICATION_ALGORITHM", "");
            withEnv("KAFKA_CONFLUENT_SUPPORT_METRICS_ENABLE", "false");
            withEmbeddedZookeeper().waitingFor(Wait.forListeningPort());
            withLogConsumer(frame -> System.out.print(frame.getUtf8String()));
        }

        @Override
        public String getBootstrapServers() {
            return String.format("SSL://%s:%s", getHost(), getMappedPort(KAFKA_PORT));
        }

        @Override
        protected void configure() {
            super.configure();

            String host = getNetwork() != null ? getNetworkAliases().get(0) : "localhost";
            withEnv("KAFKA_ADVERTISED_LISTENERS",
                    String.format("SSL://%s:9093,BROKER://%s:9092", host, host));
        }

        @Override
        protected void containerIsStarting(InspectContainerResponse containerInfo, boolean reused) {
            super.containerIsStarting(containerInfo, reused);
            copyFileToContainer(
                    MountableFile.forClasspathResource("config/" + KAFKA_KEYSTORE_FILE),
                    "/etc/kafka/secrets/" + KAFKA_KEYSTORE_FILE);

            copyFileToContainer(
                    MountableFile.forClasspathResource("config/" + KAFKA_TRUSTSTORE_FILE),
                    "/etc/kafka/secrets/" + KAFKA_TRUSTSTORE_FILE);

            copyFileToContainer(
                    Transferable.of(KAFKA_KEYSTORE_PASSWORD.getBytes(StandardCharsets.UTF_8)),
                    "/etc/kafka/secrets/" + KAFKA_SSL_CREDS_FILE);
        }
    }
}
