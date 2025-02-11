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

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.KubernetesTestServer;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
@QuarkusTestResource(CamelQuarkusKubernetesServerTestResource.class)
class KubernetesPodsTest {

    @KubernetesTestServer
    private KubernetesServer mockServer;

    @Test
    void podOperations() throws Exception {
        try (CamelKubernetesNamespace namespace = new CamelKubernetesNamespace()) {
            namespace.awaitCreation();

            Container container = new Container();
            container.setImage("busybox:latest");
            container.setName("camel-pod");
            container.setCommand(List.of("/bin/sh", "-c", "while true; do echo 'Hello, World!'; sleep 5; done"));

            Map<String, String> labels = Map.of("app", "camel-pod");
            Pod pod = new PodBuilder()
                    .withNewMetadata()
                    .withName("camel-pod")
                    .withNamespace(namespace.getNamespace())
                    .withLabels(labels)
                    .endMetadata()
                    .withNewSpec()
                    .withContainers(container)
                    .endSpec()
                    .build();

            // Create
            RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body(pod)
                    .when()
                    .post("/kubernetes/pods/" + namespace.getNamespace() + "/camel-pod")
                    .then()
                    .statusCode(201)
                    .body("metadata.name", is("camel-pod"),
                            "metadata.namespace", is(namespace.getNamespace()),
                            "metadata.resourceVersion", notNullValue());

            // Read
            Pod currentPod = RestAssured.given()
                    .when()
                    .get("/kubernetes/pods/" + namespace.getNamespace() + "/camel-pod")
                    .then()
                    .statusCode(200)
                    .body("metadata.name", is("camel-pod"),
                            "metadata.namespace", is(namespace.getNamespace()),
                            "metadata.resourceVersion", notNullValue())
                    .extract()
                    .body()
                    .as(Pod.class);

            // Update
            Pod modifiedPod = new PodBuilder(currentPod)
                    .editSpec()
                    .withActiveDeadlineSeconds(60L)
                    .endSpec()
                    .build();

            RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body(modifiedPod)
                    .when()
                    .put("/kubernetes/pods/" + namespace.getNamespace() + "/camel-pod")
                    .then()
                    .statusCode(200);

            Awaitility.await().pollInterval(Duration.ofMillis(250)).atMost(Duration.ofMinutes(1)).untilAsserted(() -> {
                RestAssured.given()
                        .when()
                        .get("/kubernetes/pods/" + namespace.getNamespace() + "/camel-pod")
                        .then()
                        .statusCode(200)
                        .body("spec.activeDeadlineSeconds", is(60));
            });

            // List pods
            RestAssured.given()
                    .when()
                    .get("/kubernetes/pods/" + namespace.getNamespace())
                    .then()
                    .statusCode(200)
                    .body("[0].metadata.name", is("camel-pod"),
                            "[0].metadata.namespace", is(namespace.getNamespace()),
                            "[0].spec.activeDeadlineSeconds", is(60));

            // List by labels
            RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body(labels)
                    .when()
                    .get("/kubernetes/pods/labels/" + namespace.getNamespace())
                    .then()
                    .statusCode(200)
                    .body("[0].metadata.name", is("camel-pod"),
                            "[0].metadata.namespace", is(namespace.getNamespace()),
                            "[0].spec.activeDeadlineSeconds", is(60));

            // Delete
            RestAssured.given()
                    .when()
                    .delete("/kubernetes/pods/" + namespace.getNamespace() + "/camel-pod")
                    .then()
                    .statusCode(204);

            Awaitility.await().pollInterval(Duration.ofMillis(250)).atMost(Duration.ofMinutes(1)).untilAsserted(() -> {
                RestAssured.given()
                        .when()
                        .get("/kubernetes/pods/" + namespace.getNamespace())
                        .then()
                        .statusCode(200)
                        .body("size()", is(0));
            });
        }
    }
}
