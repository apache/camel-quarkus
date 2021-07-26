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
package org.apache.camel.quarkus.component.pg.replication.slot.it;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import static io.restassured.RestAssured.given;
import static org.apache.camel.quarkus.component.pg.replication.slot.it.PgReplicationSlotRoute.PG_AUTHORITY_CFG_KEY;
import static org.apache.camel.quarkus.component.pg.replication.slot.it.PgReplicationSlotRoute.PG_DBNAME_CFG_KEY;
import static org.apache.camel.quarkus.component.pg.replication.slot.it.PgReplicationSlotRoute.PG_PASSRD_CFG_KEY;
import static org.apache.camel.quarkus.component.pg.replication.slot.it.PgReplicationSlotRoute.PG_USER_CFG_KEY;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTestResource(PgReplicationSlotTestResource.class)
@QuarkusTest
class PgReplicationSlotTest {

    private static Connection connection;

    @BeforeAll
    public static void setUp() throws SQLException {
        String authority = ConfigProvider.getConfig().getValue(PG_AUTHORITY_CFG_KEY, String.class);
        String dbName = ConfigProvider.getConfig().getValue(PG_DBNAME_CFG_KEY, String.class);
        String user = ConfigProvider.getConfig().getValue(PG_USER_CFG_KEY, String.class);
        String password = ConfigProvider.getConfig().getValue(PG_PASSRD_CFG_KEY, String.class);

        String url = String.format("jdbc:postgresql://%s/%s", authority, dbName);
        Properties props = new Properties();
        props.setProperty("user", user);
        props.setProperty("password", password);

        connection = DriverManager.getConnection(url, props);
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS camel_test_table(id int);");
        }
    }

    @AfterAll
    public static void tearDown() throws SQLException {
        connection.close();
    }

    //@Test
    public void insertsShouldTriggerReplicationEvents() throws SQLException {

        try (Statement statement = connection.createStatement()) {
            statement.execute("INSERT INTO camel_test_table(id) VALUES(1984);");
            statement.execute("INSERT INTO camel_test_table(id) VALUES(1998);");
        }

        await().atMost(10L, TimeUnit.SECONDS).until(() -> {
            return given().contentType(ContentType.JSON).get("/pg-replication-slot/get-events").path("size()").equals(6);
        });
        String[] results = given().contentType(ContentType.JSON).get("/pg-replication-slot/get-events").then().extract()
                .as(String[].class);
        assertEquals(6, results.length);
        assertEquals("BEGIN", results[0]);
        assertEquals("table public.camel_test_table: INSERT: id[integer]:1984", results[1]);
        assertEquals("COMMIT", results[2]);
        assertEquals("BEGIN", results[3]);
        assertEquals("table public.camel_test_table: INSERT: id[integer]:1998", results[4]);
        assertEquals("COMMIT", results[5]);
    }

}
