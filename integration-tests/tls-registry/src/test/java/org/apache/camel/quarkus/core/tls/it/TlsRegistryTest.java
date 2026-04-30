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
import io.smallrye.certs.Format;
import io.smallrye.certs.junit5.Certificate;
import org.apache.camel.quarkus.test.support.certificate.TestCertificates;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestCertificates(certificates = {
        @Certificate(name = "tls-test", formats = { Format.PKCS12, Format.PEM }, password = "changeit")
})
@QuarkusTest
@TestProfile(TlsRegistryTest.DefaultNamedConfigProfile.class)
class TlsRegistryTest {

    @Test
    void testDefaultNamedBean() {
        // Verify default TLS config is registered as named bean "default"
        RestAssured.given()
                .get("/tls-registry/bean/exists/default")
                .then()
                .statusCode(200)
                .body(is("true"));

        // Verify no global SSL context
        RestAssured.given()
                .get("/tls-registry/global-ssl")
                .then()
                .statusCode(200)
                .body(is("false"));

        // Verify beans are registered
        Map<String, String> beans = RestAssured.given()
                .get("/tls-registry/beans")
                .then()
                .statusCode(200)
                .extract().as(new TypeRef<Map<String, String>>() {
                });

        assertTrue(beans.containsKey("default"), "Should contain 'default' bean");
        assertEquals(1, beans.size(), "Should have exactly 1 SSL bean");
    }

    public static class DefaultNamedConfigProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "quarkus.camel.tls-registry.enabled", "true",
                    "quarkus.camel.tls-registry.quarkus-default-as-global", "false",
                    "quarkus.camel.tls-registry.default-bean-name", "default",
                    "quarkus.tls.key-store.p12.path", "target/certs/tls-test-keystore.p12",
                    "quarkus.tls.key-store.p12.password", "changeit");
        }
    }
}
