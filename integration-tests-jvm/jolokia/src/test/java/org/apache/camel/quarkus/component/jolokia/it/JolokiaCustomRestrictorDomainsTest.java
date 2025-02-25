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

@TestProfile(JolokiaCustomRestrictorDomainsTest.JolokiaAdditionalPropertiesProfile.class)
@QuarkusTest
class JolokiaCustomRestrictorDomainsTest {
    @BeforeEach
    public void beforeEach() {
        RestAssured.port = 8778;
    }

    @Test
    void customMBeanAllowDomains() {
        // Verify org.apache.camel domain allowed
        RestAssured.given()
                .get("/jolokia/read/org.apache.camel:context=camel-1,type=context,name=\"camel-1\"/CamelId")
                .then()
                .statusCode(200)
                .body(
                        "status", equalTo(200),
                        "value", equalTo("camel-1"));

        // Verify java.lang domain disallowed
        RestAssured.given()
                .get("/jolokia/read/java.lang:type=ClassLoading/LoadedClassCount")
                .then()
                .statusCode(200)
                .body(
                        "status", equalTo(403));

        // Verify java.nio domain disallowed
        RestAssured.given()
                .get("/jolokia/read/java.nio:type=BufferPool,name=direct/MemoryUsed")
                .then()
                .statusCode(200)
                .body(
                        "status", equalTo(403));
    }

    public static final class JolokiaAdditionalPropertiesProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("quarkus.camel.jolokia.camel-restrictor-allowed-mbean-domains", "org.apache.camel");
        }
    }
}
