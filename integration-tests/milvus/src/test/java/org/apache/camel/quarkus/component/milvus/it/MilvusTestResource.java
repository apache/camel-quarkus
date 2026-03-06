
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
package org.apache.camel.quarkus.component.milvus.it;

import java.util.HashMap;
import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.eclipse.microprofile.config.ConfigProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.milvus.MilvusContainer;
import org.testcontainers.utility.DockerImageName;

public class MilvusTestResource implements QuarkusTestResourceLifecycleManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(MilvusTestResource.class);
    private static final String MILVUS_IMAGE = ConfigProvider.getConfig().getValue("milvus.container.image",
            String.class);
    private MilvusContainer milvus;

    @Override
    public Map<String, String> start() {
        Map<String, String> properties = new HashMap<>();
        DockerImageName dockerImageName = DockerImageName.parse(MILVUS_IMAGE).asCompatibleSubstituteFor("milvusdb/milvus");
        milvus = new MilvusContainer(dockerImageName);
        milvus.withLogConsumer(new Slf4jLogConsumer(LOGGER));
        milvus.start();
        properties.put("camel.component.milvus.host", milvus.getHost());
        properties.put("camel.component.milvus.port", String.valueOf(milvus.getMappedPort(19530)));
        LOGGER.info("Properties: {}", properties);
        return properties;

    }

    @Override
    public void stop() {
        try {
            if (milvus != null) {
                milvus.stop();
            }
        } catch (Exception e) {
            LOGGER.error("An error occurred while stopping Milvus container", e);
        }

    }
}
