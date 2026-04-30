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
package org.apache.camel.quarkus.core.tls.it;

import java.util.Map;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.restassured.RestAssured;
import io.restassured.common.mapper.TypeRef;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
@TestProfile(TlsRegistryDisabledTest.DisabledProfile.class)
class TlsRegistryDisabledTest {

    @Test
    void testRegistryDisabled() {
        // Verify no global SSL context
        RestAssured.given()
                .get("/tls-registry/global-ssl")
                .then()
                .statusCode(200)
                .body(is("false"));

        // Verify no beans are registered
        Map<String, String> beans = RestAssured.given()
                .get("/tls-registry/beans")
                .then()
                .statusCode(200)
                .extract().as(new TypeRef<Map<String, String>>() {
                });

        assertEquals(0, beans.size(), "Should have no SSL beans when disabled");
    }

    public static class DisabledProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("quarkus.camel.tls-registry.enabled", "false");
        }
    }
}
