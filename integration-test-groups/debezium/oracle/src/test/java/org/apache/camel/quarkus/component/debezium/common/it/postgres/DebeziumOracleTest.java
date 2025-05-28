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
import org.apache.camel.quarkus.test.support.debezium.AbstractDebeziumTest;
import org.apache.camel.quarkus.test.support.debezium.Type;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestMethodOrder;

import static org.hamcrest.Matchers.*;

@QuarkusTest
@QuarkusTestResource(value = DebeziumOracleTestResource.class, restrictToAnnotatedClass = true)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DebeziumOracleTest extends AbstractDebeziumTest {
    private static final Logger LOG = Logger.getLogger(DebeziumOracleTest.class);

    private static Connection connection;

    public DebeziumOracleTest() {
        super(Type.oracle);
    }

    @BeforeAll
    public static void setUp() throws SQLException {
        Config config = ConfigProvider.getConfig();
        final String jdbcUrl = config.getValue(Type.oracle.getPropertyJdbc(), String.class);
        connection = DriverManager.getConnection(jdbcUrl, DebeziumOracleTestResource.DB_USERNAME,
                DebeziumOracleTestResource.DB_PASSWORD);
    }

    @Override
    protected Connection getConnection() {
        return connection;
    }

}
