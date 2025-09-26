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
package org.apache.camel.quarkus.component.elasticsearch.rest.client.it;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.util.Base64;
import java.util.Map;

import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Ports;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.apache.camel.util.CollectionHelper;
import org.eclipse.microprofile.config.ConfigProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.Transferable;

public class ElasticsearchRestTestResource implements QuarkusTestResourceLifecycleManager {

    public static final String CERTS_BASEDIR = "target/certs";
    public static final String CERTIFICATE_NAME = "elasticsearch";
    public static final String KEYSTORE_PASSWORD = "s3cr3t";
    private static final Logger LOGGER = LoggerFactory.getLogger(ElasticsearchRestTestResource.class);
    private static final String ELASTICSEARCH_IMAGE = ConfigProvider.getConfig().getValue("elasticsearch.container.image",
            String.class);
    private static final String ELASTICSEARCH_USERNAME = "elastic";
    private static final String ELASTICSEARCH_PASSWORD = "changeme";
    private static final int ELASTICSEARCH_PORT = 9200;

    private GenericContainer<?> container;

    @Override
    public Map<String, String> start() {
        exportCertificateCAForClient();

        try {
            Network elasticSearchNetwork = Network.newNetwork();

            container = new GenericContainer<>(ELASTICSEARCH_IMAGE)
                    .withExposedPorts(ELASTICSEARCH_PORT)
                    .withLogConsumer(new Slf4jLogConsumer(LOGGER))
                    .withEnv("cluster.routing.allocation.disk.threshold_enabled", "false")
                    .withEnv("discovery.type", "single-node")
                    .withEnv("http.publish_host", DockerClientFactory.instance().dockerHostIpAddress())
                    .withEnv("xpack.security.enabled", "true")
                    .withEnv("xpack.security.transport.ssl.enabled", "true")
                    .withEnv("xpack.security.transport.ssl.verification_mode", "certificate")
                    .withEnv("xpack.security.transport.ssl.keystore.path", "certs/elasticsearch-keystore.p12")
                    .withEnv("xpack.security.transport.ssl.keystore.password", KEYSTORE_PASSWORD)
                    .withEnv("xpack.security.transport.ssl.truststore.path", "certs/elasticsearch-truststore.p12")
                    .withEnv("xpack.security.transport.ssl.truststore.password", KEYSTORE_PASSWORD)
                    .withEnv("action.destructive_requires_name", "false") // needed for deleting all indexes after each test (allowing _all wildcard)
                    .withEnv("ELASTIC_USERNAME", ELASTICSEARCH_USERNAME)
                    .withEnv("ELASTIC_PASSWORD", ELASTICSEARCH_PASSWORD)
                    .withEnv("ES_JAVA_OPTS", "-Xms512m -Xmx512m")
                    .withCopyToContainer(
                            Transferable.of(Files.readAllBytes(Paths.get("target/certs/elasticsearch-keystore.p12"))),
                            "/usr/share/elasticsearch/config/certs/elasticsearch-keystore.p12")
                    .withCopyToContainer(
                            Transferable.of(Files.readAllBytes(Paths.get("target/certs/elasticsearch-truststore.p12"))),
                            "/usr/share/elasticsearch/config/certs/elasticsearch-truststore.p12")
                    .withCreateContainerCmdModifier(createContainerCmd -> {
                        Ports portBindings = new Ports();
                        portBindings.bind(ExposedPort.tcp(ELASTICSEARCH_PORT), Ports.Binding.bindPort(ELASTICSEARCH_PORT));
                        HostConfig hostConfig = HostConfig.newHostConfig()
                                .withPortBindings(portBindings)
                                .withNetworkMode(elasticSearchNetwork.getId());
                        createContainerCmd.withHostConfig(hostConfig);
                    })
                    .waitingFor(Wait.forListeningPort());

            container.start();
            String hostAddresses = String.format("%s:%s", container.getHost(), container.getMappedPort(ELASTICSEARCH_PORT));

            return CollectionHelper.mapOf(
                    "camel.component.elasticsearch-rest-client.host-addresses-list", hostAddresses,
                    "camel.component.elasticsearch-rest-client.user", ELASTICSEARCH_USERNAME,
                    "camel.component.elasticsearch-rest-client.password", ELASTICSEARCH_PASSWORD,
                    "camel.component.elasticsearch-rest-client.certificatePath", "file:target/certs/ca.crt",
                    "camel.component.elasticsearch-rest-client.enableSniffer", "true");

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void stop() {
        try {
            if (container != null) {
                container.stop();
            }
        } catch (Exception e) {
            // Ignored
        }
    }

    private void exportCertificateCAForClient() {
        Path path = Paths.get("target/certs/elasticsearch-keystore.p12");
        File outputFile = path.getParent().resolve("ca.crt").toFile();
        try {
            KeyStore keyStore = KeyStore.getInstance("pkcs12");
            try (FileInputStream fis = new FileInputStream(path.toAbsolutePath().toString())) {
                keyStore.load(fis, KEYSTORE_PASSWORD.toCharArray());
            }

            Certificate cert = keyStore.getCertificate(CERTIFICATE_NAME);
            if (cert == null) {
                throw new IllegalStateException("Unable to find a certificate in keystore named " + CERTIFICATE_NAME);
            }

            Base64.Encoder encoder = Base64.getEncoder();
            try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(outputFile), StandardCharsets.UTF_8)) {
                writer.write("-----BEGIN CERTIFICATE-----");
                writer.write("\n");
                writer.write(encoder.encodeToString(cert.getEncoded()));
                writer.write("\n");
                writer.write("-----END CERTIFICATE-----");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
