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
package org.apache.camel.quarkus.component.couchdb.it;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@QuarkusTest
@QuarkusTestResource(CouchdbTestResource.class)
class CouchdbTest {

    @Test
    public void crudShouldSucceed() {

        // Create the initial revision of the document
        CouchdbTestDocument toBeCreated = new CouchdbTestDocument();
        toBeCreated.setValue("create");
        CouchdbTestDocument created = RestAssured.given().contentType(ContentType.JSON).body(toBeCreated).put("/couchdb/create")
                .then().statusCode(200).extract()
                .as(CouchdbTestDocument.class);
        assertNotNull(created);
        assertNotNull(created.getId());
        assertNotNull(created.getRevision());

        // Consult the initial revision of the document
        CouchdbTestDocument retrieved = RestAssured.given().contentType(ContentType.JSON).body(created).get("/couchdb/get")
                .then().statusCode(200).extract()
                .as(CouchdbTestDocument.class);
        assertNotNull(retrieved);
        assertEquals(created.getId(), retrieved.getId());
        assertEquals(created.getRevision(), retrieved.getRevision());
        assertEquals(created.getValue(), retrieved.getValue());

        // Update the initial revision of the document
        retrieved.setValue("update");
        CouchdbTestDocument updated = RestAssured.given().contentType(ContentType.JSON).body(retrieved).put("/couchdb/update")
                .then().statusCode(200).extract()
                .as(CouchdbTestDocument.class);
        assertNotNull(updated);
        assertEquals(retrieved.getId(), updated.getId());
        assertNotEquals(retrieved.getRevision(), updated.getRevision());

        // Consult the updated revision of the document
        retrieved = RestAssured.given().contentType(ContentType.JSON).body(updated).get("/couchdb/get")
                .then().statusCode(200).extract()
                .as(CouchdbTestDocument.class);
        assertNotNull(retrieved);
        assertEquals(updated.getId(), retrieved.getId());
        assertEquals(updated.getRevision(), retrieved.getRevision());
        assertEquals("update", retrieved.getValue());

        // Delete the document
        CouchdbTestDocument deleted = RestAssured.given().contentType(ContentType.JSON).body(updated).delete("/couchdb/delete")
                .then().statusCode(200).extract()
                .as(CouchdbTestDocument.class);
        assertNotNull(deleted);
        assertEquals(updated.getId(), deleted.getId());
        assertNotEquals(updated.getRevision(), deleted.getRevision());

        // Check that consulting the deleted document is no more possible
        RestAssured.given().contentType(ContentType.JSON).body(created).get("/couchdb/get").then().statusCode(204);
    }

}
