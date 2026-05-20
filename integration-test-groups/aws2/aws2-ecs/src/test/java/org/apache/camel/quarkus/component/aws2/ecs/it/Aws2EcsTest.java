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
package org.apache.camel.quarkus.component.aws2.ecs.it;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
@QuarkusTestResource(Aws2EcsTestResource.class)
class Aws2EcsTest {

    static final String CLUSTER_NAME = "cq-test-cluster";

    @Test
    public void testEcsOperations() {
        String clusterArn = null;
        try {
            // Create cluster
            clusterArn = RestAssured.given()
                    .queryParam("clusterName", CLUSTER_NAME)
                    .post("/aws2-ecs/clusters")
                    .then()
                    .statusCode(200)
                    .body(notNullValue())
                    .extract().body().asString();

            // List clusters
            RestAssured.given()
                    .get("/aws2-ecs/clusters")
                    .then()
                    .statusCode(200)
                    .body("$", hasItem(clusterArn));

            // List clusters with max results
            RestAssured.given()
                    .queryParam("maxResults", 10)
                    .get("/aws2-ecs/clusters")
                    .then()
                    .statusCode(200)
                    .body("$", hasItem(clusterArn));

            // Describe cluster
            RestAssured.given()
                    .get("/aws2-ecs/clusters/" + CLUSTER_NAME)
                    .then()
                    .statusCode(200)
                    .body("clusterName", is(CLUSTER_NAME))
                    .body("clusterArn", is(clusterArn));
        } finally {
            // Clean up: delete cluster
            if (clusterArn != null) {
                RestAssured.given()
                        .delete("/aws2-ecs/clusters/" + CLUSTER_NAME)
                        .then()
                        .statusCode(200)
                        .body(is(clusterArn));
            }
        }
    }
}
