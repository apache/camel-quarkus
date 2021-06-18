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
package org.apache.camel.quarkus.component.http.it;

import java.util.Map;
import java.util.Objects;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.apache.camel.quarkus.test.AvailablePortFinder;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import static org.apache.camel.quarkus.component.http.it.HttpResource.USER_ADMIN;
import static org.apache.camel.quarkus.component.http.it.HttpResource.USER_ADMIN_PASSWORD;

public class HttpTestResource implements QuarkusTestResourceLifecycleManager {

    private static final DockerImageName TINY_PROXY_IMAGE_NAME = DockerImageName.parse("monokal/tinyproxy");
    private static final Integer TINY_PROXY_PORT = 8888;
    private GenericContainer container;

    @Override
    public Map<String, String> start() {
        container = new GenericContainer(TINY_PROXY_IMAGE_NAME)
                .withEnv("BASIC_AUTH_USER", USER_ADMIN)
                .withEnv("BASIC_AUTH_PASSWORD", USER_ADMIN_PASSWORD)
                .withExposedPorts(TINY_PROXY_PORT)
                .withCommand("ANY")
                .waitingFor(Wait.forListeningPort());

        container.start();

        Map<String, String> options = AvailablePortFinder.reserveNetworkPorts(
                Objects::toString,
                "camel.netty-http.test-port",
                "camel.netty-http.https-test-port",
                "camel.netty-http.compression-test-port");
        options.put("tiny.proxy.port", container.getMappedPort(TINY_PROXY_PORT).toString());
        return options;
    }

    @Override
    public void stop() {
        if (container != null) {
            container.stop();
        }
    }
}
