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

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.awaitility.Awaitility;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.is;

@TestProfile(KubernetesConfigMapContextReloadTest.KubernetesConfigMapContextReloadTestProfile.class)
@QuarkusTest
class KubernetesConfigMapContextReloadTest {
    @Test
    void configMapTriggersCamelContextReload() throws Exception {
        Map<String, String> data = Map.of("foo", "bar");

        String name = ConfigProvider.getConfig().getValue("camel.vault.kubernetescm.configmaps", String.class);
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
                    .body(data)
                    .when()
                    .post("/kubernetes/configmap/" + namespace + "/" + name)
                    .then()
                    .statusCode(201)
                    .body("metadata.name", is(name),
                            "metadata.namespace", is(namespace),
                            "data.foo", is("bar"));

            // Need to delay before updating the ConfigMap so that the reload trigger task can capture the event
            Awaitility.await().pollDelay(Duration.ofSeconds(1)).pollInterval(Duration.ofMillis(250))
                    .atMost(Duration.ofMinutes(1)).untilAsserted(() -> {
                        RestAssured.given()
                                .when()
                                .get("/kubernetes/configmap/" + namespace + "/" + name)
                                .then()
                                .statusCode(200)
                                .body("metadata.name", is(name));
                    });

            // Update to trigger context reload
            Map<String, String> updatedData = Map.of("bin", "baz");
            RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body(updatedData)
                    .when()
                    .put("/kubernetes/configmap/" + namespace + "/" + name)
                    .then()
                    .statusCode(200)
                    .body("metadata.name", is(name),
                            "data.bin", is("baz"));

            Awaitility.await().pollInterval(Duration.ofMillis(250)).atMost(Duration.ofMinutes(1)).untilAsserted(() -> {
                RestAssured.get("/kubernetes/configmap/context/reload/state")
                        .then()
                        .statusCode(200)
                        .body(is("reloaded"));
            });
        } finally {
            // Clean up
            RestAssured.given()
                    .when()
                    .delete("/kubernetes/configmap/" + namespace + "/" + name)
                    .then()
                    .statusCode(204);
        }
    }

    public static final class KubernetesConfigMapContextReloadTestProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "camel.vault.kubernetescm.refreshEnabled", "true",
                    "camel.vault.kubernetescm.configmaps", "configmap-reload-config");
        }
    }
}
