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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Optional;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.apache.camel.quarkus.component.debezium.common.it.AbstractDebeziumTest;
import org.apache.camel.quarkus.component.debezium.common.it.Type;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

@QuarkusTest
@QuarkusTestResource(DebeziumMysqlTestResource.class)
//@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DebeziumMysqlTest extends AbstractDebeziumTest {
    private static final Logger LOG = Logger.getLogger(DebeziumMysqlTest.class);

    //has to be constant and has to be equal to Type.mysql.getJdbcProperty
    public static final String PROPERTY_JDBC = "mysql_jdbc";
    private static Connection connection;

    public DebeziumMysqlTest() {
        super(Type.mysql);
    }

    @BeforeAll
    public static void setUp() throws SQLException {
        Config config = ConfigProvider.getConfig();
        Optional<String> jdbcUrl = config.getOptionalValue(PROPERTY_JDBC, String.class);

        if (jdbcUrl.isPresent()) {
            connection = DriverManager.getConnection(jdbcUrl.get());
        } else {
            LOG.warn("Container is not running. Connection is not created.");
        }
    }

    @AfterAll
    public static void cleanUp() throws SQLException {
        if (connection != null) {
            connection.close();
        }
    }

    //@Test
    @Order(0)
    @EnabledIfSystemProperty(named = PROPERTY_JDBC, matches = ".*")
    public void testReceiveEmptyMessages() {
        //receive all empty messages before other tests
        receiveResponse("/receiveEmptyMessages")
                .then()
                .statusCode(204);
    }

    //@Test
    @Order(1)
    @EnabledIfSystemProperty(named = PROPERTY_JDBC, matches = ".*")
    public void testInsert() throws SQLException {
        super.testInsert();
    }

    //@Test
    @Order(2)
    @EnabledIfSystemProperty(named = PROPERTY_JDBC, matches = ".*")
    public void testUpdate() throws SQLException {
        super.testUpdate();
    }

    //@Test
    @Order(3)
    @EnabledIfSystemProperty(named = PROPERTY_JDBC, matches = ".*")
    public void testDelete() throws SQLException {
        super.testDelete();
    }

    @Override
    protected Connection getConnection() {
        return connection;
    }
}
