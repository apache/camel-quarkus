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

import java.nio.file.Path;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

import com.github.dockerjava.api.command.CreateContainerCmd;
import io.strimzi.test.container.StrimziKafkaCluster;
import org.apache.camel.quarkus.test.support.kafka.KafkaTestResource;
import org.apache.camel.util.CollectionHelper;
import org.apache.kafka.clients.CommonClientConfigs;
import org.testcontainers.utility.MountableFile;

import static io.strimzi.test.container.StrimziKafkaContainer.KAFKA_PORT;

public class KafkaSslTestResource extends KafkaTestResource {

    static final String KAFKA_KEYSTORE_PASSWORD = "changeit";
    static final String KAFKA_HOSTNAME = "localhost";
    static final String KAFKA_BROKER_HOSTNAME = "broker-1";
    static final String CERTS_BASEDIR = "target/certs";

    static final String KAFKA_KEYSTORE_FILE = KAFKA_HOSTNAME + "-keystore.p12";
    static final String KAFKA_KEYSTORE_TYPE = "PKCS12";
    static final String KAFKA_TRUSTSTORE_FILE = KAFKA_HOSTNAME + "-truststore.p12";

    @Override
    public Map<String, String> start() {

        String bootstrapServers = start(name -> {
            String protocolMap = "SSL:SSL,BROKER1:PLAINTEXT,CONTROLLER:PLAINTEXT";
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

            return new StrimziKafkaCluster.StrimziKafkaClusterBuilder()
                    .withImage(name)
                    .withAdditionalKafkaConfiguration(config)
                    .withBootstrapServers(c -> String.format("SSL://%s:%s", c.getHost(), c.getMappedPort(KAFKA_PORT)))
                    .withContainerCustomizer(container -> {
                        container.withCreateContainerCmdModifier(new Consumer<CreateContainerCmd>() {
                            @Override
                            public void accept(CreateContainerCmd createContainerCmd) {
                                createContainerCmd.withName(KAFKA_BROKER_HOSTNAME);
                                createContainerCmd.withHostName(KAFKA_BROKER_HOSTNAME);
                            }
                        });
                        container.withLogConsumer(frame -> System.out.print(frame.getUtf8String()));
                        Stream.of(KAFKA_KEYSTORE_FILE, KAFKA_TRUSTSTORE_FILE)
                                .forEach(keyStoreFile -> {
                                    container.withCopyFileToContainer(
                                            MountableFile.forHostPath(Path.of(CERTS_BASEDIR).resolve(keyStoreFile)),
                                            "/etc/kafka/secrets/" + keyStoreFile);
                                });
                    })
                    .build();
        });

        return CollectionHelper.mapOf(
                "kafka." + CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                "camel.component.kafka.brokers", bootstrapServers,
                "camel.component.kafka.security-protocol", "SSL",
                "camel.component.kafka.ssl-key-password", KAFKA_KEYSTORE_PASSWORD,
                "camel.component.kafka.ssl-keystore-location", Path.of(CERTS_BASEDIR).resolve(KAFKA_KEYSTORE_FILE).toString(),
                "camel.component.kafka.ssl-keystore-password", KAFKA_KEYSTORE_PASSWORD,
                "camel.component.kafka.ssl-keystore-type", KAFKA_KEYSTORE_TYPE,
                "camel.component.kafka.ssl-truststore-location",
                Path.of(CERTS_BASEDIR).resolve(KAFKA_TRUSTSTORE_FILE).toString(),
                "camel.component.kafka.ssl-truststore-password", KAFKA_KEYSTORE_PASSWORD,
                "camel.component.kafka.ssl-truststore-type", KAFKA_KEYSTORE_TYPE);
    }
}
