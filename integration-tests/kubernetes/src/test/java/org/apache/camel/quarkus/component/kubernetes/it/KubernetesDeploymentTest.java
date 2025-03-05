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
package org.apache.camel.quarkus.component.kubernetes.it;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import io.fabric8.kubernetes.api.model.WatchEventBuilder;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentListBuilder;
import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.KubernetesTestServer;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.is;

@QuarkusTest
@QuarkusTestResource(CamelQuarkusKubernetesServerTestResource.class)
class KubernetesDeploymentTest {

    @KubernetesTestServer
    private KubernetesServer mockServer;

    @Test
    void deploymentOperations() throws Exception {
        try (CamelKubernetesNamespace namespace = new CamelKubernetesNamespace()) {
            namespace.awaitCreation();

            String name = "camel-deployment";

            Deployment deployment = new DeploymentBuilder()
                    .withNewMetadata()
                    .withName(name)
                    .endMetadata()
                    .withNewSpec()
                    .withReplicas(0)
                    .withNewSelector()
                    .addToMatchLabels("app", name)
                    .endSelector()
                    .withNewTemplate()
                    .withNewMetadata()
                    .addToLabels("app", name)
                    .endMetadata()
                    .withNewSpec()
                    .addNewContainer()
                    .withName(name)
                    .withImage("busybox:latest")
                    .endContainer()
                    .endSpec()
                    .endTemplate()
                    .endSpec()
                    .build();

            // Create
            RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body(deployment)
                    .when()
                    .post("/kubernetes/deployment/" + namespace.getNamespace())
                    .then()
                    .statusCode(201)
                    .body("metadata.name", is(name),
                            "metadata.namespace", is(namespace.getNamespace()));

            // Read
            Deployment currentDeployment = RestAssured.given()
                    .when()
                    .get("/kubernetes/deployment/" + namespace.getNamespace() + "/" + name)
                    .then()
                    .statusCode(200)
                    .body("metadata.name", is(name),
                            "metadata.namespace", is(namespace.getNamespace()))
                    .extract()
                    .as(Deployment.class);

            // Update
            int value = 120;
            Deployment updatedDeployment = new DeploymentBuilder(currentDeployment)
                    .editSpec()
                    .withMinReadySeconds(value)
                    .endSpec()
                    .build();

            RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body(updatedDeployment)
                    .when()
                    .put("/kubernetes/deployment/" + namespace.getNamespace())
                    .then()
                    .statusCode(200)
                    .body("spec.minReadySeconds", is(value));

            Awaitility.await().pollInterval(Duration.ofMillis(250)).atMost(Duration.ofMinutes(1)).untilAsserted(() -> {
                RestAssured.given()
                        .when()
                        .get("/kubernetes/deployment/" + namespace.getNamespace() + "/" + name)
                        .then()
                        .statusCode(200)
                        .body("spec.minReadySeconds", is(value));
            });

            if (mockServer != null) {
                mockServer.expect()
                        .get()
                        .withPath("/apis/apps/v1/namespaces/" + namespace.getNamespace() + "/deployments")
                        .andReturn(200, new DeploymentListBuilder().addAllToItems(List.of(updatedDeployment)).build())
                        .once();

                mockServer.expect()
                        .get()
                        .withPath(
                                "/apis/apps/v1/namespaces/" + namespace.getNamespace() + "/deployments?labelSelector=app%3D"
                                        + name)
                        .andReturn(200, new DeploymentListBuilder().addAllToItems(List.of(updatedDeployment)).build())
                        .once();
            }

            // List
            RestAssured.given()
                    .when()
                    .get("/kubernetes/deployment/" + namespace.getNamespace())
                    .then()
                    .statusCode(200)
                    .body("[0].metadata.name", is(name),
                            "[0].metadata.namespace", is(namespace.getNamespace()));

            // List by labels
            RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body(Map.of("app", name))
                    .when()
                    .get("/kubernetes/deployment/labels/" + namespace.getNamespace())
                    .then()
                    .statusCode(200)
                    .body("[0].metadata.name", is(name),
                            "[0].metadata.namespace", is(namespace.getNamespace()));

            // Scale
            // Requires a real k8s server, as the scale operation returns deployment.getStatus().getReplicas() that is not available in the mock server
            if (mockServer == null) {
                int scaleReplicas = 1;
                RestAssured.given()
                        .when()
                        .post("/kubernetes/deployment/" + namespace.getNamespace() + "/" + name + "/" + scaleReplicas)
                        .then()
                        .statusCode(201);

                Awaitility.await().pollInterval(Duration.ofMillis(250)).atMost(Duration.ofMinutes(1)).untilAsserted(() -> {
                    RestAssured.given()
                            .when()
                            .get("/kubernetes/deployment/" + namespace.getNamespace() + "/" + name)
                            .then()
                            .statusCode(200)
                            .body("spec.replicas", is(scaleReplicas));
                });
            }

            // Delete
            RestAssured.given()
                    .when()
                    .delete("/kubernetes/deployment/" + namespace.getNamespace() + "/" + name)
                    .then()
                    .statusCode(204);

            Awaitility.await().pollInterval(Duration.ofMillis(250)).atMost(Duration.ofMinutes(1)).untilAsserted(() -> {
                RestAssured.given()
                        .contentType(ContentType.JSON)
                        .body(Map.of("app", name))
                        .when()
                        .get("/kubernetes/deployment/labels/" + namespace.getNamespace())
                        .then()
                        .statusCode(200)
                        .body("size()", is(0));
            });
        }
    }

    @Test
    void deploymentEvents() throws Exception {
        try (CamelKubernetesNamespace namespace = new CamelKubernetesNamespace()) {
            namespace.awaitCreation();

            String name = "camel-deployment-watched";
            Deployment deployment = new DeploymentBuilder()
                    .withNewMetadata()
                    .withName(name)
                    .withNamespace(namespace.getNamespace())
                    .endMetadata()
                    .withNewSpec()
                    .withReplicas(0)
                    .withNewSelector()
                    .addToMatchLabels("app", name)
                    .endSelector()
                    .withNewTemplate()
                    .withNewMetadata()
                    .addToLabels("app", name)
                    .endMetadata()
                    .withNewSpec()
                    .addNewContainer()
                    .withName(name)
                    .withImage("busybox:latest")
                    .endContainer()
                    .endSpec()
                    .endTemplate()
                    .endSpec()
                    .build();

            if (mockServer != null) {
                String deploymentEvent = Serialization
                        .asJson(new WatchEventBuilder().withType("ADDED").withObject(deployment).build()) + "\n";
                String clientNamespace = mockServer.getClient().getNamespace();
                mockServer.expect()
                        .get()
                        .withPath("/apis/apps/v1/namespaces/" + clientNamespace
                                + "/deployments?allowWatchBookmarks=true&watch=true")
                        .andReturn(200, deploymentEvent)
                        .always();
            }

            RestAssured.given()
                    .queryParam("namespace", namespace.getNamespace())
                    .when()
                    .post("/kubernetes/route/deployment-listener/start")
                    .then()
                    .statusCode(204);

            RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body(deployment)
                    .when()
                    .post("/kubernetes/deployment/" + namespace.getNamespace())
                    .then()
                    .statusCode(201)
                    .body("metadata.name", is(name),
                            "metadata.namespace", is(namespace.getNamespace()));

            RestAssured.given()
                    .when()
                    .delete("/kubernetes/deployment/" + namespace.getNamespace() + "/" + name)
                    .then()
                    .statusCode(204);

            RestAssured.given()
                    .get("/kubernetes/deployment/events")
                    .then()
                    .statusCode(200)
                    .body("metadata.name", is(name),
                            "metadata.namespace", is(namespace.getNamespace()));
        } finally {
            RestAssured.given()
                    .when()
                    .post("/kubernetes/route/deployment-listener/stop")
                    .then()
                    .statusCode(204);
        }
    }
}
