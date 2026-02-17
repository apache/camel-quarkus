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
package org.apache.camel.quarkus.component.master.it;

import java.util.HashMap;
import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.eclipse.microprofile.config.ConfigProvider;
import org.infinispan.client.hotrod.impl.ConfigurationProperties;
import org.infinispan.testcontainers.InfinispanContainer;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

public class InfinispanClusterServiceTestResource implements QuarkusTestResourceLifecycleManager {
    private static final String INFINISPAN_IMAGE = ConfigProvider.getConfig().getValue("infinispan.container.image",
            String.class);
    private static final String USER = "camel";
    private static final String PASS = "2s3cr3t";
    private GenericContainer<?> container;

    @Override
    public Map<String, String> start() {
        DockerImageName dockerImageName = DockerImageName.parse(INFINISPAN_IMAGE);
        container = new InfinispanContainer(dockerImageName)
                .withExposedPorts(ConfigurationProperties.DEFAULT_HOTROD_PORT)
                .withUser(USER)
                .withPassword(PASS)
                .withClasspathResourceMapping("infinispan-cluster-service.xml", "/user-config/infinispan-cluster-service.xml",
                        BindMode.READ_ONLY)
                .withCommand(
                        "-c", "infinispan.xml",
                        "-c", "/user-config/infinispan-cluster-service.xml")
                .waitingFor(Wait.forLogMessage(".*Infinispan Server.*started.*", 1));

        container.start();

        Map<String, String> config = new HashMap<>();
        String host = String.format("%s:%d", DockerClientFactory.instance().dockerHostIpAddress(),
                container.getMappedPort(ConfigurationProperties.DEFAULT_HOTROD_PORT));
        config.put("quarkus.camel.cluster.infinispan.hosts", host);
        config.put("quarkus.camel.cluster.infinispan.username", USER);
        config.put("quarkus.camel.cluster.infinispan.password", PASS);
        config.put("quarkus.camel.cluster.infinispan.secure", "true");
        config.put("quarkus.camel.cluster.infinispan.security-realm", "default");
        config.put("quarkus.camel.cluster.infinispan.sasl-mechanism", "SCRAM-SHA-512");
        config.put("quarkus.camel.cluster.infinispan.security-server-name", "infinispan");

        return config;
    }

    @Override
    public void stop() {
        if (container != null) {
            container.stop();
        }
    }
}
