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

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.is;

@QuarkusTest
@QuarkusTestResource(MongoDbTestResource.class)
class MongodbGridfsTest {

    @Test
    public void testMongodbGridfsComponent() {
        final String fileContent = "Hello Camel Quarkus MongoDB GridFS";

        // Upload file
        String objectId = RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(fileContent)
                .post("/mongodb-gridfs/upload/test.txt")
                .then()
                .statusCode(201)
                .extract()
                .body()
                .asString();

        Assertions.assertNotNull(objectId);

        // Retrieve file
        RestAssured.given()
                .get("/mongodb-gridfs/get/test.txt")
                .then()
                .statusCode(200)
                .body(is(fileContent));

        // Delete file
        RestAssured.given()
                .delete("/mongodb-gridfs/delete/test.txt")
                .then()
                .statusCode(204);

        // Verify file deletion
        RestAssured.given()
                .get("/mongodb-gridfs/get/test.txt")
                .then()
                .statusCode(404);
    }
}
