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
package org.apache.camel.quarkus.component.aws2.ec2.it;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.apache.camel.quarkus.test.support.aws2.Aws2TestResource;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
@QuarkusTestResource(Aws2TestResource.class)
class Aws2Ec2Test {

    static final String IMAGE_ID = "ami-test-12345";
    static final String IMAGE_ID_POJO = "ami-test-67890";

    @Test
    public void testEc2Operations() {
        String instanceId = null;
        String instanceIdPojo = null;
        try {
            // Create and run instances
            instanceId = RestAssured.given()
                    .queryParam("imageId", IMAGE_ID)
                    .post("/aws2-ec2/instances")
                    .then()
                    .statusCode(201)
                    .body(notNullValue())
                    .extract().body().asString();

            // Create and run instances (POJO)
            instanceIdPojo = RestAssured.given()
                    .queryParam("imageId", IMAGE_ID_POJO)
                    .post("/aws2-ec2/instances/pojo")
                    .then()
                    .statusCode(201)
                    .body(notNullValue())
                    .extract().body().asString();

            // Describe instances
            RestAssured.given()
                    .queryParam("instanceId", instanceId)
                    .get("/aws2-ec2/instances")
                    .then()
                    .statusCode(200)
                    .contentType(ContentType.JSON)
                    .body("$", hasItem(instanceId));

            // Create tags
            RestAssured.given()
                    .queryParam("key", "Name")
                    .queryParam("value", "CamelQuarkusTest")
                    .post("/aws2-ec2/instances/" + instanceId + "/tags")
                    .then()
                    .statusCode(201);

            // Describe instances status
            RestAssured.given()
                    .queryParam("instanceId", instanceId)
                    .get("/aws2-ec2/instances/status")
                    .then()
                    .statusCode(200)
                    .body(notNullValue());

            // Stop instances
            RestAssured.given()
                    .post("/aws2-ec2/instances/" + instanceId + "/stop")
                    .then()
                    .statusCode(200)
                    .body(anyOf(is("stopped"), is("stopping")));

            // Start instances
            RestAssured.given()
                    .post("/aws2-ec2/instances/" + instanceId + "/start")
                    .then()
                    .statusCode(200)
                    .body(anyOf(is("running"), is("pending")));

            // Reboot instances
            RestAssured.given()
                    .post("/aws2-ec2/instances/" + instanceId + "/reboot")
                    .then()
                    .statusCode(204);

            // Delete tags
            RestAssured.given()
                    .queryParam("key", "Name")
                    .delete("/aws2-ec2/instances/" + instanceId + "/tags")
                    .then()
                    .statusCode(204);
        } finally {
            // Clean up: terminate instances
            if (instanceId != null) {
                RestAssured.given()
                        .post("/aws2-ec2/instances/" + instanceId + "/terminate")
                        .then()
                        .statusCode(200)
                        .body(anyOf(is("terminated"), is("shutting-down")));
            }

            if (instanceIdPojo != null) {
                RestAssured.given()
                        .post("/aws2-ec2/instances/" + instanceIdPojo + "/terminate")
                        .then()
                        .statusCode(200)
                        .body(anyOf(is("terminated"), is("shutting-down")));
            }
        }
    }
}
