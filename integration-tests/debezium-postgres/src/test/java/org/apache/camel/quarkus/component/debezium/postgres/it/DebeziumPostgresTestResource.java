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

package org.apache.camel.quarkus.component.debezium.postgres.it;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Collections;
import java.util.Map;

import org.apache.camel.quarkus.testcontainers.ContainerResourceLifecycleManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.TestcontainersConfiguration;

public class DebeziumPostgresTestResource implements ContainerResourceLifecycleManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(DebeziumPostgresTestResource.class);

    private static final int POSTGRES_PORT = 5432;
    private static final String POSTGRES_IMAGE = "debezium/postgres:11";

    private PostgreSQLContainer<?> postgresContainer;
    private Connection connection;
    private Path storeFile;
    private String hostname;
    private int port;

    @Override
    public Map<String, String> start() {
        LOGGER.info(TestcontainersConfiguration.getInstance().toString());

        try {
            storeFile = Files.createTempFile("debezium-postgress-store-", "");

            postgresContainer = new PostgreSQLContainer<>(POSTGRES_IMAGE)
                    .withUsername(DebeziumPostgresResource.DB_USERNAME)
                    .withPassword(DebeziumPostgresResource.DB_PASSWORD)
                    .withDatabaseName(DebeziumPostgresResource.DB_NAME)
                    .withInitScript("init.sql");

            postgresContainer.start();

            final String jdbcUrl = "jdbc:postgresql://" + postgresContainer.getContainerIpAddress() + ":"
                    + postgresContainer.getMappedPort(POSTGRES_PORT) + "/" + DebeziumPostgresResource.DB_NAME + "?user="
                    + DebeziumPostgresResource.DB_USERNAME + "&password=" + DebeziumPostgresResource.DB_PASSWORD;
            connection = DriverManager.getConnection(jdbcUrl);
            hostname = postgresContainer.getContainerIpAddress();
            port = postgresContainer.getMappedPort(POSTGRES_PORT);

            return Collections.emptyMap();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void stop() {
        try {
            if (connection != null) {
                connection.close();
            }
            if (postgresContainer != null) {
                postgresContainer.stop();
            }
            if (storeFile != null) {
                Files.deleteIfExists(storeFile);
            }
        } catch (Exception e) {
            // ignored
        }
    }

    @Override
    public void inject(Object testInstance) {
        ((DebeziumPostgresTest) testInstance).connection = this.connection;
        ((DebeziumPostgresTest) testInstance).hostname = this.hostname;
        ((DebeziumPostgresTest) testInstance).port = this.port;
        ((DebeziumPostgresTest) testInstance).offsetStorageFileName = this.storeFile.toString();
    }

}
