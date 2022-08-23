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

import org.apache.camel.quarkus.component.debezium.common.it.AbstractDebeziumTestResource;
import org.apache.camel.quarkus.component.debezium.common.it.DebeziumPostgresResource;
import org.apache.camel.quarkus.component.debezium.common.it.Type;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

public class DebeziumPostgresTestResource extends AbstractDebeziumTestResource<PostgreSQLContainer<?>> {

    public static final String DB_USERNAME = "postgres";
    public static final String DB_PASSWORD = "changeit";
    private static final String POSTGRES_IMAGE = "debezium/postgres:11";
    private static final int DB_PORT = 5432;

    public DebeziumPostgresTestResource() {
        super(Type.postgres);
    }

    @Override
    protected PostgreSQLContainer<?> createContainer() {
        DockerImageName imageName = DockerImageName.parse(POSTGRES_IMAGE)
                .asCompatibleSubstituteFor("postgres");
        return new PostgreSQLContainer<>(imageName)
                .withUsername(DB_USERNAME)
                .withPassword(DB_PASSWORD)
                .withDatabaseName(DebeziumPostgresResource.DB_NAME)
                .withInitScript("initPostgres.sql");
    }

    @Override
    protected String getJdbcUrl() {
        return "jdbc:postgresql://" + container.getHost() + ":"
                + container.getMappedPort(DB_PORT) + "/" + DebeziumPostgresResource.DB_NAME + "?user="
                + DB_USERNAME + "&password=" + DB_PASSWORD;
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
