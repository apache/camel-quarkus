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
package org.apache.camel.quarkus.component.debezium.common.it;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.util.UUID;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.hamcrest.Matcher;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.org.awaitility.Awaitility;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Abstract parent for debezium based tests.
 * Contains tests for insert, update and delete.
 */
public abstract class AbstractDebeziumTest {
    private static final Logger LOG = Logger.getLogger(AbstractDebeziumTest.class);

    protected static String COMPANY_1 = "Best Company";
    protected static String COMPANY_2 = "Even Better Company";
    protected static String CITY_1 = "Prague";
    protected static String CITY_2 = "Paris";
    public static int REPEAT_COUNT = 3;

    /**
     * Each impleentation is responsible for connection creation and its closure.
     */
    protected abstract Connection getConnection();

    private final Type type;

    public AbstractDebeziumTest(Type type) {
        this.type = type;
    }

    protected String getCompanyTableName() {
        return "COMPANY";
    }

    @Test
    @Order(1)
    public void testInsert() throws SQLException {
        isInitialized("Test 'testInsert' is skipped, because container is not running.");

        String suffix = UUID.randomUUID().toString();
        insertCompany(COMPANY_1 + "_" + suffix, CITY_1);

        Awaitility.await().pollDelay(Duration.ofMillis(250)).atMost(Duration.ofMinutes(1)).untilAsserted(() -> {
            receiveResponse()
                    .then()
                    .statusCode(200)
                    .body(containsString((COMPANY_1 + "_" + suffix)));
        });
    }

    protected void isInitialized(String s) {
        assertNotNull(getConnection(), s);
    }

    protected void insertCompany(String name, String city) throws SQLException {
        executeUpdate(String.format("INSERT INTO %s (name, city) VALUES ('%s', '%s')", getCompanyTableName(),
                name, city));
    }

    @Disabled("https://github.com/apache/camel-quarkus/issues/6911")
    @Test
    @Order(2)
    public void testUpdate() throws SQLException {
        isInitialized("Test 'testUpdate' is skipped, because container is not running.");

        executeUpdate(String.format("INSERT INTO %s (name, city) VALUES ('%s', '%s')", getCompanyTableName(),
                COMPANY_2, CITY_2));

        //validate that event is in queue
        receiveResponse(200, containsString(COMPANY_2));

        executeUpdate(String.format("UPDATE %s SET name = '%s_changed' WHERE city = '%s'", getCompanyTableName(),
                COMPANY_2, CITY_2));

        //validate that event for delete is in queue
        receiveResponse(204, is(emptyOrNullString()));
        //validate that event for create is in queue
        receiveResponse(200, containsString(COMPANY_2 + "_changed"));
    }

    @Disabled("https://github.com/apache/camel-quarkus/issues/6911")
    @Test
    @Order(3)
    public void testDelete() throws SQLException {
        isInitialized("Test 'testDelete' is skipped, because container is not running.");

        int res = executeUpdate("DELETE FROM " + getCompanyTableName());
        int i = 0;
        for (i = 0; i < res; i++) {
            //validate that event for delete is in queue
            receiveResponse(204, is(emptyOrNullString()));
        }
        assertTrue(i > 1, "No records were deleted");
    }

    protected Response receiveResponse() {
        return receiveResponse("/receive");
    }

    protected Response receiveResponse(String method) {
        return RestAssured.get(type.getComponent() + method);
    }

    protected void receiveResponse(int statusCode, Matcher<String> stringMatcher) {
        receiveResponse().then()
                .statusCode(statusCode)
                .body(stringMatcher);
    }

    protected void receiveResponse(int statusCode, Matcher<String> stringMatcher, String methodName) {
        receiveResponse(methodName).then()
                .statusCode(statusCode)
                .body(stringMatcher);
    }

    protected int executeUpdate(String sql) throws SQLException {
        try (Statement statement = getConnection().createStatement()) {
            return statement.executeUpdate(sql);
        }
    }

}
