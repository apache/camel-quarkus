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
package org.apache.camel.quarkus.support.azure.core.http.vertx;

import java.util.HashMap;
import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.apache.commons.lang3.SystemUtils;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

public class VertxHttpClientTestResource implements QuarkusTestResourceLifecycleManager {

    public static final String PROXY_USER = "admin";
    public static final String PROXY_PASSWORD = "p4ssw0rd";

    private static final DockerImageName TINY_PROXY_IMAGE_NAME = DockerImageName.parse("monokal/tinyproxy");
    private static final Integer TINY_PROXY_PORT = 8888;
    private GenericContainer container;

    @Override
    public Map<String, String> start() {
        String host;
        int port;

        container = new GenericContainer(TINY_PROXY_IMAGE_NAME)
                .withEnv("BASIC_AUTH_USER", PROXY_USER)
                .withEnv("BASIC_AUTH_PASSWORD", PROXY_PASSWORD)
                .withCommand("ANY")
                .waitingFor(Wait.forListeningPort());

        if (SystemUtils.IS_OS_LINUX) {
            container.withNetworkMode("host");
            port = TINY_PROXY_PORT;
            host = "localhost";
        } else {
            container.withNetworkMode("bridge")
                    .withExposedPorts(TINY_PROXY_PORT);
            port = container.getMappedPort(TINY_PROXY_PORT);
            host = "host.docker.internal";
        }

        container.start();

        Map<String, String> options = new HashMap<>();
        options.put("tiny.proxy.host", host);
        options.put("tiny.proxy.port", String.valueOf(port));
        return options;
    }

    @Override
    public void stop() {
        if (container != null) {
            container.stop();
        }
    }
}
