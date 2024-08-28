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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.apache.camel.quarkus.component.debezium.common.it.AbstractDebeziumTest;
import org.apache.camel.quarkus.component.debezium.common.it.Type;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static org.hamcrest.Matchers.is;

@QuarkusTest
@QuarkusTestResource(DebeziumPostgresTestResource.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DebeziumPostgresTest extends AbstractDebeziumTest {
    private static final Logger LOG = Logger.getLogger(DebeziumPostgresTest.class);

    private static Connection connection;

    public DebeziumPostgresTest() {
        super(Type.postgres);
    }

    @BeforeAll
    public static void setUp() throws SQLException {
        Config config = ConfigProvider.getConfig();
        final String jdbcUrl = config.getValue(Type.postgres.getPropertyJdbc(), String.class);
        connection = DriverManager.getConnection(jdbcUrl);
    }

    @Test
    @Order(4)
    public void testAdditionalProperty() {
        //https://github.com/apache/camel-quarkus/issues/3488
        RestAssured.get(Type.postgres.getComponent() + "/getAdditionalProperties")
                .then()
                .statusCode(200)
                .body("'database.connectionTimeZone'", is("CET"));
    }

    @AfterAll
    public static void cleanUp() throws SQLException {
        if (connection != null) {
            connection.close();
        }
    }

    @Override
    protected Connection getConnection() {
        return connection;
    }

}
