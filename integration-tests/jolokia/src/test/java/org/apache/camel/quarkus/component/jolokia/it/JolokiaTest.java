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
package org.apache.camel.quarkus.component.jolokia.it;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import org.apache.camel.quarkus.jolokia.restrictor.CamelJolokiaRestrictor;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.matchesPattern;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class JolokiaTest {
    @BeforeEach
    public void beforeEach() {
        RestAssured.port = 8778;
    }

    @ParameterizedTest
    @ValueSource(strings = { "/jolokia/", "/q/jolokia" })
    void defaultConfiguration(String path) {
        if (path.startsWith("/q")) {
            RestAssured.port = ConfigProvider.getConfig().getValue("quarkus.http.test-port", Integer.class);
        }

        RestAssured.given()
                .get(path)
                .then()
                .statusCode(200)
                .body(
                        "status", equalTo(200),
                        "value.config.discoveryEnabled", equalTo("true"),
                        "value.config.restrictorClass", equalTo(CamelJolokiaRestrictor.class.getName()),
                        "value.config.agentDescription", equalTo("camel-quarkus-integration-test-jolokia"),
                        "value.details.url", matchesPattern("http://.*:8778/jolokia/"));
    }

    @Test
    void sendMessage() {
        String jolokiaPayload = "{\"type\":\"exec\",\"mbean\":\"org.apache.camel:context=camel-1,type=context,name=\\\"camel-1\\\"\",\"operation\":\"sendStringBody(java.lang.String, java.lang.String)\",\"arguments\":[\"direct://start\",\"Hello World\"]}";
        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(jolokiaPayload)
                .post("/jolokia/")
                .then()
                .statusCode(200)
                .body("status", equalTo(200));

        RestAssured.port = ConfigProvider.getConfig().getValue("quarkus.http.test-port", Integer.class);

        RestAssured.get("/jolokia/message/get")
                .then()
                .statusCode(200)
                .body(equalTo("Hello World"));
    }

    @Test
    void additionalAllowedDefaultMBeanDomains() {
        // Verify java.lang domain
        RestAssured.given()
                .get("/jolokia/read/java.lang:type=ClassLoading/LoadedClassCount")
                .then()
                .statusCode(200)
                .body(
                        "status", equalTo(200),
                        "value", greaterThanOrEqualTo(0));

        // Verify java.nio domain
        JsonPath response = RestAssured.given()
                .get("/jolokia/read/java.nio:type=BufferPool,name=direct/MemoryUsed")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .jsonPath();

        // java.nio MBeans are not available in native mode so the Jolokia status must be checked
        int status = response.getInt("status");
        if (status == 200) {
            assertTrue(response.getInt("value") >= 0);
        }

        // Disallowed domain
        RestAssured.given()
                .get("/jolokia/read/java.util.logging:type=Logging/LoggerNames")
                .then()
                .statusCode(200)
                .body(
                        "status", equalTo(403));
    }
}
