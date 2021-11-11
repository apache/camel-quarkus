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
package org.apache.camel.quarkus.component.debezium.common.it.mongodb;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.result.DeleteResult;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.apache.camel.quarkus.component.debezium.common.it.AbstractDebeziumTest;
import org.apache.camel.quarkus.component.debezium.common.it.Type;
import org.bson.Document;
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
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@QuarkusTest
@QuarkusTestResource(DebeziumMongodbTestResource.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DebeziumMongodbTest extends AbstractDebeziumTest {
    private static final Logger LOG = Logger.getLogger(DebeziumMongodbTest.class);

    //constant with value of Type.mongodb.getJdbcProperty
    public static final String PROPERTY_JDBC = "mongodb_jdbc";

    private static MongoClient mongoClient;

    private static MongoCollection companies;

    public DebeziumMongodbTest() {
        super(Type.mongodb);
    }

    @BeforeAll
    public static void setUp() throws SQLException {
        Config config = ConfigProvider.getConfig();

        final Optional<String> mongoUrl = config.getOptionalValue(Type.mongodb.getPropertyJdbc(), String.class);

        if (mongoUrl.isPresent()) {
            mongoClient = MongoClients.create(mongoUrl.get());
        } else {
            LOG.warn("Container is not running. Connection is not created.");
        }

        assumeTrue(mongoClient != null);

        MongoDatabase db = mongoClient.getDatabase("test");

        companies = db.getCollection("companies");
    }

    @BeforeEach
    public void before() {
        assumeTrue(mongoClient != null);
    }

    @AfterAll
    public static void cleanUp() throws SQLException {
        if (mongoClient != null) {
            mongoClient.close();
        }
    }

    @Override
    protected Connection getConnection() {
        throw new IllegalStateException("Not used");
    }

    @Override
    protected String getCompanyTableName() {
        throw new IllegalStateException("Not used");
    }

    @Test
    @Order(0)
    @EnabledIfSystemProperty(named = PROPERTY_JDBC, matches = ".*")
    public void testReceiveInit() {
        receiveResponse()
                .then()
                .statusCode(200)
                .body(containsString("init"));
    }

    @Override
    protected void insertCompany(String name, String city) {
        Document doc = new Document();
        doc.put("name", name);
        doc.put("city", city);
        companies.insertOne(doc);
    }

    @Override
    protected void isInitialized(String s) {
        assertNotNull(mongoClient, s);
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
        Document doc = new Document().append("name", COMPANY_2).append("city", CITY_2);
        companies.insertOne(doc);

        //validate that event is received
        receiveResponse(200, containsString(COMPANY_2));

        Document searchQuery = new Document().append("name", COMPANY_2);
        Document updateQuery = new Document().append("$set", new Document().append("city", CITY_2 + "_changed"));
        companies.updateMany(searchQuery, updateQuery);

        //validate that event for create is in queue
        receiveResponse(200, containsString(CITY_2 + "_changed"));
    }

    @Test
    @Order(3)
    @EnabledIfSystemProperty(named = PROPERTY_JDBC, matches = ".*")
    public void testDelete() throws SQLException {
        DeleteResult dr = companies.deleteMany(new Document().append("name", COMPANY_2));
        assertEquals(1, dr.getDeletedCount(), "Only one company should be deleted.");

        //validate that event for delete is in queue
        receiveResponse(200, equalTo("d"), "/receiveOperation");
    }
}
