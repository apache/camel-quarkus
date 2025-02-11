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
import java.util.UUID;

import io.restassured.RestAssured;
import org.awaitility.Awaitility;

import static org.hamcrest.Matchers.is;

public class CamelKubernetesNamespace implements AutoCloseable {
    private final String namespace = UUID.randomUUID().toString();

    public CamelKubernetesNamespace() {
        RestAssured.given()
                .post("/kubernetes/namespace/" + namespace)
                .then()
                .statusCode(201);
    }

    public void awaitCreation() {
        Awaitility.await().pollInterval(Duration.ofMillis(250)).atMost(Duration.ofMinutes(1)).untilAsserted(() -> {
            RestAssured.given()
                    .get("/kubernetes/namespace/" + namespace)
                    .then()
                    .statusCode(200);
        });
    }

    public String getNamespace() {
        return namespace;
    }

    @Override
    public void close() throws Exception {
        RestAssured.given()
                .delete("/kubernetes/namespace/" + namespace)
                .then()
                .statusCode(200)
                .body(is("true"));
    }

    @Override
    public String toString() {
        return namespace;
    }
}
