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

import java.util.List;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that the protocol and cipher suite policy configured via {@code quarkus.tls.*} is enforced on
 * SSLContextParameters beans produced by the TLS registry bridge. Quarkus keeps that policy on the Vert.x SSLOptions
 * and never applies it to the SSLContext it builds, so the bridge has to carry it across itself.
 */
@TestCertificates(certificates = {
        @Certificate(name = "tls-test", formats = { Format.PKCS12, Format.PEM }, password = "changeit")
})
@QuarkusTest
@TestProfile(TlsRegistryTransportPolicyTest.TransportPolicyProfile.class)
class TlsRegistryTransportPolicyTest {

    private static final String TLS_12_CIPHER_SUITE = "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256";
    private static final String KEY_EXCHANGE_GROUP = "secp384r1";

    @Test
    void configuredProtocolsAndCipherSuitesAreEnforced() {
        Map<String, List<String>> policy = sslPolicy("restricted");

        assertEquals(List.of("TLSv1.2"), policy.get("protocols"),
                "Only the protocol configured via quarkus.tls.restricted.protocols should be enabled");
        assertEquals(List.of(TLS_12_CIPHER_SUITE), policy.get("cipherSuites"),
                "Only the cipher suite configured via quarkus.tls.restricted.cipher-suites should be enabled");
    }

    @Test
    void quarkusProtocolDefaultIsApplied() {
        Map<String, List<String>> policy = sslPolicy("defaultSslContextParameters");

        // quarkus.tls.protocols defaults to TLSv1.3, so Camel consumers of the bridged bean negotiate exactly what
        // the rest of the Quarkus application negotiates rather than the wider JVM default set
        assertEquals(List.of("TLSv1.3"), policy.get("protocols"));
    }

    @Test
    void configuredKeyExchangeGroupsAreCarriedOver() {
        assertEquals(List.of(KEY_EXCHANGE_GROUP), sslPolicy("restricted").get("namedGroups"),
                "quarkus.tls.restricted.key-exchange-groups should be carried over as Camel named groups");
    }

    /**
     * Camel's default cipher suite exclusions only apply while no explicit list is configured, since an explicit list
     * takes precedence over the filters. This covers that unset case; a configured list is applied verbatim.
     */
    @Test
    void camelDefaultCipherSuiteFiltersApplyWhenNoListIsConfigured() {
        List<String> cipherSuites = sslPolicy("defaultSslContextParameters").get("cipherSuites");

        assertFalse(cipherSuites.isEmpty());
        assertTrue(cipherSuites.stream().noneMatch(suite -> suite.contains("_NULL_")
                || suite.contains("_anon_")
                || suite.contains("_EXPORT_")
                || suite.contains("_DES_")
                || suite.endsWith("MD5")
                || suite.contains("RC4")),
                "Camel's default cipher suite exclusions should apply to bridged beans: " + cipherSuites);
    }

    private static Map<String, List<String>> sslPolicy(String beanName) {
        return RestAssured.given()
                .get("/tls-registry/ssl-policy/" + beanName)
                .then()
                .statusCode(200)
                .extract().as(new TypeRef<Map<String, List<String>>>() {
                });
    }

    public static class TransportPolicyProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.ofEntries(
                    Map.entry("quarkus.camel.tls-registry.enabled", "true"),
                    Map.entry("quarkus.camel.tls-registry.quarkus-default-as-global", "false"),
                    Map.entry("quarkus.tls.key-store.p12.path", "target/certs/tls-test-keystore.p12"),
                    Map.entry("quarkus.tls.key-store.p12.password", "changeit"),
                    Map.entry("quarkus.tls.restricted.key-store.p12.path", "target/certs/tls-test-keystore.p12"),
                    Map.entry("quarkus.tls.restricted.key-store.p12.password", "changeit"),
                    Map.entry("quarkus.tls.restricted.protocols", "TLSv1.2"),
                    Map.entry("quarkus.tls.restricted.cipher-suites", TLS_12_CIPHER_SUITE),
                    Map.entry("quarkus.tls.restricted.key-exchange-groups", KEY_EXCHANGE_GROUP));
        }
    }
}
