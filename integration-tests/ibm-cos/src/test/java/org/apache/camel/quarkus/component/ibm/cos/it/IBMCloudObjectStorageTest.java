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
package org.apache.camel.quarkus.component.ibm.cos.it;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.is;

/**
 * Integration test for IBM COS Consumer.
 */
@EnabledIfSystemProperty(named = "camel.ibm.cos.apiKey", matches = ".*", disabledReason = "IBM COS API Key not provided")
@EnabledIfSystemProperty(named = "camel.ibm.cos.serviceInstanceId", matches = ".*", disabledReason = "IBM COS Service Instance ID not provided")
@EnabledIfSystemProperty(named = "camel.ibm.cos.endpointUrl", matches = ".*", disabledReason = "IBM COS Endpoint URL not provided")
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class IBMCloudObjectStorageTest {

    @Test
    @Order(0)
    void setup() {
        // Create Bucket (shared by all tests)
        given()
                .post("/ibm-cos/bucket/create")
                .then()
                .statusCode(201);
    }

    @Test
    @Order(1)
    void basicOperations() {
        String contentInitial = "Hello Camel Quarkus IBM Cloud Object Storage";

        // Create Object
        given()
                .contentType(ContentType.TEXT)
                .body(contentInitial)
                .post("/ibm-cos/object/put")
                .then()
                .statusCode(201);

        // Read Object
        given()
                .get("/ibm-cos/object/read")
                .then()
                .statusCode(200)
                .body(is(contentInitial));

        // List Objects in bucket
        given()
                .get("/ibm-cos/list")
                .then()
                .statusCode(200)
                .body("objects[0].key", is(IBMCloudObjectStorageRoutes.KEY_OF_OBJECT_CREATED));

        // Delete Object
        given()
                .body(contentInitial)
                .post("/ibm-cos/object/delete")
                .then()
                .statusCode(201);
    }

    @Test
    @Order(2)
    void consumerBasic() {
        final String content = "Hello Consumer";

        // Upload object
        given()
                .contentType(ContentType.TEXT)
                .body(content)
                .post("/ibm-cos/object/put")
                .then()
                .statusCode(201);

        // Start consumer
        given()
                .post("/ibm-cos/consumer/" + IBMCloudObjectStorageRoutes.CONSUME_ROUTE_BASIC + "/start")
                .then()
                .statusCode(204);

        // Wait for consumer to receive the message
        await().atMost(10, TimeUnit.SECONDS)
                .pollDelay(500, TimeUnit.MILLISECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .until(() -> {
                    String countStr = given().get("/ibm-cos/consumer/consumerBasic/count")
                            .then().statusCode(200).extract().asString();
                    int count = Integer.parseInt(countStr);
                    return count >= 1;
                });

        // Stop consumer
        given()
                .post("/ibm-cos/consumer/" + IBMCloudObjectStorageRoutes.CONSUME_ROUTE_BASIC + "/stop")
                .then()
                .statusCode(204);

        // Get consumed messages
        String consumedMessages = given()
                .get("/ibm-cos/consumer/consumerBasic")
                .then()
                .statusCode(200)
                .extract()
                .body().asString();

        // Verify message consumed
        assertThat(consumedMessages).isEqualTo(content);

        // Verify object deleted (deleteAfterRead)
        int objectCount = given()
                .get("/ibm-cos/list")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath().getList("objects").size();

        assertThat(objectCount).isZero();
    }

    @Test
    @Order(3)
    void consumerMultipleObjects() {
        final Map<String, String> objectsToUpload = new HashMap<>();
        objectsToUpload.put("obj-1", "Content 1");
        objectsToUpload.put("obj-2", "Content 2");
        objectsToUpload.put("obj-3", "Content 3");
        objectsToUpload.put("obj-4", "Content 4");
        objectsToUpload.put("obj-5", "Content 5");

        // Upload 5 objects with different keys
        for (Map.Entry<String, String> entry : objectsToUpload.entrySet()) {
            given()
                    .contentType(ContentType.TEXT)
                    .body(entry.getValue())
                    .post("/ibm-cos/object/put/" + entry.getKey())
                    .then()
                    .statusCode(201);
        }

        // Start consumer
        given()
                .post("/ibm-cos/consumer/" + IBMCloudObjectStorageRoutes.CONSUME_ROUTE_MULTIPLE + "/start")
                .then()
                .statusCode(204);

        // Wait for consumer to receive all 5 messages
        await().atMost(20, TimeUnit.SECONDS)
                .pollDelay(500, TimeUnit.MILLISECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .until(() -> {
                    String countStr = given().get("/ibm-cos/consumer/consumerMultipleObjects/count")
                            .then().statusCode(200).extract().asString();
                    int count = Integer.parseInt(countStr);
                    return count >= 5;
                });

        // Stop consumer
        given()
                .post("/ibm-cos/consumer/" + IBMCloudObjectStorageRoutes.CONSUME_ROUTE_MULTIPLE + "/stop")
                .then()
                .statusCode(204);

        // Get consumed messages
        String consumedMessages = given()
                .get("/ibm-cos/consumer/consumerMultipleObjects")
                .then()
                .statusCode(200)
                .extract()
                .body().asString();

        // Verify all messages consumed (split by newline)
        String[] messages = consumedMessages.split("\n");
        assertThat(messages).hasSize(5);

        // Verify all contents present
        Set<String> expectedContents = new HashSet<>(objectsToUpload.values());
        Set<String> actualContents = new HashSet<>(Arrays.asList(messages));
        assertThat(actualContents).isEqualTo(expectedContents);

        // Verify bucket empty (all objects deleted)
        int objectCount = given()
                .get("/ibm-cos/list")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath().getList("objects").size();

        assertThat(objectCount).isZero();
    }

    @Test
    @Order(4)
    void multipleObjectsOperations() {
        final Map<String, String> objectsToUpload = new HashMap<>();
        objectsToUpload.put("obj-1", "Content for obj-1");
        objectsToUpload.put("obj-2", "Content for obj-2");
        objectsToUpload.put("obj-3", "Content for obj-3");
        objectsToUpload.put("obj-4", "Content for obj-4");
        objectsToUpload.put("obj-5", "Content for obj-5");

        // Upload 5 objects with different keys
        for (Map.Entry<String, String> entry : objectsToUpload.entrySet()) {
            given()
                    .contentType(ContentType.TEXT)
                    .body(entry.getValue())
                    .post("/ibm-cos/object/put/" + entry.getKey())
                    .then()
                    .statusCode(201);
        }

        // List objects - verify 5 objects
        int objectCount = given()
                .get("/ibm-cos/list")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath().getList("objects").size();

        assertThat(objectCount).isEqualTo(5);

        // Get each object - verify content
        for (Map.Entry<String, String> entry : objectsToUpload.entrySet()) {
            String content = given()
                    .get("/ibm-cos/object/read/" + entry.getKey())
                    .then()
                    .statusCode(200)
                    .extract()
                    .body().asString();

            assertThat(content).isEqualTo(entry.getValue());
        }

        // Copy object obj-1 to obj-1-copy
        given()
                .post("/ibm-cos/object/copy/obj-1/obj-1-copy")
                .then()
                .statusCode(201);

        // Verify copy exists and content matches
        String copiedContent = given()
                .get("/ibm-cos/object/read/obj-1-copy")
                .then()
                .statusCode(200)
                .extract()
                .body().asString();

        assertThat(copiedContent).isEqualTo("Content for obj-1");

        // List objects - verify 6 objects now
        objectCount = given()
                .get("/ibm-cos/list")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath().getList("objects").size();

        assertThat(objectCount).isEqualTo(6);

        // Delete objects batch: obj-2, obj-3, obj-4
        given()
                .contentType(ContentType.JSON)
                .body("[\"obj-2\", \"obj-3\", \"obj-4\"]")
                .post("/ibm-cos/objects/delete")
                .then()
                .statusCode(201);

        // List objects - verify 3 objects remain
        objectCount = given()
                .get("/ibm-cos/list")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath().getList("objects").size();

        assertThat(objectCount).isEqualTo(3);

        // Verify correct objects remain (obj-1, obj-5, obj-1-copy)
        List<String> remainingKeys = given()
                .get("/ibm-cos/list")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath().getList("objects.key", String.class);

        assertThat(remainingKeys).contains("obj-1", "obj-5", "obj-1-copy");

        // Delete remaining objects using deleteObjects (batch)
        given()
                .contentType(ContentType.JSON)
                .body("[\"obj-1\", \"obj-5\", \"obj-1-copy\"]")
                .post("/ibm-cos/objects/delete")
                .then()
                .statusCode(201);

        // List objects - verify bucket empty
        objectCount = given()
                .get("/ibm-cos/list")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath().getList("objects").size();

        assertThat(objectCount).isZero();
    }

    @Test
    @Order(5)
    void getObjectRangeOperation() {
        final String testContent = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        // Upload object
        given()
                .contentType(ContentType.TEXT)
                .body(testContent)
                .post("/ibm-cos/object/put")
                .then()
                .statusCode(201);

        // Get range 0-9
        String range1 = given()
                .queryParam("start", 0)
                .queryParam("end", 9)
                .get("/ibm-cos/object/range/" + IBMCloudObjectStorageRoutes.KEY_OF_OBJECT_CREATED)
                .then()
                .statusCode(200)
                .extract()
                .body().asString();

        assertThat(range1).isEqualTo("0123456789");

        // Get range 10-35
        String range2 = given()
                .queryParam("start", 10)
                .queryParam("end", 35)
                .get("/ibm-cos/object/range/" + IBMCloudObjectStorageRoutes.KEY_OF_OBJECT_CREATED)
                .then()
                .statusCode(200)
                .extract()
                .body().asString();

        assertThat(range2).isEqualTo("ABCDEFGHIJKLMNOPQRSTUVWXYZ");

        // Delete object
        given()
                .post("/ibm-cos/object/delete")
                .then()
                .statusCode(201);
    }

    @Test
    @Order(6)
    void autoCreateBucket() {
        final String content = "Test autoCreateBucket content";

        // Put object with autoCreateBucket=true
        given()
                .contentType(ContentType.TEXT)
                .body(content)
                .post("/ibm-cos/object/put-autocreate")
                .then()
                .statusCode(201);

        // Read object from auto-created bucket
        String readContent = given()
                .get("/ibm-cos/object/read-autocreate")
                .then()
                .statusCode(200)
                .extract()
                .body().asString();

        assertThat(readContent).isEqualTo(content);

        // Delete object
        given().post("/ibm-cos/object/delete-autocreate")
                .then()
                .statusCode(201);
    }

    @Test
    @Order(7)
    void finalCleanup() {
        // Cleanup bucket
        given()
                .post("/ibm-cos/bucket/delete")
                .then()
                .statusCode(201);
        // Cleanup auto created bucket
        given()
                .post("/ibm-cos/bucket/delete-autocreate")
                .then()
                .statusCode(201);
    }
}
