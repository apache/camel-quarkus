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
import org.apache.camel.component.salesforce.api.dto.RestResources;
import org.apache.camel.component.salesforce.api.dto.SObjectBasicInfo;
import org.apache.camel.component.salesforce.api.dto.SObjectDescription;
import org.apache.camel.component.salesforce.api.dto.Versions;
import org.apache.camel.quarkus.component.salesforce.model.GlobalObjectsAndHeaders;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.is;
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
        RestAssured.get("/salesforce/account")
                .then()
                .statusCode(200)
                .body(
                        "Id", not(emptyString()),
                        "AccountNumber", not(emptyString()));
    }

    @Test
    public void testGetAccountByQueryHelper() {
        RestAssured.get("/salesforce/account/query")
                .then()
                .statusCode(200)
                .body(
                        "Id", not(emptyString()),
                        "AccountNumber", not(emptyString()));
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
        GlobalObjectsAndHeaders globalObjectsAndHeaders = RestAssured.given()
                .get("/salesforce/sobjects/force-limit")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .as(GlobalObjectsAndHeaders.class);

        assertNotNull(globalObjectsAndHeaders);
        assertNotNull(globalObjectsAndHeaders.getGlobalObjects());
        assertTrue(globalObjectsAndHeaders.getHeader("Sforce-Limit-Info").contains("api-usage="));
    }

    @Test
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
        RestResources restResources = RestAssured.given()
                .get("/salesforce/resources")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .as(RestResources.class);
        assertNotNull(restResources);
    }

    @Test
    void testAccountWithBasicInfo() {
        // create an object of type Account
        String accountName = "Camel Quarkus Account Test: " + UUID.randomUUID().toString();
        final String accountId = RestAssured.given()
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
        assertTrue(recentItems.stream().anyMatch(recentItem -> recentItem.getAttributes().getUrl().contains(accountId)));

        // Get Account - querying Sobject by ID
        RestAssured.get("/salesforce/account/" + accountId)
                .then()
                .statusCode(200)
                .body(
                        "Id", not(emptyString()),
                        "AccountNumber", not(emptyString()));

        // delete the account
        // Clean up
        RestAssured.delete("/salesforce/account/" + accountId)
                .then()
                .statusCode(204);
    }

    @Test
    void testGetAccountDescription() {
        SObjectDescription accountDescription = RestAssured.given()
                .get("/salesforce/describe/account")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .as(SObjectDescription.class);
        assertNotNull(accountDescription);
    }

    @Test
    void testLimits() {
        final Map<String, Object> limits = RestAssured.given()
                .get("/salesforce/limits")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .as(Map.class);

        assertNotNull(limits, "Should fetch limits from Salesforce REST API");
        assertNotNull(limits.get("concurrentAsyncGetReportInstances"));
        assertNotNull(limits.get("concurrentSyncReportRuns"));
        assertNotNull(limits.get("dailyApiRequests"));
        assertNotNull(limits.get("dailyAsyncApexExecutions"));
        assertNotNull(limits.get("dailyBulkApiRequests"));
    }

}
