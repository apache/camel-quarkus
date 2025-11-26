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

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

/**
 * Integration test for IBM COS Consumer.
 */
@EnabledIfSystemProperty(named = "camel.ibm.cos.apiKey", matches = ".*", disabledReason = "IBM COS API Key not provided")
@EnabledIfSystemProperty(named = "camel.ibm.cos.serviceInstanceId", matches = ".*", disabledReason = "IBM COS Service Instance ID not provided")
@EnabledIfSystemProperty(named = "camel.ibm.cos.endpointUrl", matches = ".*", disabledReason = "IBM COS Endpoint URL not provided")
@QuarkusTest
class IBMCloudObjectStorageTest {

    @Test
    void basicOperations() {
        try {
            // Create Bucket
            given()
                    .post("/ibm-cos/bucket/create")
                    .then()
                    .statusCode(201);

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

        } finally {
            // Delete Bucket
            given()
                    .post("/ibm-cos/bucket/delete")
                    .then()
                    .statusCode(201);
        }
    }
}
