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
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.awaitility.Awaitility;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.is;

@Disabled("https://github.com/apache/camel-quarkus/issues/7042")
@TestProfile(KubernetesSecretContextReloadTest.KubernetesSecretContextReloadTestProfile.class)
@QuarkusTest
class KubernetesSecretContextReloadTest {
    @Test
    void secretTriggersCamelContextReload() throws Exception {
        Map<String, String> data = Map.of("project-name", "Camel");

        String name = ConfigProvider.getConfig().getValue("camel.vault.kubernetes.secrets", String.class);
        Secret secret = new SecretBuilder()
                .withNewMetadata()
                .withLabels(Map.of("app", name))
                .withName(name)
                .endMetadata()
                .withData(data)
                .build();

        String namespace = RestAssured.get("/kubernetes/default/namespace")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .asString();

        try {
            // Create
            RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body(secret)
                    .when()
                    .post("/kubernetes/secret/" + namespace)
                    .then()
                    .statusCode(201)
                    .body("metadata.name", is(name),
                            "metadata.namespace", is(namespace),
                            "metadata.annotations.app", is(name),
                            "data.project-name", is(data.get("project-name")));

            // Need to delay before updating the secret so that the reload trigger task can capture the event
            Thread.sleep(500);

            // Update to trigger context reload
            Map<String, String> newData = Map.of("project-name", "Apache Camel Quarkus");
            secret.setData(newData);
            RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body(secret)
                    .when()
                    .put("/kubernetes/secret/" + namespace)
                    .then()
                    .statusCode(200)
                    .body("metadata.name", is(name),
                            "data.project-name", is(newData.get("project-name")));

            Awaitility.await().pollInterval(Duration.ofMillis(250)).atMost(Duration.ofMinutes(1)).untilAsserted(() -> {
                RestAssured.get("/kubernetes/secret/context/reload/state")
                        .then()
                        .statusCode(200)
                        .body(is("reloaded"));
            });

        } finally {
            // Clean up
            RestAssured.given()
                    .when()
                    .delete("/kubernetes/secret/" + namespace + "/" + name)
                    .then()
                    .statusCode(204);
        }
    }

    public static final class KubernetesSecretContextReloadTestProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "camel.vault.kubernetes.refreshEnabled", "true",
                    "camel.vault.kubernetes.secrets", "secret-reload-config");
        }
    }
}
