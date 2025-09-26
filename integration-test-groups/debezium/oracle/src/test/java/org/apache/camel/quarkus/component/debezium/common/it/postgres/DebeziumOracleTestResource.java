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

package org.apache.camel.quarkus.component.debezium.common.it.postgres;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.apache.camel.quarkus.test.support.debezium.AbstractDebeziumTestResource;
import org.apache.camel.quarkus.test.support.debezium.Type;
import org.eclipse.microprofile.config.ConfigProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.OracleContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

public class DebeziumOracleTestResource extends AbstractDebeziumTestResource<GenericContainer<?>> {

    private static final Logger LOG = LoggerFactory.getLogger(DebeziumOracleTestResource.class);
    public static final String DB_USERNAME = "oracleUser";
    public static final String DB_PASSWORD = "changeit";
    private static final String ORACLE_IMAGE = ConfigProvider.getConfig().getValue("oracle-debezium.container.image",
            String.class);
    private static final int DB_PORT = 1521;
    private Path historyFile;

    public DebeziumOracleTestResource() {
        super(Type.oracle);
    }

    @Override
    protected GenericContainer<?> createContainer() {
        DockerImageName imageName = DockerImageName.parse(ORACLE_IMAGE)
                .asCompatibleSubstituteFor("gvenzl/oracle-xe");
        return new OracleContainer(imageName)
                .withUsername(DB_USERNAME)
                .withPassword(DB_PASSWORD)
                .withDatabaseName(DebeziumOracleResource.DB_NAME)
                .withCopyFileToContainer(
                        MountableFile.forClasspathResource("initOraclePermissions.sql"),
                        "/docker-entrypoint-initdb.d/init.sql")
                .withInitScript("initOracle.sql");
    }

    @Override
    public Map<String, String> start() {
        Map<String, String> properties;

        // TODO: Remove retry logic - https://github.com/apache/camel-quarkus/issues/7773
        int maxRetries = 5;
        for (int i = 1; i <= maxRetries; i++) {
            try {
                LOG.info("Starting {} attempt {} of {}", ORACLE_IMAGE, i, maxRetries);
                properties = super.start();
                historyFile = Files.createTempFile(getClass().getSimpleName() + "-history-file-", "");
                properties.put(DebeziumOracleResource.PROPERTY_DB_HISTORY_FILE, historyFile.toString());
                return properties;
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (Exception e) {
                LOG.warn("Container startup failed", e);
                LOG.warn(e.getMessage());
                if (i == maxRetries) {
                    LOG.warn("Giving up starting {} - max container startup attempts reached", ORACLE_IMAGE);
                    throw e;
                }

                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        throw new IllegalStateException("Could not start container for " + ORACLE_IMAGE);
    }

    @Override
    public void stop() {
        super.stop();

        try {
            if (historyFile != null) {
                Files.deleteIfExists(historyFile);
            }
        } catch (Exception e) {
            // ignored
        }
    }

    @Override
    protected String getJdbcUrl() {
        return "jdbc:oracle:thin:%s/%s@%s:%d/%s".formatted(DB_USERNAME, DB_PASSWORD, container.getHost(),
                container.getMappedPort(DB_PORT), DebeziumOracleResource.DB_NAME);
    }

    @Override
    protected String getUsername() {
        return DB_USERNAME;
    }

    @Override
    protected String getPassword() {
        return DB_PASSWORD;
    }

    @Override
    protected int getPort() {
        return DB_PORT;
    }
}
