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
package org.apache.camel.quarkus.component.salesforce;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnabledIfEnvironmentVariable(named = "SALESFORCE_USERNAME", matches = ".+")
@EnabledIfEnvironmentVariable(named = "SALESFORCE_PASSWORD", matches = ".+")
@EnabledIfEnvironmentVariable(named = "SALESFORCE_CLIENTID", matches = ".+")
@EnabledIfEnvironmentVariable(named = "SALESFORCE_CLIENTSECRET", matches = ".+")
@QuarkusTest
public class SalesforceIntegrationTest {

    @Test
    public void testCDCAndStreamingEvents() {
        String accountId = null;
        String topicId = null;
        try {
            // Start the Salesforce CDC consumer
            RestAssured.post("/salesforce/route/cdc/start")
                    .then()
                    .statusCode(200);

            // Create an account
            String accountName = "Camel Quarkus Account Test: " + UUID.randomUUID();
            accountId = RestAssured.given()
                    .body(accountName)
                    .post("/salesforce/account")
                    .then()
                    .statusCode(200)
                    .extract()
                    .body()
                    .asString();

            // Verify we captured the account creation event
            await().atMost(30, TimeUnit.SECONDS).until(() -> {
                Map result = RestAssured.given()
                        .get("/salesforce/cdc")
                        .body()
                        .as(Map.class);
                return result != null && accountName.equals(result.get(("Name")));
            });

            // Verify we can stream the Account as Object
            await().atMost(30, TimeUnit.SECONDS).until(() -> {
                String result = RestAssured.given()
                        .get("/salesforce/streaming")
                        .body()
                        .asString();
                return accountName.equals(result);
            });

            // Verify we can stream the Account as Raw payload
            await().atMost(30, TimeUnit.SECONDS).until(() -> {
                String result = RestAssured.given()
                        .get("/salesforce/streaming/raw")
                        .body()
                        .asString();
                return result != null && result.contains(accountName);
            });

            // Get the topic ID
            topicId = RestAssured.given()
                    .get("/salesforce/topic")
                    .then()
                    .statusCode(200)
                    .extract()
                    .body()
                    .asString();
            assertNotNull(topicId);
        } finally {
            // Shut down the CDC consumer
            RestAssured.post("/salesforce/route/cdc/stop")
                    .then()
                    .statusCode(200);

            // Clean up
            if (accountId != null) {
                RestAssured.delete("/salesforce/account/" + accountId)
                        .then()
                        .statusCode(204);
            }

            // delete the topic
            if (topicId != null) {
                RestAssured.delete("/salesforce/topic/" + topicId)
                        .then()
                        .statusCode(204);
            }
        }
    }

    @Test
    void testPlatformEvents() {
        try {
            RestAssured.post("/salesforce/route/platformEventTimer/start")
                    .then()
                    .statusCode(200);

            String event = given()
                    .contentType(ContentType.JSON)
                    .get("/salesforce/platform/event")
                    .asString();
            assertTrue(event.contains("/event/TestEvent__e"));
            assertTrue(event.contains("Test_Field__c"));
        } finally {
            RestAssured.post("/salesforce/route/platformEventTimer/stop")
                    .then()
                    .statusCode(200);
        }
    }

    @Test
    public void testPubSubAvro() {
        try {
            RestAssured.post("/salesforce/route/topicSubscribeAvro/start")
                    .then()
                    .statusCode(200);

            String createdBy = UUID.randomUUID().toString();
            long createdDate = Instant.now().toEpochMilli();
            String testFieldValue = UUID.randomUUID().toString();

            await().atMost(60, TimeUnit.SECONDS).pollInterval(10, TimeUnit.SECONDS).pollDelay(1, TimeUnit.SECONDS)
                    .untilAsserted(() -> {
                        RestAssured.given()
                                .queryParam("createdBy", createdBy)
                                .queryParam("createdDate", createdDate)
                                .queryParam("testFieldValue", testFieldValue)
                                .post("/salesforce/publish/event/avro")
                                .then()
                                .statusCode(204);

                        RestAssured.get("/salesforce/subscribe/event/avro")
                                .then()
                                .statusCode(200)
                                .body(
                                        "createdBy", is(createdBy),
                                        "createdDate", is(createdDate),
                                        "testFieldValue", is(testFieldValue));
                    });
        } finally {
            RestAssured.post("/salesforce/route/topicSubscribeAvro/stop")
                    .then()
                    .statusCode(200);
        }
    }

    @Test
    public void testPubSubGenericRecord() {
        try {
            RestAssured.post("/salesforce/route/topicSubscribeGenericRecord/start")
                    .then()
                    .statusCode(200);

            String createdBy = UUID.randomUUID().toString();
            long createdDate = Instant.now().toEpochMilli();
            String testFieldValue = UUID.randomUUID().toString();

            await().atMost(60, TimeUnit.SECONDS).pollInterval(10, TimeUnit.SECONDS).pollDelay(1, TimeUnit.SECONDS)
                    .untilAsserted(() -> {
                        RestAssured.given()
                                .queryParam("createdBy", createdBy)
                                .queryParam("createdDate", createdDate)
                                .queryParam("testFieldValue", testFieldValue)
                                .post("/salesforce/publish/event/generic/record")
                                .then()
                                .statusCode(204);

                        RestAssured.get("/salesforce/subscribe/event/generic/record")
                                .then()
                                .statusCode(200)
                                .body(
                                        "createdBy", is(createdBy),
                                        "createdDate", is(String.valueOf(createdDate)),
                                        "testFieldValue", is(testFieldValue));
                    });
        } finally {
            RestAssured.post("/salesforce/route/topicSubscribeGenericRecord/stop")
                    .then()
                    .statusCode(200);
        }
    }

    @Test
    public void testPubSubJson() {
        try {
            RestAssured.post("/salesforce/route/topicSubscribeJson/start")
                    .then()
                    .statusCode(200);

            String createdBy = UUID.randomUUID().toString();
            long createdDate = Instant.now().toEpochMilli();
            String testFieldValue = UUID.randomUUID().toString();
            String json = """
                            {
                                "CreatedById": "%s",
                                "CreatedDate": %d,
                                "Test_Field__c": "%s"
                            }
                    """;

            await().atMost(60, TimeUnit.SECONDS).pollInterval(10, TimeUnit.SECONDS).pollDelay(1, TimeUnit.SECONDS)
                    .untilAsserted(() -> {
                        RestAssured.given()
                                .contentType(ContentType.JSON)
                                .body(json.formatted(createdBy, createdDate, testFieldValue))
                                .post("/salesforce/publish/event/json")
                                .then()
                                .statusCode(204);

                        RestAssured.get("/salesforce/subscribe/event/json")
                                .then()
                                .statusCode(200)
                                .body(
                                        "CreatedById", is(createdBy),
                                        "CreatedDate", is(createdDate),
                                        "Test_Field__c", is(testFieldValue));
                    });
        } finally {
            RestAssured.post("/salesforce/route/topicSubscribeJson/stop")
                    .then()
                    .statusCode(200);
        }
    }

    @Test
    public void testPubSubPojo() {
        try {
            RestAssured.post("/salesforce/route/topicSubscribePojo/start")
                    .then()
                    .statusCode(200);

            String createdBy = UUID.randomUUID().toString();
            long createdDate = Instant.now().toEpochMilli();
            String testFieldValue = UUID.randomUUID().toString();

            await().atMost(60, TimeUnit.SECONDS).pollInterval(10, TimeUnit.SECONDS).pollDelay(1, TimeUnit.SECONDS)
                    .untilAsserted(() -> {
                        RestAssured.given()
                                .queryParam("createdBy", createdBy)
                                .queryParam("createdDate", createdDate)
                                .queryParam("testFieldValue", testFieldValue)
                                .post("/salesforce/publish/event/pojo")
                                .then()
                                .statusCode(204);

                        RestAssured.get("/salesforce/subscribe/event/pojo")
                                .then()
                                .statusCode(200)
                                .body(
                                        "createdBy", is(createdBy),
                                        "createdDate", is(createdDate),
                                        "testFieldValue", is(testFieldValue));
                    });
        } finally {
            RestAssured.post("/salesforce/route/topicSubscribePojo/stop")
                    .then()
                    .statusCode(200);
        }
    }
}
