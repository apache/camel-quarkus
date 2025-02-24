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
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobBuilder;
import io.fabric8.kubernetes.api.model.batch.v1.JobListBuilder;
import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
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
class KubernetesJobTest {

    @KubernetesTestServer
    private KubernetesServer mockServer;

    @Test
    void jobOperations() throws Exception {
        try (CamelKubernetesNamespace namespace = new CamelKubernetesNamespace()) {
            namespace.awaitCreation();

            Container container = new Container();
            container.setImage("busybox:latest");
            container.setName("camel-job");
            container.setCommand(List.of("echo", "hello world"));

            Job job = new JobBuilder()
                    .withNewMetadata()
                    .withName("camel-job")
                    .withNamespace(namespace.getNamespace())
                    .endMetadata()
                    .withNewSpec()
                    .withNewTemplate()
                    .withNewSpec()
                    .withContainers()
                    .addToContainers(container)
                    .withRestartPolicy("Never")
                    .endSpec()
                    .endTemplate()
                    .endSpec()
                    .build();

            // Create
            RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body(job)
                    .when()
                    .post("/kubernetes/job/" + namespace.getNamespace() + "/camel-job")
                    .then()
                    .statusCode(201)
                    .body("metadata.name", is("camel-job"),
                            "metadata.namespace", is(namespace.getNamespace()));

            // Read
            Job currentJob = RestAssured.given()
                    .when()
                    .get("/kubernetes/job/" + namespace.getNamespace() + "/camel-job")
                    .then()
                    .statusCode(200)
                    .body("metadata.name", is("camel-job"),
                            "metadata.namespace", is(namespace.getNamespace()))
                    .extract()
                    .as(Job.class);

            // Update
            Job updatedJob = new JobBuilder(currentJob)
                    .editSpec()
                    .withBackoffLimit(5)
                    .endSpec()
                    .build();

            RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body(updatedJob)
                    .when()
                    .put("/kubernetes/job/" + namespace.getNamespace() + "/camel-job")
                    .then()
                    .statusCode(200)
                    .body("spec.backoffLimit", is(5));

            Awaitility.await().pollInterval(Duration.ofMillis(250)).atMost(Duration.ofMinutes(1)).untilAsserted(() -> {
                RestAssured.given()
                        .when()
                        .get("/kubernetes/job/" + namespace.getNamespace() + "/camel-job")
                        .then()
                        .statusCode(200)
                        .body("spec.backoffLimit", is(5));
            });

            String listNamespace = mockServer == null ? namespace.getNamespace() : "test";
            if (mockServer != null) {
                mockServer.expect()
                        .get()
                        .withPath("/apis/batch/v1/namespaces/" + listNamespace + "/jobs")
                        .andReturn(200, new JobListBuilder().addAllToItems(List.of(updatedJob)).build())
                        .once();

                mockServer.expect()
                        .get()
                        .withPath("/apis/batch/v1/namespaces/" + listNamespace + "/jobs?labelSelector=app%3Dcamel-job")
                        .andReturn(200, new JobListBuilder().addAllToItems(List.of(updatedJob)).build())
                        .once();

                // List
                RestAssured.given()
                        .when()
                        .get("/kubernetes/job/" + listNamespace)
                        .then()
                        .statusCode(200)
                        .body("[0].metadata.name", is("camel-job"),
                                "[0].metadata.namespace", is(namespace.getNamespace()));

                // List by labels
                RestAssured.given()
                        .contentType(ContentType.JSON)
                        .body(Map.of("app", "camel-job"))
                        .when()
                        .get("/kubernetes/job/labels/" + listNamespace)
                        .then()
                        .statusCode(200)
                        .body("[0].metadata.name", is("camel-job"),
                                "[0].metadata.namespace", is(namespace.getNamespace()));
            }

            // Delete
            RestAssured.given()
                    .when()
                    .delete("/kubernetes/job/" + namespace.getNamespace() + "/camel-job")
                    .then()
                    .statusCode(204);

            Awaitility.await().pollInterval(Duration.ofMillis(250)).atMost(Duration.ofMinutes(1)).untilAsserted(() -> {
                RestAssured.given()
                        .contentType(ContentType.JSON)
                        .body(Map.of("app", "camel-job"))
                        .when()
                        .get("/kubernetes/job/labels/" + namespace.getNamespace())
                        .then()
                        .statusCode(200)
                        .body("size()", is(0));
            });
        }
    }
}
