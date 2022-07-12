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
package org.apache.camel.quarkus.component.arangodb.it;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoDB;
import com.arangodb.ArangoDatabase;
import com.arangodb.DbName;
import com.arangodb.entity.BaseDocument;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@QuarkusTestResource(ArangodbTestResource.class)
class ArangodbTest {
    protected static final String DATABASE_NAME = "test";
    protected static final String COLLECTION_NAME = "camel";
    protected static ArangoDB arangoDb;
    protected static ArangoCollection collection;

    @Test
    public void testCreateGetDeleteDocument() {
        // create a new document
        RequestSpecification request = RestAssured.given()
                .contentType(ContentType.TEXT)
                .body("{\"foo\":\"bar\"}");
        Response response = request.put("/arangodb/camel");

        int statusCode = response.getStatusCode();
        assertEquals(201, statusCode);
        // getting key of the document inserted
        String key = response.body().asString().trim();

        // get the document by key
        request = RestAssured.given()
                .contentType(ContentType.TEXT);
        response = request.get("/arangodb/camel/" + key);
        statusCode = response.getStatusCode();
        assertEquals(200, statusCode);
        String document = response.body().asString().trim();
        assertTrue(document.contains("\"foo\":\"bar\""));

        // delete document
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .delete("/arangodb/camel/" + key)
                .then()
                .statusCode(200);

        // search for the document deleted
        BaseDocument documentDeleted = collection.getDocument(key, BaseDocument.class);
        assertNull(documentDeleted);
    }

    @Test
    public void testUpdateDocument() {
        // insert one Document
        BaseDocument myObject = new BaseDocument();
        myObject.setKey("myKey");
        myObject.addAttribute("foo", "bar");
        collection.insertDocument(myObject);

        // udpate document
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body("{\"key\":\"myKey\",\"foo\":\"hello\", \"gg\":\"42\"}")
                .post("/arangodb/camel/myKey")
                .then()
                .statusCode(200);

        // get document by key
        RequestSpecification request = RestAssured.given()
                .contentType(ContentType.TEXT);
        Response response = request.get("/arangodb/camel/myKey");
        assertEquals(200, response.getStatusCode());
        String document = response.body().asString().trim();
        assertTrue(document.contains("\"foo\":\"hello\""));
        assertTrue(document.contains("\"gg\":\"42\""));
    }

    @Test
    public void testAQLQuery() {
        // insert first Document
        BaseDocument myObject1 = new BaseDocument();
        myObject1.setKey("keyBar1");
        myObject1.addAttribute("foo", "bar1");
        collection.insertDocument(myObject1);

        // insert second Document
        BaseDocument myObject2 = new BaseDocument();
        myObject2.setKey("keyBar2");
        myObject2.addAttribute("foo", "bar2");
        collection.insertDocument(myObject2);

        // get document with foo = bar1
        RequestSpecification request = RestAssured.given()
                .contentType(ContentType.TEXT);
        Response response = request.get("/arangodb/camel/foo/bar1");
        assertEquals(200, response.getStatusCode());
        String result = response.body().asString();
        assertTrue(result.contains("\"_key\":\"keyBar1\""));

        //get document with foo = bar2
        request = RestAssured.given()
                .contentType(ContentType.TEXT);
        response = request.get("/arangodb/camel/foo/bar2");
        assertEquals(200, response.getStatusCode());
        result = response.body().asString().trim();
        assertTrue(result.contains("\"_key\":\"keyBar2\""));
    }

    @BeforeAll
    public static void setup() {
        String host = ConfigProvider.getConfig().getValue("camel.arangodb.host", String.class);
        Integer port = ConfigProvider.getConfig().getValue("camel.arangodb.port", Integer.class);
        arangoDb = new ArangoDB.Builder().host(host, port).build();
        arangoDb.createDatabase(DbName.of(DATABASE_NAME));
        ArangoDatabase arangoDatabase = arangoDb.db(DbName.of(DATABASE_NAME));
        arangoDatabase.createCollection(COLLECTION_NAME);
        collection = arangoDatabase.collection(COLLECTION_NAME);
    }

    @AfterAll
    public static void teardown() {
        arangoDb.shutdown();
    }

}
