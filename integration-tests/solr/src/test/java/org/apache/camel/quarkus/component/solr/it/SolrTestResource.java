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
package org.apache.camel.quarkus.component.solr.it;

import java.nio.file.Paths;
import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.eclipse.microprofile.config.ConfigProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.SolrContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import static org.apache.camel.quarkus.component.solr.it.SolrProducers.KEYSTORE_PASSWORD;

public class SolrTestResource implements QuarkusTestResourceLifecycleManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(SolrTestResource.class);
    private static final String COLLECTION_NAME = "test-collection";
    private static final DockerImageName SOLR_IMAGE = DockerImageName
            .parse(ConfigProvider.getConfig().getValue("solr.container.image", String.class))
            .asCompatibleSubstituteFor("solr");

    private SolrContainer container;

    @Override
    public Map<String, String> start() {
        container = new SolrContainer(SOLR_IMAGE)
                .withCollection(COLLECTION_NAME)
                .withZookeeper(false)
                .withCopyFileToContainer(MountableFile.forHostPath(Paths.get("target/certs/solr-keystore.p12")),
                        "/ssl/solr-keystore.p12")
                .withCopyFileToContainer(MountableFile.forHostPath(Paths.get("target/certs/solr-truststore.p12")),
                        "/ssl/solr-truststore.p12")
                .withEnv("SOLR_SSL_ENABLED", "true")
                .withEnv("SOLR_SSL_KEY_STORE", "/ssl/solr-keystore.p12")
                .withEnv("SOLR_SSL_KEY_STORE_PASSWORD", KEYSTORE_PASSWORD)
                .withEnv("SOLR_SSL_TRUST_STORE", "/ssl/solr-truststore.p12")
                .withEnv("SOLR_SSL_TRUST_STORE_PASSWORD", KEYSTORE_PASSWORD)
                .withEnv("SOLR_SSL_NEED_CLIENT_AUTH", "false")
                .withEnv("SOLR_SSL_WANT_CLIENT_AUTH", "false")
                .withEnv("SOLR_SSL_CHECK_PEER_NAME", "true")
                .withEnv("SOLR_SSL_KEY_STORE_TYPE", "PKCS12")
                .withEnv("SOLR_SSL_TRUST_STORE_TYPE", "PKCS12")
                .withLogConsumer(new Slf4jLogConsumer(LOGGER));

        container.start();
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        return Map.of("camel.component.solr.default-collection", COLLECTION_NAME,
                "solr.host", "https://%s:%d/solr".formatted(container.getHost(), container.getSolrPort()));
    }

    @Override
    public void stop() {
        if (container != null) {
            container.stop();
        }
    }
}
