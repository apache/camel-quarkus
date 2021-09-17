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

import java.util.UUID;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
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
            RestAssured.post("/salesforce/cdc/start")
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
            RestAssured.given()
                    .get("/salesforce/cdc")
                    .then()
                    .statusCode(200)
                    .body("Name", is(accountName));

            // Verify we can stream the Account as Object
            RestAssured.given()
                    .get("/salesforce/streaming")
                    .then()
                    .statusCode(200)
                    .body(equalTo(accountName));

            // Verify we can stream the Account as Raw payload
            RestAssured.given()
                    .get("/salesforce/streaming/raw")
                    .then()
                    .statusCode(200)
                    .body("Name", equalTo(accountName));

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
            RestAssured.post("/salesforce/cdc/stop")
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
        String event = given()
                .contentType(ContentType.JSON)
                .get("/salesforce/platform/event")
                .asString();
        assertTrue(event.contains("channel=/event/TestEvent__e"));
        assertTrue(event.contains("Test_Field__c=data"));
    }

}
