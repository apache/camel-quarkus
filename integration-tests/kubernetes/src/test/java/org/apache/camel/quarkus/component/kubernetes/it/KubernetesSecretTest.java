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

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
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
class KubernetesSecretTest {

    @KubernetesTestServer
    private KubernetesServer mockServer;

    @Test
    void secretOperations() throws Exception {
        try (CamelKubernetesNamespace namespace = new CamelKubernetesNamespace()) {
            namespace.awaitCreation();

            String name = "camel-secret";
            // base64 of "secretValue" - the mockServer does not work process .withStringData into base64
            Map<String, String> data = Map.of("secretKey", "c2VjcmV0VmFsdWUK");

            Secret secret = new SecretBuilder()
                    .withNewMetadata()
                    .withLabels(Map.of("app", name))
                    .withName(name)
                    .endMetadata().withData(data)
                    .build();

            // Create
            RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body(secret)
                    .when()
                    .post("/kubernetes/secret/" + namespace.getNamespace())
                    .then()
                    .statusCode(201)
                    .body("metadata.name", is(name),
                            "metadata.namespace", is(namespace.getNamespace()),
                            "metadata.annotations.app", is(name),
                            "data.secretKey", is(data.get("secretKey")));

            // Read
            RestAssured.given()
                    .when()
                    .get("/kubernetes/secret/" + namespace.getNamespace() + "/" + name)
                    .then()
                    .statusCode(200)
                    .body("metadata.name", is(name),
                            "metadata.namespace", is(namespace.getNamespace()),
                            "data.secretKey", is(data.get("secretKey")));

            // Update
            Map<String, String> newData = Map.of("newSecretKey", "bmV3U2VjcmV0VmFsdWUK");
            secret.setData(newData);
            RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body(secret)
                    .when()
                    .put("/kubernetes/secret/" + namespace.getNamespace())
                    .then()
                    .statusCode(200)
                    .body("metadata.name", is(name),
                            "data.newSecretKey", is(newData.get("newSecretKey")));

            Awaitility.await().pollInterval(Duration.ofMillis(250)).atMost(Duration.ofMinutes(1))
                    .untilAsserted(() -> RestAssured.given()
                            .when()
                            .get("/kubernetes/secret/" + namespace.getNamespace() + "/" + name)
                            .then()
                            .statusCode(200)
                            .body("metadata.name", is(name),
                                    "metadata.namespace", is(namespace.getNamespace()),
                                    "data.newSecretKey", is(newData.get("newSecretKey"))));

            // List
            RestAssured.given()
                    .when()
                    .get("/kubernetes/secret/" + namespace.getNamespace())
                    .then()
                    .statusCode(200)
                    .body("[0].metadata.name", is(name),
                            "[0].metadata.namespace", is(namespace.getNamespace()),
                            "[0].data.newSecretKey", is(newData.get("newSecretKey")));

            // List by labels
            RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body(Map.of("app", name))
                    .when()
                    .get("/kubernetes/secret/labels/" + namespace.getNamespace())
                    .then()
                    .statusCode(200)
                    .body("[0].metadata.name", is(name),
                            "[0].metadata.namespace", is(namespace.getNamespace()),
                            "[0].data.newSecretKey", is(newData.get("newSecretKey")));

            // Delete
            RestAssured.given()
                    .when()
                    .delete("/kubernetes/secret/" + namespace.getNamespace() + "/" + name)
                    .then()
                    .statusCode(204);

            Awaitility.await().pollInterval(Duration.ofMillis(250)).atMost(Duration.ofMinutes(1))
                    .untilAsserted(() -> RestAssured.given()
                            .contentType(ContentType.JSON)
                            .body(Map.of("app", name))
                            .when()
                            .get("/kubernetes/secret/labels/" + namespace.getNamespace())
                            .then()
                            .statusCode(200)
                            .body("size()", is(0)));
        }
    }
}
