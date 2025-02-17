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
import java.util.Map;

import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.WatchEventBuilder;
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
class KubernetesConfigMapTest {

    @KubernetesTestServer
    private KubernetesServer mockServer;

    @Test
    void configMapOperations() throws Exception {
        try (CamelKubernetesNamespace namespace = new CamelKubernetesNamespace()) {
            namespace.awaitCreation();

            Map<String, String> data = Map.of("foo", "bar");

            // Create
            RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body(data)
                    .when()
                    .post("/kubernetes/configmap/" + namespace.getNamespace() + "/camel-configmap")
                    .then()
                    .statusCode(201)
                    .body("metadata.name", is("camel-configmap"),
                            "metadata.namespace", is(namespace.getNamespace()),
                            "data.foo", is("bar"));

            // Read
            RestAssured.given()
                    .when()
                    .get("/kubernetes/configmap/" + namespace.getNamespace() + "/camel-configmap")
                    .then()
                    .statusCode(200)
                    .body("metadata.name", is("camel-configmap"),
                            "metadata.namespace", is(namespace.getNamespace()),
                            "data.foo", is("bar"));

            // Update
            Map<String, String> updatedData = Map.of("bin", "baz");
            RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body(updatedData)
                    .when()
                    .put("/kubernetes/configmap/" + namespace.getNamespace() + "/camel-configmap")
                    .then()
                    .statusCode(200)
                    .body("metadata.name", is("camel-configmap"),
                            "data.bin", is("baz"));

            Awaitility.await().pollInterval(Duration.ofMillis(250)).atMost(Duration.ofMinutes(1)).untilAsserted(() -> {
                RestAssured.given()
                        .when()
                        .get("/kubernetes/configmap/" + namespace.getNamespace() + "/camel-configmap")
                        .then()
                        .statusCode(200)
                        .body("metadata.name", is("camel-configmap"),
                                "metadata.namespace", is(namespace.getNamespace()),
                                "data.bin", is("baz"));
            });

            // List ConfigMaps
            RestAssured.given()
                    .when()
                    .get("/kubernetes/configmap/" + namespace.getNamespace())
                    .then()
                    .statusCode(200)
                    .body("[0].metadata.name", is("camel-configmap"),
                            "[0].metadata.namespace", is(namespace.getNamespace()),
                            "[0].data.bin", is("baz"));

            // List by labels
            RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body(Map.of("app", "camel-configmap"))
                    .when()
                    .get("/kubernetes/configmap/labels/" + namespace.getNamespace())
                    .then()
                    .statusCode(200)
                    .body("[0].metadata.name", is("camel-configmap"),
                            "[0].metadata.namespace", is(namespace.getNamespace()),
                            "[0].data.bin", is("baz"));

            // Delete
            RestAssured.given()
                    .when()
                    .delete("/kubernetes/configmap/" + namespace.getNamespace() + "/camel-configmap")
                    .then()
                    .statusCode(204);

            Awaitility.await().pollInterval(Duration.ofMillis(250)).atMost(Duration.ofMinutes(1)).untilAsserted(() -> {
                RestAssured.given()
                        .contentType(ContentType.JSON)
                        .body(Map.of("app", "camel-configmap"))
                        .when()
                        .get("/kubernetes/configmap/labels/" + namespace.getNamespace())
                        .then()
                        .statusCode(200)
                        .body("size()", is(0));
            });
        }
    }

    @Test
    void configMapEvents() throws Exception {
        try (CamelKubernetesNamespace namespace = new CamelKubernetesNamespace()) {
            namespace.awaitCreation();

            Map<String, String> data = Map.of("some", "data");

            if (mockServer != null) {
                String configMapEvent = Serialization.asJson(new WatchEventBuilder().withType("ADDED")
                        .withObject(new ConfigMapBuilder()
                                .withNewMetadata()
                                .withName("camel-configmap-watched")
                                .withNamespace(namespace.getNamespace())
                                .endMetadata()
                                .withData(data)
                                .build())
                        .build()) + "\n";

                String clientNamespace = mockServer.getClient().getNamespace();
                mockServer.expect()
                        .get()
                        .withPath("/api/v1/namespaces/" + clientNamespace
                                + "/configmaps?allowWatchBookmarks=true&fieldSelector=metadata.name%3Dcamel-configmap-watched&watch=true")
                        .andReturn(200, configMapEvent)
                        .always();
            }

            RestAssured.given()
                    .queryParam("namespace", namespace.getNamespace())
                    .when()
                    .post("/kubernetes/route/configmap-listener/start")
                    .then()
                    .statusCode(204);

            RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body(data)
                    .when()
                    .post("/kubernetes/configmap/" + namespace.getNamespace() + "/camel-configmap-watched")
                    .then()
                    .statusCode(201)
                    .body("metadata.name", is("camel-configmap-watched"),
                            "metadata.namespace", is(namespace.getNamespace()),
                            "data.some", is("data"));

            RestAssured.given()
                    .when()
                    .delete("/kubernetes/configmap/" + namespace.getNamespace() + "/camel-configmap-watched")
                    .then()
                    .statusCode(204);

            RestAssured.given()
                    .get("/kubernetes/configmap/events")
                    .then()
                    .statusCode(200)
                    .body("metadata.name", is("camel-configmap-watched"),
                            "metadata.namespace", is(namespace.getNamespace()),
                            "data.some", is("data"));
        } finally {
            RestAssured.given()
                    .when()
                    .post("/kubernetes/route/configmap-listener/stop")
                    .then()
                    .statusCode(204);
        }
    }
}
