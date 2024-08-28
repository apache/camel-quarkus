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

import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import org.apache.camel.component.salesforce.api.dto.RecentItem;
import org.apache.camel.component.salesforce.api.dto.SObjectBasicInfo;
import org.apache.camel.component.salesforce.api.dto.Versions;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.IsNot.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@QuarkusTestResource(SalesforceTestResource.class)
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
        String accountId = null;
        String accountName = "Camel Quarkus Account Test: " + UUID.randomUUID();

        try {
            accountId = RestAssured.given()
                    .body(accountName)
                    .post("/salesforce/account")
                    .then()
                    .statusCode(200)
                    .extract()
                    .body()
                    .asString();

            RestAssured.get("/salesforce/account/" + accountId)
                    .then()
                    .statusCode(200)
                    .body(
                            "Id", is(accountId),
                            "AccountNumber", not(emptyString()));
        } finally {
            if (accountId != null) {
                RestAssured.delete("/salesforce/account/" + accountId)
                        .then()
                        .statusCode(204);
            }
        }
    }

    @Test
    public void testGetAccountByQueryRecords() {
        String accountId = null;
        String accountName = "Camel Quarkus Account Test: " + UUID.randomUUID();

        try {
            accountId = RestAssured.given()
                    .body(accountName)
                    .post("/salesforce/account")
                    .then()
                    .statusCode(200)
                    .extract()
                    .body()
                    .asString();

            RestAssured.get("/salesforce/account/query/" + accountId)
                    .then()
                    .statusCode(200)
                    .body(
                            "Id", not(emptyString()),
                            "AccountNumber", not(emptyString()));
        } finally {
            if (accountId != null) {
                RestAssured.delete("/salesforce/account/" + accountId)
                        .then()
                        .statusCode(204);
            }
        }
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
    void testGlobalObjectsWithHeaders() {
        RestAssured.given()
                .get("/salesforce/sobjects/force-limit")
                .then()
                .statusCode(200)
                .body(containsString("api-usage="));
    }

    @Test
    @SuppressWarnings("unchecked")
    void testGetVersions() {
        List<Versions> versions = RestAssured.given()
                .get("/salesforce/versions")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .as(List.class);
        assertNotNull(versions);
        assertNotEquals(0, versions.size());
    }

    @Test
    void testGetRestResources() {
        JsonPath resources = RestAssured.given()
                .get("/salesforce/resources")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .jsonPath();

        resources.getMap("$.")
                .values()
                .stream()
                .map(Object::toString)
                .filter(path -> path.startsWith("/services"))
                .forEach(value -> assertTrue(value.matches("/services/data/.*/.*")));
    }

    @Test
    void testAccountWithBasicInfo() {
        String accountId = null;
        String accountName = "Camel Quarkus Account Test: " + UUID.randomUUID();

        try {
            // create an object of type Account
            accountId = RestAssured.given()
                    .body(accountName)
                    .post("/salesforce/account")
                    .then()
                    .statusCode(200)
                    .extract()
                    .body()
                    .asString();

            // get Account basic info
            SObjectBasicInfo accountBasicInfo = RestAssured.given()
                    .get("/salesforce/basic-info/account")
                    .then()
                    .statusCode(200)
                    .extract()
                    .body()
                    .as(SObjectBasicInfo.class);
            assertNotNull(accountBasicInfo);
            List<RecentItem> recentItems = accountBasicInfo.getRecentItems();
            assertNotNull(recentItems);

            // make sure the created account is referenced
            boolean accountIdMatched = false;
            for (RecentItem recentItem : recentItems) {
                if (recentItem.getAttributes().getUrl().contains(accountId)) {
                    accountIdMatched = true;
                    break;
                }
            }
            assertTrue(accountIdMatched);

            // Get Account - querying Sobject by ID
            RestAssured.get("/salesforce/account/" + accountId)
                    .then()
                    .statusCode(200)
                    .body(
                            "Id", not(emptyString()),
                            "AccountNumber", not(emptyString()));
        } finally {
            if (accountId != null) {
                // delete the account
                // Clean up
                RestAssured.delete("/salesforce/account/" + accountId)
                        .then()
                        .statusCode(204);
            }
        }
    }

    @Test
    void testGetAccountDescription() {
        RestAssured.given()
                .get("/salesforce/describe/account")
                .then()
                .statusCode(200)
                .body(notNullValue());
    }

    @Test
    @SuppressWarnings("unchecked")
    void testLimits() {
        final Map<String, Integer> limits = RestAssured.given()
                .get("/salesforce/limits")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .as(Map.class);

        assertNotNull(limits, "Should fetch limits from Salesforce REST API");
        assertNotNull(limits.get("ConcurrentAsyncGetReportInstances"));
        assertNotNull(limits.get("ConcurrentSyncReportRuns"));
        assertNotNull(limits.get("DailyApiRequests"));
        assertNotNull(limits.get("DailyAsyncApexExecutions"));
        assertNotNull(limits.get("DailyBulkApiRequests"));
    }

}
