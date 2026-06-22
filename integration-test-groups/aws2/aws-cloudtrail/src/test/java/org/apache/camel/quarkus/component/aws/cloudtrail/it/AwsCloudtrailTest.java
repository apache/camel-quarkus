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
package org.apache.camel.quarkus.component.aws.cloudtrail.it;

import java.util.concurrent.TimeUnit;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;

import static org.awaitility.Awaitility.await;

@QuarkusTest
@QuarkusTestResource(AwsCloudtrailTestResource.class)
class AwsCloudtrailTest {

    static final String TRAIL_NAME = "cq-test-trail";

    @Test
    public void testCloudtrailOperations() throws Exception {
        String trailArn = null;
        try {
            // Create trail - tests AWS SDK management API and generates CloudTrail event
            trailArn = RestAssured.given()
                    .post("/aws-cloudtrail/trail/" + TRAIL_NAME)
                    .then()
                    .statusCode(201)
                    .extract().body().asString();

            // Wait for the Camel CloudTrail consumer to poll and consume events
            // Consumer runs automatically in background, polling every 1 second
            await().atMost(10, TimeUnit.SECONDS).until(() -> {
                int count = RestAssured.given()
                        .get("/aws-cloudtrail/consumer/events")
                        .then()
                        .statusCode(200)
                        .extract().as(Integer.class);
                return count > 0;
            });
        } finally {
            // Delete trail - cleanup
            if (trailArn != null) {
                RestAssured.given()
                        .delete("/aws-cloudtrail/trail/" + TRAIL_NAME)
                        .then()
                        .statusCode(204);
            }
        }
    }
}
