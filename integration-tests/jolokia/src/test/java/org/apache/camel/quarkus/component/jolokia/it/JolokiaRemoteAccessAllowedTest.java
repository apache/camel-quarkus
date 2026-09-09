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

import java.util.Map;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.equalTo;

@TestProfile(JolokiaRemoteAccessAllowedTest.RemoteAccessAllowedProfile.class)
@QuarkusTest
class JolokiaRemoteAccessAllowedTest {
    @BeforeEach
    public void beforeEach() {
        RestAssured.port = 8778;
    }

    @Test
    void jolokiaAccessibleWithRemoteAccessAllowed() {
        RestAssured.given()
                .get("/jolokia/")
                .then()
                .statusCode(200)
                .body("status", equalTo(200));
    }

    /**
     * Remote access is a control over client addresses only, so an unlisted origin is still refused.
     */
    @Test
    void unlistedOriginDenied() {
        RestAssured.given()
                .header("Origin", "http://domain.example.com")
                .get("/jolokia/")
                .then()
                .statusCode(200)
                .body("status", equalTo(403));
    }

    @Test
    void listedOriginAllowed() {
        RestAssured.given()
                .header("Origin", "http://allowed.example.com")
                .get("/jolokia/")
                .then()
                .statusCode(200)
                .body("status", equalTo(200));
    }

    /**
     * Jolokia falls back to Referer when there is no Origin, and a Referer carries a path.
     */
    @Test
    void listedOriginAllowedFromReferer() {
        RestAssured.given()
                .header("Referer", "http://allowed.example.com/console/index.html")
                .get("/jolokia/")
                .then()
                .statusCode(200)
                .body("status", equalTo(200));
    }

    /**
     * The origin is listed, so the restrictor permits it. Jolokia refuses it anyway, because answering a secure
     * origin over a plain HTTP connection would downgrade it.
     */
    @Test
    void httpsOriginRejectedByJolokiaOverPlainHttp() {
        RestAssured.given()
                .header("Origin", "https://secure.example.com")
                .get("/jolokia/")
                .then()
                .statusCode(200)
                .body("status", equalTo(403));
    }

    @Test
    void mbeanAccessStillRestrictedByDomain() {
        RestAssured.given()
                .get("/jolokia/read/java.util.logging:type=Logging/LoggerNames")
                .then()
                .statusCode(200)
                .body("status", equalTo(403));
    }

    public static final class RemoteAccessAllowedProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "quarkus.camel.jolokia.remote-access-allowed", "true",
                    "quarkus.camel.jolokia.allowed-origins",
                    "http://allowed.example.com,https://secure.example.com");
        }
    }
}
