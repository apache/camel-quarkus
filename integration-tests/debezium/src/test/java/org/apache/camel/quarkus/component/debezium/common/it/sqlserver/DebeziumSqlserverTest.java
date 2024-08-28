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
package org.apache.camel.quarkus.component.debezium.common.it.sqlserver;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Optional;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.Response;
import org.apache.camel.quarkus.component.debezium.common.it.AbstractDebeziumTest;
import org.apache.camel.quarkus.component.debezium.common.it.Record;
import org.apache.camel.quarkus.component.debezium.common.it.Type;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@QuarkusTest
@QuarkusTestResource(DebeziumSqlserverTestResource.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DebeziumSqlserverTest extends AbstractDebeziumTest {
    private static final Logger LOG = Logger.getLogger(DebeziumSqlserverTest.class);

    private static Connection connection;

    public DebeziumSqlserverTest() {
        super(Type.sqlserver);
    }

    @BeforeAll
    public static void setUp() throws SQLException {
        Config config = ConfigProvider.getConfig();
        final Optional<String> jdbcUrl = config.getOptionalValue(Type.sqlserver.getPropertyJdbc(), String.class);

        assumeTrue(jdbcUrl.isPresent(),
                "Ms SQL EULA is not accepted. Container won't start.");

        if (jdbcUrl.isPresent()) {
            connection = DriverManager.getConnection(jdbcUrl.get());
        } else {
            LOG.warn("Container is not running. Connection is not created.");
        }
    }

    @BeforeEach
    public void before() {
        assumeTrue(connection != null);
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

    @Override
    protected String getCompanyTableName() {
        return "Test." + super.getCompanyTableName();
    }

    @Test
    @Order(0)
    public void testReceiveInitCompany() {
        Config config = ConfigProvider.getConfig();
        assumeTrue(config.getOptionalValue(Type.sqlserver.getPropertyJdbc(), String.class).isPresent());

        int i = 0;

        while (i++ < AbstractDebeziumTest.REPEAT_COUNT) {
            //receive first record (operation r) for the init company - using larger timeout
            Response response = receiveResponse("/receiveAsRecord");

            response.then()
                    .statusCode(200);

            Record record = response.getBody().as(Record.class);

            if (record.getOperation() == null) {
                continue;
            }

            assertEquals("r", record.getOperation());
            assertEquals("Struct{NAME=init,CITY=init}", record.getValue());
            break;
        }
    }
}
