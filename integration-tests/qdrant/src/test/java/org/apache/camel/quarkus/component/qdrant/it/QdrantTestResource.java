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
package org.apache.camel.quarkus.component.qdrant.it;

import java.util.HashMap;
import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.eclipse.microprofile.config.ConfigProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.qdrant.QdrantContainer;
import org.testcontainers.utility.DockerImageName;

public class QdrantTestResource implements QuarkusTestResourceLifecycleManager {

    private static final Logger LOG = LoggerFactory.getLogger(QdrantTestResource.class);
    private static final String QDRANT_IMAGE = ConfigProvider.getConfig().getValue("qdrant.container.image", String.class);
    private static final int QDRANT_GRPC_PORT = 6334;

    private GenericContainer<?> qdrantContainer;

    @Override
    public Map<String, String> start() {
        Map<String, String> properties = new HashMap<>();

        DockerImageName qdrantImageName = DockerImageName.parse(QDRANT_IMAGE).asCompatibleSubstituteFor("qdrant/qdrant");
        qdrantContainer = new QdrantContainer(qdrantImageName)
                .withLogConsumer(new Slf4jLogConsumer(LOG));
        qdrantContainer.start();

        String grpcHost = qdrantContainer.getHost();
        Integer grpcPort = qdrantContainer.getMappedPort(QDRANT_GRPC_PORT);

        properties.put("camel.component.qdrant.host", grpcHost);
        properties.put("camel.component.qdrant.port", grpcPort.toString());

        LOG.info("Properties: {}", properties);

        return properties;
    }

    @Override
    public void stop() {
        try {
            if (qdrantContainer != null) {
                qdrantContainer.stop();
            }
        } catch (Exception ex) {
            LOG.error("An issue occurred while stopping Qdrant container", ex);
        }
    }

}
