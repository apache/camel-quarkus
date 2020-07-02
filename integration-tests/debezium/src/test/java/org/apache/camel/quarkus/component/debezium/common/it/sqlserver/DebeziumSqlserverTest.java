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

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.Response;
import org.apache.camel.quarkus.component.debezium.common.it.AbstractDebeziumTest;
import org.apache.camel.quarkus.component.debezium.common.it.Record;
import org.apache.camel.quarkus.component.debezium.common.it.Type;
import org.jboss.logging.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

@QuarkusTest
@QuarkusTestResource(DebeziumSqlserverTestResource.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DebeziumSqlserverTest extends AbstractDebeziumTest {
    private static final Logger LOG = Logger.getLogger(DebeziumSqlserverTest.class);

    //has to be constant and has to be equal to Type.mysql.getJdbcProperty
    public static final String PROPERTY_JDBC = "sqlserver_jdbc";

    private static Connection connection;

    public DebeziumSqlserverTest() {
        super(Type.sqlserver);
    }

    @BeforeAll
    public static void setUp() throws SQLException {
        final String jdbcUrl = System.getProperty(Type.sqlserver.getPropertyJdbc());

        if (jdbcUrl != null) {
            connection = DriverManager.getConnection(jdbcUrl);
        } else {
            LOG.warn("Container is not running. Connection is not created.");
        }
    }

    @Before
    public void before() {
        org.junit.Assume.assumeTrue(connection != null);
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
    @EnabledIfSystemProperty(named = PROPERTY_JDBC, matches = ".*")
    public void testReceiveInitCompany() {
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

            Assert.assertEquals("r", record.getOperation());
            Assert.assertEquals("Struct{NAME=init,CITY=init}", record.getValue());
            break;
        }
    }

    @Test
    @Order(1)
    @EnabledIfSystemProperty(named = PROPERTY_JDBC, matches = ".*")
    public void testInsert() throws SQLException {
        super.testInsert();
    }

    @Test
    @Order(2)
    @EnabledIfSystemProperty(named = PROPERTY_JDBC, matches = ".*")
    public void testUpdate() throws SQLException {
        super.testUpdate();
    }

    @Test
    @Order(3)
    @EnabledIfSystemProperty(named = PROPERTY_JDBC, matches = ".*")
    public void testDelete() throws SQLException {
        super.testDelete();
    }

}
