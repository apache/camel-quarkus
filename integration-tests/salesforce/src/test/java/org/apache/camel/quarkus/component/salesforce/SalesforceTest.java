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

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
}
