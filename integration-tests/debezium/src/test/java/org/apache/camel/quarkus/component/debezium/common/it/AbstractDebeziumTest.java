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

import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.hamcrest.Matcher;
import org.jboss.logging.Logger;
import org.junit.Assert;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.is;

/**
 * Abstract parent for debezium based tests.
 * Contains tests for insert, update and delete.
 */
public abstract class AbstractDebeziumTest {
    private static final Logger LOG = Logger.getLogger(AbstractDebeziumTest.class);

    private static String COMPANY_1 = "Best Company";
    private static String COMPANY_2 = "Even Better Company";
    private static String CITY_1 = "Prague";
    private static String CITY_2 = "Paris";
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
        if (getConnection() == null) {
            LOG.warn("Test 'testInsert' is skipped, because container is not running.");
            return;
        }

        int i = 0;

        while (i++ < REPEAT_COUNT) {
            executeUpdate(String.format("INSERT INTO %s (name, city) VALUES ('%s', '%s')", getCompanyTableName(),
                    COMPANY_1 + "_" + i, CITY_1));

            Response response = receiveResponse();

            //if status code is 204 (no response), try again
            if (response.getStatusCode() == 204) {
                LOG.debug("Response code 204. Debezium is not running yet, repeating (" + i + "/" + REPEAT_COUNT + ")");
                continue;
            }

            response
                    .then()
                    .statusCode(200)
                    .body(containsString((COMPANY_1 + "_" + i)));
            //if response is valid, no need for another inserts
            break;
        }

        Assert.assertTrue("Debezium does not respond (consider changing timeout in AbstractDebeziumResource).",
                i < REPEAT_COUNT);
    }

    @Test
    @Order(2)
    public void testUpdate() throws SQLException {
        if (getConnection() == null) {
            LOG.warn("Test 'testUpdate' is skipped, because container is not running.");
            return;
        }

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

    @Test
    @Order(3)
    public void testDelete() throws SQLException {
        if (getConnection() == null) {
            LOG.warn("Test 'testDelete' is skipped, because container is not running.");
            return;
        }

        int res = executeUpdate("DELETE FROM " + getCompanyTableName());
        int i = 0;
        for (i = 0; i < res; i++) {
            //validate that event for delete is in queue
            receiveResponse(204, is(emptyOrNullString()));
        }
        Assert.assertTrue("No records were deleted", i > 1);
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

    protected int executeUpdate(String sql) throws SQLException {
        try (Statement statement = getConnection().createStatement()) {
            return statement.executeUpdate(sql);
        }
    }

}
