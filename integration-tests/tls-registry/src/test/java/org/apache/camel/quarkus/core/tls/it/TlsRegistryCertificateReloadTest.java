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
import io.smallrye.certs.Format;
import io.smallrye.certs.junit5.Certificate;
import org.apache.camel.quarkus.test.support.certificate.TestCertificates;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static java.util.concurrent.TimeUnit.SECONDS;

import static org.hamcrest.Matchers.equalTo;

@TestCertificates(certificates = {
        @Certificate(name = "tls-reload", formats = { Format.PKCS12, Format.PEM }, password = "changeit")
})
@QuarkusTest
@TestProfile(TlsRegistryCertificateReloadTest.CertReloadTestProfile.class)
class TlsRegistryCertificateReloadTest {

    @BeforeEach
    void resetCounter() {
        RestAssured.given()
                .post("/tls-registry/reload/reset")
                .then()
                .statusCode(200);
    }

    @Test
    void testSingleCertificateUpdateTriggersReload() {
        // Fire a certificate updated event for the default cert (which exists in TLS registry)
        RestAssured.given()
                .post("/tls-registry/reload/fire-event/default")
                .then()
                .statusCode(200);

        // Wait for debounced reload (2 seconds + buffer) and verify exactly one reload occurred
        Awaitility.await()
                .atMost(4, SECONDS)
                .pollInterval(100, java.util.concurrent.TimeUnit.MILLISECONDS)
                .untilAsserted(() -> RestAssured.given()
                        .get("/tls-registry/reload/count")
                        .then()
                        .statusCode(200)
                        .body(equalTo("1")));
    }

    @Test
    void testMultipleCertificateUpdatesDebouncedToSingleReload() {
        // Fire multiple certificate updated events in quick succession for the default cert
        for (int i = 1; i <= 5; i++) {
            RestAssured.given()
                    .post("/tls-registry/reload/fire-event/default")
                    .then()
                    .statusCode(200);

            // Space out events by 100ms
            if (i < 5) {
                Awaitility.await().pollDelay(100, java.util.concurrent.TimeUnit.MILLISECONDS).until(() -> true);
            }
        }

        // Wait for debounced reload (2 seconds + buffer from last event)
        // Should have exactly one reload despite 5 certificate updates
        Awaitility.await()
                .atMost(4, SECONDS)
                .pollInterval(100, java.util.concurrent.TimeUnit.MILLISECONDS)
                .untilAsserted(() -> RestAssured.given()
                        .get("/tls-registry/reload/count")
                        .then()
                        .statusCode(200)
                        .body(equalTo("1")));

        // Verify it's still 1 after a bit more time (no additional reloads)
        Awaitility.await().pollDelay(500, java.util.concurrent.TimeUnit.MILLISECONDS).until(() -> true);
        RestAssured.given()
                .get("/tls-registry/reload/count")
                .then()
                .statusCode(200)
                .body(equalTo("1"));
    }

    public static class CertReloadTestProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.ofEntries(
                    Map.entry("quarkus.camel.tls-registry.enabled", "true"),
                    Map.entry("quarkus.camel.tls-registry.reload-on-certificate-update", "true"),
                    Map.entry("quarkus.tls.key-store.p12.path", "target/certs/tls-reload-keystore.p12"),
                    Map.entry("quarkus.tls.key-store.p12.password", "changeit"));
        }
    }
}
