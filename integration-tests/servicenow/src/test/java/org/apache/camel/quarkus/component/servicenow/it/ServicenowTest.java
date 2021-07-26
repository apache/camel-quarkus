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
package org.apache.camel.quarkus.component.servicenow.it;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

import static org.hamcrest.Matchers.matchesPattern;

@QuarkusTest
@QuarkusTestResource(ServicenowTestResource.class)
class ServicenowTest {

    //@Test
    public void test() {
        // Create incident
        final String incidentSysId = RestAssured.given()
                .contentType(ContentType.TEXT)
                .body("Demo incident")
                .post("/servicenow/post")
                .then()
                .statusCode(201)
                .extract().body().asString();

        // Retrieve incident
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .queryParam("incidentSysId", incidentSysId)
                .get("/servicenow/get")
                .then()
                .statusCode(200)
                .body(matchesPattern("INC[0-9]+"));

        // Delete incident
        RestAssured.given()
                .queryParam("incidentSysId", incidentSysId)
                .delete("/servicenow/delete")
                .then()
                .statusCode(204);

        // Check it is deleted
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .queryParam("incidentSysId", incidentSysId)
                .get("/servicenow/get")
                .then()
                .statusCode(404);
    }
}
