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
package org.apache.camel.quarkus.component.minio.it;

import java.time.Duration;
import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.apache.camel.util.CollectionHelper;
import org.eclipse.microprofile.config.ConfigProvider;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;

public class MinioTestResource implements QuarkusTestResourceLifecycleManager {

    public static final String CONTAINER_ACCESS_KEY = "MINIO_ACCESS_KEY";
    public static final String CONTAINER_SECRET_KEY = "MINIO_SECRET_KEY";
    private static final String CONTAINER_IMAGE = ConfigProvider.getConfig().getValue("minio.container.image", String.class);
    private static final int BROKER_PORT = 9000;
    private String endpoint;

    private GenericContainer minioServer = new GenericContainer(CONTAINER_IMAGE)
            .withEnv(CONTAINER_ACCESS_KEY, MinioResource.SERVER_ACCESS_KEY)
            .withEnv(CONTAINER_SECRET_KEY, MinioResource.SERVER_SECRET_KEY)
            .withCommand("server /data")
            .withExposedPorts(BROKER_PORT)
            .waitingFor(new HttpWaitStrategy()
                    .forPath("/minio/health/ready")
                    .forPort(BROKER_PORT)
                    .withStartupTimeout(Duration.ofSeconds(10)));

    @Override
    public Map<String, String> start() {
        minioServer.start();

        String port = minioServer.getMappedPort(BROKER_PORT) + "";
        String host = minioServer.getHost();

        endpoint = String.format("http://%s:%s", host, port);
        return CollectionHelper.mapOf(
                MinioProducer.MINIO_CLIENT_URL_PARAMETER, endpoint);
    }

    @Override
    public void stop() {
        if (minioServer.isRunning()) {
            minioServer.stop();
        }
    }

    @Override
    public void inject(Object testInstance) {
        ((MinioTest) testInstance).setEndpoint(endpoint);
    }
}
