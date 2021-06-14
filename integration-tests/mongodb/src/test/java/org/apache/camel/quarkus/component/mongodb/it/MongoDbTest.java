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
package org.apache.camel.quarkus.component.mongodb.it;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import org.apache.camel.ServiceStatus;
import org.apache.camel.util.CollectionHelper;
import org.bson.Document;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.apache.camel.quarkus.component.mongodb.it.MongoDbRoute.COLLECTION_PERSISTENT_TAILING;
import static org.apache.camel.quarkus.component.mongodb.it.MongoDbRoute.COLLECTION_STREAM_CHANGES;
import static org.apache.camel.quarkus.component.mongodb.it.MongoDbRoute.COLLECTION_TAILING;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@QuarkusTestResource(MongoDbTestResource.class)
class MongoDbTest {
    public static final String MSG = "Hello Camel Quarkus Mongo DB";
    public static final int CAP_NUMBER = 1000;

    private static String COLLECTION_OUTPUT_TYPE_DOCUMENT_LIST = "outputTypeDocumentList";
    private static String COLLECTION_OUTPUT_TYPE_DOCUMENT = "outputTypeDocument";

    private static MongoClient mongoClient;

    private static MongoDatabase db;

    @BeforeAll
    public static void setUp() throws SQLException {
        final String mongoUrl = "mongodb://" + ConfigProvider.getConfig().getValue("quarkus.mongodb.hosts", String.class);

        if (mongoUrl != null) {
            mongoClient = MongoClients.create(mongoUrl);
        }

        db = mongoClient.getDatabase("test");
    }

    @AfterAll
    public static void cleanUp() throws SQLException {
        if (mongoClient != null) {
            mongoClient.close();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = { MongoDbResource.DEFAULT_MONGO_CLIENT_NAME, MongoDbResource.NAMED_MONGO_CLIENT_NAME })
    public void testMongoDbComponent(String namedClient) {
        // As we will create a route for each client, we use a different collection for each route
        String collectionName = String.format("%sCamelTest", namedClient);

        // Write to collection
        RestAssured.given()
                .contentType(ContentType.JSON)
                .body("{message:\"" + MSG + "\"}")
                .header("mongoClientName", namedClient)
                .post("/mongodb/collection/" + collectionName)
                .then()
                .statusCode(201);

        // Retrieve from collection
        JsonPath jsonPath = RestAssured
                .given()
                .header("mongoClientName", namedClient)
                .get("/mongodb/collection/" + collectionName)
                .then()
                .contentType(ContentType.JSON)
                .statusCode(200)
                .extract()
                .body()
                .jsonPath();

        List<Map<String, String>> documents = jsonPath.get();
        assertEquals(1, documents.size());

        Map<String, String> document = documents.get(0);
        assertEquals(MSG, document.get("message"));
    }

    @Test
    public void testDynamicOperation() {
        String collectionName = "dynamicCamelTest";

        // Write to collection
        RestAssured.given()
                .contentType(ContentType.JSON)
                .body("{message:\"" + MSG + "\"}")
                .header("mongoClientName", MongoDbResource.DEFAULT_MONGO_CLIENT_NAME)
                .post("/mongodb/collection/dynamic/" + collectionName)
                .then()
                .statusCode(200)
                .body("message", is(MSG));

        //count results with dynamic operation
        RestAssured.given()
                .contentType(ContentType.JSON)
                .body("{message:\"" + MSG + "\"}")
                .header("mongoClientName", MongoDbResource.DEFAULT_MONGO_CLIENT_NAME)
                .header("dynamicOperation", "count")
                .post("/mongodb/collection/dynamic/" + collectionName)
                .then()
                .statusCode(200)
                .body(is("1"));

    }

    @Test
    public void testTailingConsumer() throws Exception {
        MongoCollection collection = db.getCollection(COLLECTION_TAILING, Document.class);

        for (int i = 1; i <= (10 * CAP_NUMBER); i++) {
            collection.insertOne(new Document("increasing", i).append("string", "value" + i));

            //verify continuously
            if (i % CAP_NUMBER == 0) {
                waitAndResetTailingResults(CAP_NUMBER, "value" + i, COLLECTION_TAILING);
            }
        }
    }

    @Test
    public void testPersistentTailingConsumer() throws Exception {
        MongoCollection collection = db.getCollection(COLLECTION_PERSISTENT_TAILING, Document.class);

        for (int i = 1; i <= (3 * CAP_NUMBER); i++) {
            collection.insertOne(new Document("increasing", i).append("string", "value" + i));

            //verify continuously
            if (i % CAP_NUMBER == 0) {
                waitAndResetTailingResults(CAP_NUMBER, "value" + i, COLLECTION_PERSISTENT_TAILING);
            }
        }

        //restart route
        routeController(COLLECTION_PERSISTENT_TAILING, "stop", null);
        routeController(COLLECTION_PERSISTENT_TAILING, "status", ServiceStatus.Stopped.name());
        routeController(COLLECTION_PERSISTENT_TAILING, "start", null);
        routeController(COLLECTION_PERSISTENT_TAILING, "status", ServiceStatus.Started.name());

        for (int i = (3 * CAP_NUMBER + 1); i <= (6 * CAP_NUMBER); i++) {
            collection.insertOne(new Document("increasing", i).append("string", "value" + i));

            //verify continuously
            if (i % CAP_NUMBER == 0) {
                waitAndResetTailingResults(CAP_NUMBER, "value" + i, COLLECTION_PERSISTENT_TAILING);
            }
        }
    }

    private String routeController(String routeId, String operation, String expectedResult) {
        if (expectedResult == null) {
            RestAssured.given()
                    .get("/mongodb/route/" + routeId + "/" + operation)
                    .then().statusCode(204);
        } else {
            await().atMost(5, TimeUnit.SECONDS).until(
                    () -> RestAssured
                            .get("/mongodb/route/" + routeId + "/" + operation)
                            .then()
                            .statusCode(200)
                            .extract().asString(),
                    is(expectedResult));
        }

        return null;
    }

    @Test
    public void testStreamConsumerWithFilter() throws Exception {
        MongoCollection collection = db.getCollection(COLLECTION_STREAM_CHANGES, Document.class);

        for (int i = 1; i <= 10; i++) {
            collection.insertOne(new Document("increasing", i).append("string", "value" + i));
        }

        waitAndResetTailingResults(1, "value2", COLLECTION_STREAM_CHANGES);
    }

    @Test
    public void testConvertMapToDocument() {
        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(CollectionHelper.mapOf("key1", "val1", "key2", "val2"))
                .post("/mongodb/convertMapToDocument")
                .then()
                .statusCode(200)
                .body("clazz", is(Document.class.getName()), "key1", is("val1"), "key2", is("val2"));
    }

    @Test
    public void testConvertAnyObjectToDocument() {
        RestAssured.given()
                .body("Hello!")
                .post("/mongodb/convertAnyObjectToDocument")
                .then()
                .statusCode(200)
                .body("clazz", is(Document.class.getName()), "value", is("Hello!"));
    }

    @Test
    public void testOutputTypeDocumentList() throws Exception {
        MongoCollection collection = db.getCollection(COLLECTION_OUTPUT_TYPE_DOCUMENT_LIST, Document.class);

        collection.insertOne(new Document("name", "Sheldon"));
        collection.insertOne(new Document("name", "Irma"));
        collection.insertOne(new Document("name", "Leonard"));

        List results = RestAssured
                .given()
                .header("mongoClientName", MongoDbResource.DEFAULT_MONGO_CLIENT_NAME)
                .get("/mongodb/collectionAsList/" + COLLECTION_OUTPUT_TYPE_DOCUMENT_LIST)
                .then()
                .contentType(ContentType.JSON)
                .statusCode(200)
                .body("size()", is(3))
                .extract().as(List.class);

        assertTrue(results.contains("Sheldon"));
        assertTrue(results.contains("Irma"));
        assertTrue(results.contains("Leonard"));

    }

    @Test
    public void testOutputTypeDocument() throws Exception {
        MongoCollection collection = db.getCollection(COLLECTION_OUTPUT_TYPE_DOCUMENT, Document.class);

        collection.insertOne(new Document("name", "Sheldon"));

        RestAssured
                .given()
                .header("mongoClientName", MongoDbResource.DEFAULT_MONGO_CLIENT_NAME)
                .get("/mongodb/searchByNameAsDocument/" + COLLECTION_OUTPUT_TYPE_DOCUMENT + "/Sheldon")
                .then()
                .contentType(ContentType.JSON)
                .statusCode(200)
                .body("name", is("Sheldon"), "_id", not(nullValue()));

    }

    private void waitAndResetTailingResults(int expectedSize, String laststring, String resultId) {
        await().atMost(5, TimeUnit.SECONDS).until(
                () -> RestAssured
                        .given().contentType(ContentType.JSON)
                        .get("/mongodb/results/" + resultId)
                        .then()
                        .statusCode(200)
                        .extract().as(Map.class),
                m -> ((int) m.get("size") == expectedSize && laststring.equals(((Map) m.get("last")).get("string"))));

        RestAssured
                .given().contentType(ContentType.JSON)
                .get("/mongodb/resultsReset/" + resultId)
                .then()
                .statusCode(204);
    }
}
