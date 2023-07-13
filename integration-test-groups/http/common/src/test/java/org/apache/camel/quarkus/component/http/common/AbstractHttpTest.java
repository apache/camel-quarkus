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
package org.apache.camel.quarkus.component.http.common;

import io.restassured.RestAssured;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Test;

import static org.apache.camel.quarkus.component.http.common.AbstractHttpResource.USER_ADMIN;
import static org.apache.camel.quarkus.component.http.common.AbstractHttpResource.USER_ADMIN_PASSWORD;
import static org.apache.camel.quarkus.component.http.common.AbstractHttpResource.USER_NO_ADMIN;
import static org.apache.camel.quarkus.component.http.common.AbstractHttpResource.USER_NO_ADMIN_PASSWORD;
import static org.hamcrest.Matchers.is;

public abstract class AbstractHttpTest {
    public abstract String component();

    public abstract void compression();

    @Test
    public void basicProducerGet() {
        RestAssured
                .given()
                .queryParam("test-port", RestAssured.port)
                .when()
                .get("/test/client/{component}/get", component())
                .then()
                .statusCode(200)
                .body(is("get"));
    }

    @Test
    public void basicProducerPost() {
        RestAssured
                .given()
                .queryParam("test-port", RestAssured.port)
                .body("message")
                .when()
                .post("/test/client/{component}/post", component())
                .then()
                .statusCode(200)
                .body(is("MESSAGE"));
    }

    @Test
    public void httpsProducer() {
        RestAssured
                .given()
                .queryParam("component", component())
                .when()
                .get("/test/client/{component}/get-https", component())
                .then()
                .statusCode(200)
                .body(is("HTTPS GET"));
    }

    @Test
    public void basicAuth() {
        // No credentials expect 401 response
        RestAssured
                .given()
                .queryParam("test-port", RestAssured.port)
                .queryParam("component", component())
                .when()
                .get("/test/client/{component}/auth/basic", component())
                .then()
                .statusCode(401);

        // Invalid credentials expect 403 response
        RestAssured
                .given()
                .queryParam("test-port", RestAssured.port)
                .queryParam("component", component())
                .queryParam("username", USER_NO_ADMIN)
                .queryParam("password", USER_NO_ADMIN_PASSWORD)
                .when()
                .get("/test/client/{component}/auth/basic", component())
                .then()
                .statusCode(403);

        // Valid credentials expect 200 response
        RestAssured
                .given()
                .queryParam("test-port", RestAssured.port)
                .queryParam("component", component())
                .queryParam("username", USER_ADMIN)
                .queryParam("password", USER_ADMIN_PASSWORD)
                .when()
                .get("/test/client/{component}/auth/basic", component())
                .then()
                .statusCode(200)
                .body(is("Component " + component() + " is using basic auth"));
    }

    @Test
    public void proxyServer() {
        RestAssured
                .given()
                .when()
                .get("/test/client/{component}/proxy", component())
                .then()
                .statusCode(200)
                .body(
                        "metadata.groupId", is("org.apache.camel.quarkus"),
                        "metadata.artifactId", is("camel-quarkus-" + component()));
    }

    protected Integer getPort(String configKey) {
        return ConfigProvider.getConfig().getValue(configKey, Integer.class);
    }
}
