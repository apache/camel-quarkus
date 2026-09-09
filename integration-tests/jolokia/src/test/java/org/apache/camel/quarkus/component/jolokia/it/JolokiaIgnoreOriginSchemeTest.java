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

/**
 * Jolokia refuses an https Origin over a plain http connection before any origin configuration is consulted.
 * This is the property that turns that off, in place of the ignore-scheme element of an access policy.
 */
@TestProfile(JolokiaIgnoreOriginSchemeTest.IgnoreOriginSchemeProfile.class)
@QuarkusTest
class JolokiaIgnoreOriginSchemeTest {
    @BeforeEach
    public void beforeEach() {
        RestAssured.port = 8778;
    }

    @Test
    void httpsOriginAcceptedOverPlainHttp() {
        RestAssured.given()
                .header("Origin", "https://secure.example.com")
                .get("/jolokia/")
                .then()
                .statusCode(200)
                .body("status", equalTo(200));
    }

    @Test
    void unlistedOriginStillDenied() {
        RestAssured.given()
                .header("Origin", "https://untrusted.example")
                .get("/jolokia/")
                .then()
                .statusCode(200)
                .body("status", equalTo(403));
    }

    public static final class IgnoreOriginSchemeProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "quarkus.camel.jolokia.ignore-origin-scheme", "true",
                    "quarkus.camel.jolokia.allowed-origins", "https://secure.example.com");
        }
    }
}
