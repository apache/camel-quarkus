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
import java.time.Duration;
import java.util.UUID;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.apache.camel.quarkus.test.support.debezium.AbstractDebeziumTest;
import org.apache.camel.quarkus.test.support.debezium.SharedKafkaTestResource;
import org.apache.camel.quarkus.test.support.debezium.Type;
import org.awaitility.Awaitility;
import org.eclipse.microprofile.config.Config;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
@QuarkusTestResource(value = DebeziumPostgresTestResource.class, restrictToAnnotatedClass = true)
@QuarkusTestResource(SharedKafkaTestResource.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DebeziumPostgresTest extends AbstractDebeziumTest {
    private static Connection connection;

    public DebeziumPostgresTest() {
        super(Type.postgres);
    }

    @BeforeAll
    public static void setUp(Config config) throws SQLException {
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
                .body("'database.connectionTimeZone'", is("CET"))
                .body("'bootstrap.servers'", is(notNullValue()));
    }

    @Test
    @Order(5)
    public void testKafkaOffsetBackingStore() throws SQLException {
        given()
                .when().get("/debezium-postgres/kafkaBootstrapServers")
                .then()
                .statusCode(200)
                .body(not(equalTo("not-available")))
                .body(containsString("PLAINTEXT://"));

        String suffix = UUID.randomUUID().toString().substring(0, 8);
        insertCompany("KafkaOffsetTest_" + suffix, "KafkaCity");

        Awaitility.await().pollDelay(Duration.ofMillis(250)).atMost(Duration.ofMinutes(1)).untilAsserted(() -> {
            given()
                    .when().get("/debezium-postgres/receiveViaKafkaOffset")
                    .then()
                    .statusCode(200)
                    .body(is(not(emptyOrNullString())));
        });
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
