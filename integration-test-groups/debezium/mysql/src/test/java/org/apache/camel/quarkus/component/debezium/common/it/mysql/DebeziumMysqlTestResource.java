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

package org.apache.camel.quarkus.component.debezium.common.it.mysql;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;

import org.apache.camel.quarkus.test.support.debezium.AbstractDebeziumTestResource;
import org.apache.camel.quarkus.test.support.debezium.Type;
import org.eclipse.microprofile.config.ConfigProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

public class DebeziumMysqlTestResource extends AbstractDebeziumTestResource<MySQLContainer<?>> {
    private static final Logger log = LoggerFactory.getLogger(DebeziumMysqlTestResource.class);

    public static final String DB_NAME = "test";
    public static final String DB_USERNAME = "user";
    public static final String DB_PASSWORD = "test";
    private static final int DB_PORT = 3306;
    private static final String MYSQL_IMAGE = ConfigProvider.getConfig().getValue("mysql.container.image", String.class);

    private Path historyFile;

    public DebeziumMysqlTestResource() {
        super(Type.mysql);
    }

    @Override
    protected MySQLContainer createContainer() {
        // workaround for Failed to verify that image 'mirror.gcr.io/library/mysql:8.4' is a compatible substitute for 'mysql'.
        // This generally means that you are trying to use an image that Testcontainers has not been designed to use.
        DockerImageName mySqlImage = DockerImageName.parse(MYSQL_IMAGE).asCompatibleSubstituteFor("mysql");

        return new MySQLContainer<>(mySqlImage)
                .withUsername(DB_USERNAME)
                .withPassword(DB_PASSWORD)
                .withDatabaseName(DB_NAME)
                //                .withLogConsumer(new Slf4jLogConsumer(log))
                .withInitScript("initMysql.sql");
    }

    @Override
    public Map<String, String> start() {
        //in case that driver is not provided, container is not started
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            log.warn("Driver for mySql database is not provided. Container won't start.");
            return Collections.emptyMap();
        }

        Map<String, String> properties = super.start();

        try {
            historyFile = Files.createTempFile(getClass().getSimpleName() + "-history-file-", "");

            properties.put(DebeziumMysqlResource.PROPERTY_DB_HISTORY_FILE, historyFile.toString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return properties;
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
        return "jdbc:mysql://" + container.getHost() + ":" + container.getMappedPort(DB_PORT) + "/"
                + DebeziumMysqlTestResource.DB_NAME + "?user=" + DB_USERNAME
                + "&password=" + DB_PASSWORD;
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
