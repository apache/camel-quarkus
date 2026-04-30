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
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestCertificates(certificates = {
        @Certificate(name = "tls-test", formats = { Format.PKCS12, Format.PEM }, password = "changeit")
})
@QuarkusTest
@TestProfile(TlsRegistryMultipleConfigsTest.MultipleConfigsProfile.class)
class TlsRegistryMultipleConfigsTest {

    @Test
    void testMultipleNamedConfigs() {
        // Verify default bean exists
        RestAssured.given()
                .get("/tls-registry/bean/exists/defaultSslContextParameters")
                .then()
                .statusCode(200)
                .body(is("true"));

        // Verify client bean exists
        RestAssured.given()
                .get("/tls-registry/bean/exists/client")
                .then()
                .statusCode(200)
                .body(is("true"));

        // Verify server bean exists
        RestAssured.given()
                .get("/tls-registry/bean/exists/server")
                .then()
                .statusCode(200)
                .body(is("true"));

        // Verify no global SSL context
        RestAssured.given()
                .get("/tls-registry/global-ssl")
                .then()
                .statusCode(200)
                .body(is("false"));

        // Verify all beans are registered
        Map<String, String> beans = RestAssured.given()
                .get("/tls-registry/beans")
                .then()
                .statusCode(200)
                .extract().as(new TypeRef<Map<String, String>>() {
                });

        assertTrue(beans.containsKey("defaultSslContextParameters"), "Should contain 'defaultSslContextParameters' bean");
        assertTrue(beans.containsKey("client"), "Should contain 'client' bean");
        assertTrue(beans.containsKey("server"), "Should contain 'server' bean");
    }

    public static class MultipleConfigsProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.ofEntries(
                    Map.entry("quarkus.camel.tls-registry.enabled", "true"),
                    Map.entry("quarkus.camel.tls-registry.quarkus-default-as-global", "false"),
                    Map.entry("quarkus.tls.key-store.p12.path", "target/certs/tls-test-keystore.p12"),
                    Map.entry("quarkus.tls.key-store.p12.password", "changeit"),
                    Map.entry("quarkus.tls.client.key-store.p12.path", "target/certs/tls-test-keystore.p12"),
                    Map.entry("quarkus.tls.client.key-store.p12.password", "changeit"),
                    Map.entry("quarkus.tls.client.trust-all", "true"),
                    Map.entry("quarkus.tls.server.key-store.p12.path", "target/certs/tls-test-keystore.p12"),
                    Map.entry("quarkus.tls.server.key-store.p12.password", "changeit"),
                    Map.entry("quarkus.tls.server.trust-all", "true"));
        }
    }
}
