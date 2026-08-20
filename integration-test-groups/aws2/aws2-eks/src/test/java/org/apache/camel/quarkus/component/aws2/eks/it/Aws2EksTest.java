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
package org.apache.camel.quarkus.component.aws2.eks.it;

import java.util.List;
import java.util.concurrent.TimeUnit;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.apache.camel.quarkus.test.support.aws2.Aws2TestResource;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
@QuarkusTestResource(Aws2TestResource.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class Aws2EksTest {

    static final String CLUSTER_NAME = "cq-test-cluster";

    static String clusterArn;

    @Test
    @Order(1)
    public void testCreateCluster() {
        clusterArn = RestAssured.given()
                .queryParam("clusterName", CLUSTER_NAME)
                .post("/aws2-eks/clusters")
                .then()
                .statusCode(200)
                .body(notNullValue())
                .extract().body().asString();

        // Wait for the cluster to become ACTIVE before running further tests
        Awaitility.await()
                .atMost(30, TimeUnit.MINUTES)
                .pollDelay(0, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.MINUTES)
                .until(() -> "ACTIVE".equals(clusterStatus(CLUSTER_NAME)));
    }

    @Test
    @Order(2)
    public void testClusterOperations() {
        // List all clusters (EKS returns cluster names, not ARNs)
        RestAssured.given()
                .get("/aws2-eks/clusters")
                .then()
                .statusCode(200)
                .body("$", hasItem(CLUSTER_NAME));

        // List clusters with max results: assert limit behavior only (order is not guaranteed)
        RestAssured.given()
                .queryParam("maxResults", 10)
                .get("/aws2-eks/clusters")
                .then()
                .statusCode(200)
                .body("$", hasSize(lessThanOrEqualTo(10)));

        // Describe cluster
        RestAssured.given()
                .get("/aws2-eks/clusters/" + CLUSTER_NAME)
                .then()
                .statusCode(200)
                .body("clusterName", is(CLUSTER_NAME))
                .body("clusterArn", is(clusterArn))
                .body("clusterStatus", is("ACTIVE"));
    }

    @Test
    @Order(3)
    public void testDeleteCluster() {
        RestAssured.given()
                .delete("/aws2-eks/clusters/" + CLUSTER_NAME)
                .then()
                .statusCode(200)
                .body(is(clusterArn));
    }

    @Test
    @Order(4)
    public void cleanUpVerification() {
        // Wait until the cluster is no longer listed as deletion is asynchronous in AWS
        Awaitility.await()
                .atMost(30, TimeUnit.MINUTES)
                .pollDelay(0, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.MINUTES)
                .until(() -> {
                    List<?> clusters = RestAssured.given()
                            .get("/aws2-eks/clusters")
                            .then()
                            .statusCode(200)
                            .extract().jsonPath().getList("$");
                    return !clusters.contains(CLUSTER_NAME);
                });
    }

    private static String clusterStatus(String clusterName) {
        return RestAssured.given()
                .get("/aws2-eks/clusters/" + clusterName)
                .then()
                .statusCode(200)
                .extract().jsonPath().getString("clusterStatus");
    }
}
