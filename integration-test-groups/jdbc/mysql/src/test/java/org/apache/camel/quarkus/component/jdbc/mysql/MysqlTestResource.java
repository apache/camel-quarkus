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
package org.apache.camel.quarkus.component.jdbc.mysql;

import java.util.Collections;
import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.eclipse.microprofile.config.ConfigProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

/**
 * This is a workaround because of the FIPS environment.
 * DevService does not work on FIS https://github.com/quarkusio/quarkus/issues/40526
 *
 * If devservice is disabled (by FIPS profile), this test resource starts a simple test container.
 * (which works on FIPS, because the where the validation of the DB is not solved via connection ,but by the listening
 * port.)
 */
public class MysqlTestResource implements QuarkusTestResourceLifecycleManager {
    private static final Logger LOG = LoggerFactory.getLogger(MysqlTestResource.class);

    public static final String DB_NAME = "test";
    public static final String DB_USERNAME = "user";
    public static final String DB_PASSWORD = "test";
    private static final int DB_PORT = 3306;
    private static final String MYSQL_IMAGE = ConfigProvider.getConfig().getValue("mysql.container.image", String.class);
    private GenericContainer container;

    @Override
    public Map<String, String> start() {

        //in dev service is not enabled, spawn a db
        if (!ConfigProvider.getConfig().getOptionalValue("quarkus.datasource.mysql.devservices.enabled", Boolean.class)
                .orElse(true)) {
            LOG.info("DevService is disabled, MySql container is starting.");

            container = new GenericContainer<>(MYSQL_IMAGE)
                    .withExposedPorts(DB_PORT)
                    .withEnv("MYSQL_USER", DB_USERNAME)
                    .withEnv("MYSQL_ROOT_PASSWORD", DB_PASSWORD)
                    .withEnv("MYSQL_PASSWORD", DB_PASSWORD)
                    .withEnv("MYSQL_DATABASE", DB_NAME)
                    .waitingFor(Wait.forListeningPort());

            container.start();

            return Map.of(
                    "quarkus.datasource.mysql.jdbc.url", getJdbcUrl(),
                    "quarkus.datasource.mysql.username", DB_USERNAME,
                    "quarkus.datasource.mysql.password", DB_PASSWORD,
                    "quarkus.datasource.mysql.devservices.enabled", "false");
        }
        //if devservice is running, nothing has to be done
        return Collections.emptyMap();
    }

    @Override
    public void stop() {
        try {
            if (container != null) {
                container.stop();
            }
        } catch (Exception e) {
            // Ignored
        }
    }

    protected String getJdbcUrl() {
        return "jdbc:mysql://" + container.getHost() + ":" + container.getMappedPort(DB_PORT) + "/"
                + DB_NAME + "?user=" + DB_USERNAME
                + "&password=" + DB_PASSWORD;
    }

}
