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
import com.arangodb.entity.BaseDocument;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertNull;

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
        String key = RestAssured.given()
                .contentType(ContentType.TEXT)
                .body("foo:bar")
                .put("/arangodb")
                .then()
                .statusCode(201)
                .extract().body().asString();

        RestAssured.get("/arangodb/" + key)
                .then()
                .statusCode(200)
                .body(containsString("foo:bar"));

        // delete document
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .delete("/arangodb/" + key)
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
                .body("key:myKey,foo:hello,gg:42")
                .post("/arangodb/myKey")
                .then()
                .statusCode(200);

        // get document by key
        RestAssured.get("/arangodb/myKey")
                .then()
                .statusCode(200)
                .body(both(containsString("foo:hello")).and(containsString("gg:42")));
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

        RestAssured.get("/arangodb/foo/bar1")
                .then()
                .statusCode(200)
                .body(containsString("_key:keyBar1"));

        RestAssured.get("/arangodb/foo/bar2")
                .then()
                .statusCode(200)
                .body(containsString("_key:keyBar2"));
    }

    @BeforeAll
    public static void setup() {
        String host = ConfigProvider.getConfig().getValue("camel.arangodb.host", String.class);
        Integer port = ConfigProvider.getConfig().getValue("camel.arangodb.port", Integer.class);
        arangoDb = new ArangoDB.Builder().host(host, port).build();
        arangoDb.createDatabase(DATABASE_NAME);
        ArangoDatabase arangoDatabase = arangoDb.db(DATABASE_NAME);
        arangoDatabase.createCollection(COLLECTION_NAME);
        collection = arangoDatabase.collection(COLLECTION_NAME);
    }

    @AfterAll
    public static void teardown() {
        arangoDb.shutdown();
    }

}
