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
package org.apache.camel.quarkus.component.azure.storage.queue.it;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.apache.camel.quarkus.test.support.azure.AzureStorageTestResource;

import static org.hamcrest.core.Is.is;

@QuarkusTest
@QuarkusTestResource(AzureStorageTestResource.class)
class AzureStorageQueueTest {

    //@Test
    public void crud() {
        String message = "Hello Camel Quarkus Azure Queue";

        // Create
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .post("/azure-storage-queue/queue/create")
                .then()
                .statusCode(201);

        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(message)
                .post("/azure-storage-queue/queue/message")
                .then()
                .statusCode(201);

        // Read
        RestAssured.get("/azure-storage-queue/queue/read")
                .then()
                .statusCode(200)
                .body(is(message));

        // Delete
        RestAssured.delete("/azure-storage-queue/queue/delete")
                .then()
                .statusCode(204);
    }

}
