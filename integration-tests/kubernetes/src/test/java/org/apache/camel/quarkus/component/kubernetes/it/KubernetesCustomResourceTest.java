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
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinition;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinitionBuilder;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinitionVersionBuilder;
import io.fabric8.kubernetes.api.model.apiextensions.v1.JSONSchemaPropsBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.KubernetesTestServer;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.is;

@QuarkusTest
@QuarkusTestResource(CamelQuarkusKubernetesServerTestResource.class)
class KubernetesCustomResourceTest {
    @KubernetesTestServer
    private KubernetesServer mockServer;

    private final CustomResourceDefinition crd = new CustomResourceDefinitionBuilder()
            .withApiVersion("apiextensions.k8s.io/v1")
            .withKind("CustomResourceDefinition")
            .withNewMetadata()
            .withName(KubernetesCRResource.CRD_PLURAL + "." + KubernetesCRResource.CRD_GROUP)
            .endMetadata()
            .withNewSpec()
            .withGroup(KubernetesCRResource.CRD_GROUP)
            .withVersions(new CustomResourceDefinitionVersionBuilder()
                    .withName(KubernetesCRResource.CRD_VERSION)
                    .withServed(true)
                    .withStorage(true)
                    .withNewSchema()
                    .withNewOpenAPIV3Schema()
                    .withType("object")
                    .withProperties(Map.of("spec", new JSONSchemaPropsBuilder().withType("object")
                            .withProperties(Map.of("foo", new JSONSchemaPropsBuilder().withType("string").build())).build()))
                    .endOpenAPIV3Schema()
                    .endSchema()
                    .build())
            .withScope(KubernetesCRResource.CRD_SCOPE)
            .withNewNames()
            .withSingular(KubernetesCRResource.CRD_NAME)
            .withPlural(KubernetesCRResource.CRD_PLURAL)
            .withKind(KubernetesCRResource.CRD_NAME)
            .endNames()
            .endSpec()
            .build();

    @BeforeEach
    public void loadCRD() {
        if (mockServer == null) {
            try (KubernetesClient client = new KubernetesClientBuilder().build()) {
                client.apiextensions().v1().customResourceDefinitions().resource(crd).serverSideApply();
            }
        }
    }

    @AfterEach
    public void removeCRD() {
        if (mockServer == null) {
            try (KubernetesClient client = new KubernetesClientBuilder().build()) {
                client.apiextensions().v1().customResourceDefinitions().resource(crd).delete();

            }
        }
    }

    @Test
    void customResourceOperations() throws Exception {

        try (CamelKubernetesNamespace namespace = new CamelKubernetesNamespace()) {
            namespace.awaitCreation();

            ObjectMapper mapper = new ObjectMapper();
            String name = "camel-cr";
            Map<String, Object> instance = Map.of(
                    "apiVersion", KubernetesCRResource.CRD_GROUP + "/" + KubernetesCRResource.CRD_VERSION,
                    "kind", KubernetesCRResource.CRD_NAME,
                    "metadata", Map.of(
                            "name", name,
                            "labels", Map.of(
                                    "app", name)),
                    "spec", new HashMap<>(Map.of(
                            "foo", "bar")));
            String json = mapper.writeValueAsString(instance);
            // Create
            RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body(json)
                    .when()
                    .post("/kubernetes/customresource/" + namespace.getNamespace())
                    .then()
                    .statusCode(200)
                    .body("metadata.name", is(name),
                            "metadata.namespace", is(namespace.getNamespace()),
                            "spec.foo", is(((Map) instance.get("spec")).get("foo")));

            // Read
            RestAssured.given()
                    .when()
                    .get("/kubernetes/customresource/" + namespace.getNamespace() + "/" + name)
                    .then()
                    .statusCode(200)
                    .body("spec.foo", is(((Map) instance.get("spec")).get("foo")));

            // Update
            ((Map<String, Object>) instance.get("spec")).put("foo", "baz");
            json = mapper.writeValueAsString(instance);
            RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body(json)
                    .when()
                    .put("/kubernetes/customresource/" + namespace.getNamespace() + "/" + name)
                    .then()
                    .statusCode(200)
                    .body("metadata.name", is(name),
                            "metadata.namespace", is(namespace.getNamespace()),
                            "spec.foo", is(((Map) instance.get("spec")).get("foo")));

            Awaitility.await().pollInterval(Duration.ofMillis(250)).atMost(Duration.ofMinutes(1))
                    .untilAsserted(() -> RestAssured.given()
                            .when()
                            .get("/kubernetes/customresource/" + namespace.getNamespace() + "/" + name)
                            .then()
                            .statusCode(200)
                            .body("spec.foo", is(((Map) instance.get("spec")).get("foo"))));

            // List
            RestAssured.given()
                    .when()
                    .get("/kubernetes/customresource/" + namespace.getNamespace())
                    .then()
                    .statusCode(200)
                    .body("[0].metadata.name", is(name),
                            "[0].metadata.namespace", is(namespace.getNamespace()),
                            "[0].spec.foo", is(((Map) instance.get("spec")).get("foo")));

            // List by labels
            RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body(Map.of("app", name))
                    .when()
                    .get("/kubernetes/customresource/labels/" + namespace.getNamespace())
                    .then()
                    .statusCode(200)
                    .body("[0].metadata.name", is(name),
                            "[0].metadata.namespace", is(namespace.getNamespace()),
                            "[0].spec.foo", is(((Map) instance.get("spec")).get("foo")));

            // Delete
            RestAssured.given()
                    .when()
                    .delete("/kubernetes/customresource/" + namespace.getNamespace() + "/" + name)
                    .then()
                    .statusCode(204);

            Awaitility.await().pollInterval(Duration.ofMillis(250)).atMost(Duration.ofMinutes(1)).untilAsserted(() -> {
                RestAssured.given()
                        .contentType(ContentType.JSON)
                        .body(Map.of("app", name))
                        .when()
                        .get("/kubernetes/customresource/labels/" + namespace.getNamespace())
                        .then()
                        .statusCode(200)
                        .body("size()", is(0));
            });
        }
    }
}
