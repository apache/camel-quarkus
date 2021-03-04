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
import io.restassured.path.json.JsonPath;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnabledIfEnvironmentVariable(named = "SALESFORCE_USERNAME", matches = ".+")
@EnabledIfEnvironmentVariable(named = "SALESFORCE_PASSWORD", matches = ".+")
@EnabledIfEnvironmentVariable(named = "SALESFORCE_CLIENTID", matches = ".+")
@EnabledIfEnvironmentVariable(named = "SALESFORCE_CLIENTSECRET", matches = ".+")
@QuarkusTest
class SalesforceTest {

    @Test
    public void testGetDocumentRaw() {
        RestAssured.get("/salesforce/document/test")
                .then()
                .statusCode(200)
                .body("attributes.type", is("Document"));
    }

    @Test
    public void testGetAccountDTO() {
        RestAssured.get("/salesforce/account")
                .then()
                .statusCode(200)
                .body(
                        "id", not(emptyString()),
                        "accountNumber", not(emptyString()));
    }

    @Test
    public void testBulkJobApi() {
        // Create bulk job
        JsonPath jobInfo = RestAssured.given()
                .post("/salesforce/bulk")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .jsonPath();

        String id = jobInfo.getString("id");
        String state = jobInfo.getString("state");
        assertNotNull(id);
        assertTrue(id.length() > 0);
        assertEquals("OPEN", state);

        // Abort bulk job
        jobInfo = RestAssured.given()
                .queryParam("jobId", id)
                .delete("/salesforce/bulk")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .jsonPath();

        state = jobInfo.getString("state");
        assertEquals("ABORTED", state);
    }

    @Test
    public void testChangeDataCaptureEvents() {
        String accountId = null;
        try {
            // Start the Salesforce CDC consumer
            RestAssured.post("/salesforce/cdc/start")
                    .then()
                    .statusCode(200);

            // Create an account
            String accountName = "Camel Quarkus Account Test: " + UUID.randomUUID().toString();
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
        }
    }
}
