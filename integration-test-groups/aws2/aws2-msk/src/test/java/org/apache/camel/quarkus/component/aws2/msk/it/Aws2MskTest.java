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
package org.apache.camel.quarkus.component.aws2.msk.it;

import java.util.concurrent.TimeUnit;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static org.hamcrest.Matchers.hasItem;

@QuarkusTest
@QuarkusTestResource(Aws2MskTestResource.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class Aws2MskTest {

    static final String CLUSTER_NAME = "cq-test-cluster";
    static final String CLUSTER_POJO_NAME = "cq-pojo-test-cluster";

    static String clusterArn;
    static String clusterPojoArn;

    @Test
    @Order(1)
    public void testCreateClusters() {
        clusterArn = RestAssured.given()
                .post("/aws2-msk/cluster/" + CLUSTER_NAME)
                .then()
                .statusCode(201)
                .extract().body().asString();

        clusterPojoArn = RestAssured.given()
                .post("/aws2-msk/cluster/" + CLUSTER_POJO_NAME + "/pojo")
                .then()
                .statusCode(201)
                .extract().body().asString();

        // Wait for both clusters to become ACTIVE before running further tests
        Awaitility.await()
                .atMost(30, TimeUnit.MINUTES)
                .pollDelay(0, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.MINUTES)
                .until(() -> "ACTIVE".equals(clusterState(clusterArn))
                        && "ACTIVE".equals(clusterState(clusterPojoArn)));
    }

    @Test
    @Order(2)
    public void testClusterOperations() {
        // List all clusters
        RestAssured.given()
                .get("/aws2-msk/clusters")
                .then()
                .statusCode(200)
                .body("$", hasItem(CLUSTER_NAME));

        // List cluster with filter
        RestAssured.given()
                .queryParam("filter", CLUSTER_NAME)
                .get("/aws2-msk/clusters")
                .then()
                .statusCode(200)
                .body("$", hasItem(CLUSTER_NAME));
    }

    @Test
    @Order(3)
    public void testClusterOperationsPojoRequest() {
        // List all clusters
        RestAssured.given()
                .get("/aws2-msk/clusters")
                .then()
                .statusCode(200)
                .body("$", hasItem(CLUSTER_POJO_NAME));
    }

    @Test
    @Order(4)
    public void testDeleteClusters() {
        RestAssured.given()
                .queryParam("clusterArn", clusterArn)
                .delete("/aws2-msk/cluster")
                .then()
                .statusCode(204);

        RestAssured.given()
                .queryParam("clusterArn", clusterPojoArn)
                .delete("/aws2-msk/cluster/pojo")
                .then()
                .statusCode(204);
    }

    @Test
    @Order(5)
    public void cleanUpVerification() {
        // Wait until both clusters are no longer listed as deletion is asynchronous in AWS
        Awaitility.await()
                .atMost(30, TimeUnit.MINUTES)
                .pollDelay(0, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.MINUTES)
                .until(() -> {
                    java.util.List<?> clusters = RestAssured.given()
                            .get("/aws2-msk/clusters")
                            .then()
                            .statusCode(200)
                            .extract().jsonPath().getList("$");
                    return !clusters.contains(CLUSTER_NAME) && !clusters.contains(CLUSTER_POJO_NAME);
                });
    }

    private static String clusterState(String arn) {
        return RestAssured.given()
                .queryParam("clusterArn", arn)
                .get("/aws2-msk/cluster/describe")
                .then()
                .statusCode(200)
                .extract().body().asString();
    }
}
