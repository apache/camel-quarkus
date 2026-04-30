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

import java.io.File;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.restassured.RestAssured;
import io.smallrye.certs.CertificateGenerator;
import io.smallrye.certs.CertificateRequest;
import io.smallrye.certs.Format;
import io.smallrye.certs.junit5.Certificate;
import org.apache.camel.quarkus.test.support.certificate.TestCertificates;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static java.util.concurrent.TimeUnit.SECONDS;

import static org.hamcrest.Matchers.equalTo;

/**
 * Tests that certificate reload works when certificates are actually regenerated on the filesystem.
 * This test simulates real-world certificate rotation by regenerating certs and letting Quarkus
 * TLS reload mechanism detect the changes.
 */
@TestCertificates(certificates = {
        @Certificate(name = "tls-fs-reload", formats = { Format.PKCS12 }, password = "changeit")
})
@QuarkusTest
@TestProfile(TlsRegistryFilesystemCertificateReloadTest.FilesystemReloadTestProfile.class)
class TlsRegistryFilesystemCertificateReloadTest {

    private static final String CERT_BASE_DIR = "target/certs";
    private static final String CERT_NAME = "tls-fs-reload";

    @BeforeEach
    void resetCounter() {
        RestAssured.given()
                .post("/tls-registry/reload/reset")
                .then()
                .statusCode(200);
    }

    @Test
    void testHttpsConnectionWorksAfterFilesystemCertificateReload() throws Exception {
        // 1. Verify initial HTTPS connectivity works
        RestAssured.given()
                .get("/tls-registry/reload/test-https")
                .then()
                .statusCode(200)
                .body(equalTo("pong"));

        // 2. Regenerate certificates on filesystem (simulating cert rotation)
        // Add small delay to ensure file timestamp will be detectably different
        Thread.sleep(100);

        File baseDir = new File(CERT_BASE_DIR);
        CertificateGenerator generator = new CertificateGenerator(baseDir.toPath(), true);
        CertificateRequest request = new CertificateRequest()
                .withName(CERT_NAME)
                .withFormats(List.of(Format.PKCS12))
                .withPassword("changeit")
                .withDuration(Duration.ofDays(1));

        // Force regeneration by specifying replaceIfExists=true
        generator.generate(request);

        // Touch the keystore file to force a modification time update that Quarkus will detect
        File keystoreFile = new File(CERT_BASE_DIR, CERT_NAME + "-keystore.p12");
        keystoreFile.setLastModified(System.currentTimeMillis());

        // 3. Wait for Quarkus TLS to detect the change and trigger Camel context reload
        // Quarkus reload-period is 1s, plus debounce delay of 2s
        // Allow extra time for filesystem polling and debouncing - wait up to 15s
        Awaitility.await()
                .atMost(15, SECONDS)
                .pollInterval(200, java.util.concurrent.TimeUnit.MILLISECONDS)
                .untilAsserted(() -> RestAssured.given()
                        .get("/tls-registry/reload/count")
                        .then()
                        .statusCode(200)
                        .body(equalTo("1")));

        // 4. Verify HTTPS still works after real certificate reload
        RestAssured.given()
                .get("/tls-registry/reload/test-https")
                .then()
                .statusCode(200)
                .body(equalTo("pong"));
    }

    public static class FilesystemReloadTestProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            String keystorePath = CERT_BASE_DIR + "/" + CERT_NAME + "-keystore.p12";

            return Map.ofEntries(
                    Map.entry("quarkus.camel.tls-registry.enabled", "true"),
                    Map.entry("quarkus.camel.tls-registry.reload-on-certificate-update", "true"),
                    Map.entry("quarkus.tls.reload-period", "5s"),
                    Map.entry("quarkus.tls.key-store.p12.path", keystorePath),
                    Map.entry("quarkus.tls.key-store.p12.password", "changeit"),
                    Map.entry("quarkus.tls.trust-store.p12.path", keystorePath),
                    Map.entry("quarkus.tls.trust-store.p12.password", "changeit"));
        }
    }
}
