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
package org.apache.camel.quarkus.component.opentelemetry.it;

import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

public class OpenTelemetryTestResource implements QuarkusTestResourceLifecycleManager {
    private static final String POSTGRES_USER = "test";
    private static final String POSTGRES_PASSWORD = "s3cr3t";
    private static final String POSTGRES_DB = "otel";
    private static final int POSTGRES_PORT = 5432;
    private static final String POSTGRES_IMAGE = "postgres:13.0";

    private GenericContainer<?> container;

    @Override
    public Map<String, String> start() {
        container = new GenericContainer<>(POSTGRES_IMAGE)
                .withExposedPorts(POSTGRES_PORT)
                .withEnv("POSTGRES_USER", POSTGRES_USER)
                .withEnv("POSTGRES_PASSWORD", POSTGRES_PASSWORD)
                .withEnv("POSTGRES_DB", POSTGRES_DB)
                .waitingFor(Wait.forListeningPort());

        container.start();

        String jdbcUrl = String.format("jdbc:otel:postgresql://localhost:%d/%s", container.getMappedPort(POSTGRES_PORT),
                POSTGRES_DB);
        return Map.of("quarkus.datasource.jdbc.url", jdbcUrl);
    }

    @Override
    public void stop() {
        if (container != null) {
            container.stop();
        }
    }
}
