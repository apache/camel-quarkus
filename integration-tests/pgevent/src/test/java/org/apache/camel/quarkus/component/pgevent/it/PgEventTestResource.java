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
package org.apache.camel.quarkus.component.pgevent.it;

import java.util.Map;

import org.apache.camel.quarkus.testcontainers.ContainerResourceLifecycleManager;
import org.apache.camel.util.CollectionHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;

public class PgEventTestResource implements ContainerResourceLifecycleManager {

    protected static final String CONTAINER_NAME = "pg-event";
    protected static final String POSTGRES_USER = "postgres";
    protected static final String POSTGRES_PASSWORD = "mysecretpassword";
    protected static final String POSTGRES_DB = "postgres";

    private static final Logger LOGGER = LoggerFactory.getLogger(PgEventTestResource.class);
    private static final int POSTGRES_PORT = 5432;
    private static final String POSTGRES_IMAGE = "postgres:13.0";

    private GenericContainer container;

    @Override
    public Map<String, String> start() {
        container = createContainer();
        container.start();
        return CollectionHelper.mapOf(
                "database.port",
                container.getMappedPort(POSTGRES_PORT).toString(),
                "database.host",
                container.getHost(),
                "quarkus.datasource.pgDatasource.jdbc.url",
                String.format("jdbc:pgsql://%s:%s/postgres", container.getHost(), container.getMappedPort(POSTGRES_PORT)));
    }

    @Override
    public void stop() {
        if (container != null) {
            container.stop();
        }
    }

    private GenericContainer createContainer() {
        GenericContainer container = new GenericContainer(POSTGRES_IMAGE)
                .withCommand("postgres -c wal_level=logical")
                .withExposedPorts(POSTGRES_PORT)
                .withNetworkAliases(CONTAINER_NAME)
                .withEnv("POSTGRES_USER", POSTGRES_USER)
                .withEnv("POSTGRES_PASSWORD", POSTGRES_PASSWORD)
                .withEnv("POSTGRES_DB", POSTGRES_DB)
                .withLogConsumer(new Slf4jLogConsumer(LOGGER))
                .waitingFor(Wait.forListeningPort());
        return container;
    }

    public Integer getMappedPort() {
        return container.getMappedPort(POSTGRES_PORT);
    }

    public String getHost() {
        return container.getHost();
    }

}
