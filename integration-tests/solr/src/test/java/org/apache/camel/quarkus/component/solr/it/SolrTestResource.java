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

import java.io.File;
import java.net.URI;
import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.apache.camel.util.CollectionHelper;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.SolrContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

public class SolrTestResource implements QuarkusTestResourceLifecycleManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(SolrTestResource.class);

    private static final DockerImageName SOLR_IMAGE = DockerImageName.parse("solr:8.7.0");
    private static final String COLLECTION_NAME = "collection1";
    private static final String URL_FORMAT = "localhost:%s/solr/collection1";
    private static final String CLOUD_COMPONENT_URL_FORMAT = "localhost:%s/solr?zkHost=localhost:%s&collection=collection1&username=solr&password=SolrRocks";
    private static final int ZOOKEEPER_PORT = 2181;
    private static final int SOLR_PORT = 8983;

    private SolrContainer standaloneContainer;
    private SolrContainer sslContainer;
    private DockerComposeContainer cloudContainer;

    @Override
    public Map<String, String> start() {
        // creates 3 containers for 3 different modes of using SOLR
        createContainers();
        // start containers
        startContainers(cloudContainer, standaloneContainer, sslContainer);
        // return custom URLs
        return CollectionHelper.mapOf("solr.standalone.url", String.format(URL_FORMAT, standaloneContainer.getSolrPort()),
                "solr.ssl.url", String.format(URL_FORMAT, sslContainer.getSolrPort()),
                "solr.cloud.url", String.format(URL_FORMAT, cloudContainer.getServicePort("solr1", SOLR_PORT)),
                "solr.cloud.component.url", String.format(CLOUD_COMPONENT_URL_FORMAT,
                        cloudContainer.getServicePort("solr1", SOLR_PORT),
                        cloudContainer.getServicePort("zoo1", ZOOKEEPER_PORT)));
    }

    private void createContainers() {
        createStandaloneContainer();
        createSslContainer();
        createCloudContainer();
    }

    private void startContainers(DockerComposeContainer dc, GenericContainer... containers) {
        for (GenericContainer container : containers) {
            container.start();
        }

        dc.start();
    }

    /**
     * creates a standalone container
     */
    private void createStandaloneContainer() {
        standaloneContainer = new SolrContainer(SOLR_IMAGE)
                .withCollection(COLLECTION_NAME)
                .withZookeeper(false)
                .withLogConsumer(new Slf4jLogConsumer(LOGGER));
    }

    /**
     * creates a standalone container with SSL
     */
    private void createSslContainer() {
        sslContainer = new SolrContainer(SOLR_IMAGE)
                .withCollection(COLLECTION_NAME)
                .withZookeeper(false)
                .withClasspathResourceMapping("ssl", "/ssl", BindMode.READ_ONLY)
                .withEnv("SOLR_SSL_ENABLED", "true")
                .withEnv("SOLR_SSL_KEY_STORE", "/ssl/solr-ssl.keystore.jks")
                .withEnv("SOLR_SSL_KEY_STORE_PASSWORD", "secret")
                .withEnv("SOLR_SSL_TRUST_STORE", "/ssl/solr-ssl.keystore.jks")
                .withEnv("SOLR_SSL_TRUST_STORE_PASSWORD", "secret")
                .withEnv("SOLR_SSL_NEED_CLIENT_AUTH", "false")
                .withEnv("SOLR_SSL_WANT_CLIENT_AUTH", "false")
                .withEnv("SOLR_SSL_CHECK_PEER_NAME", "true")
                .withEnv("SOLR_SSL_KEY_STORE_TYPE", "JKS")
                .withEnv("SOLR_SSL_TRUST_STORE_TYPE", "JKS")
                .withLogConsumer(new Slf4jLogConsumer(LOGGER));

    }

    /**
     * creates a cloud container with zookeeper
     */
    private void createCloudContainer() {
        URI uri = null;
        try {
            if (SystemUtils.IS_OS_LINUX) {
                uri = this.getClass().getClassLoader().getResource("cloud-docker-compose.yml").toURI();
            } else {
                uri = this.getClass().getClassLoader().getResource("cloud-docker-compose_nonlinux.yml").toURI();
            }
            cloudContainer = new DockerComposeContainer(new File(uri))
                    .withExposedService("solr1", SOLR_PORT)
                    .withExposedService("zoo1", ZOOKEEPER_PORT)
                    .waitingFor("create-collection", Wait.forLogMessage(".*Created collection 'collection1'.*", 1));
        } catch (Exception e) {
            LOGGER.warn("can't create Cloud Container", e);
        }

    }

    @Override
    public void stop() {
        stopContainers(cloudContainer, standaloneContainer, sslContainer);
    }

    private void stopContainers(DockerComposeContainer dc, GenericContainer... containers) {
        for (GenericContainer container : containers) {
            if (container != null) {
                container.stop();
            }
        }

        if (dc != null) {
            dc.stop();
        }
    }
}
