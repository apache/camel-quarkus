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
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.equalTo;

@TestCertificates(certificates = {
        @Certificate(name = "tls-global", formats = { Format.PKCS12, Format.PEM }, password = "changeit")
})
@QuarkusTest
@TestProfile(TlsRegistryHttpsGlobalTest.HttpsGlobalTestProfile.class)
class TlsRegistryHttpsGlobalTest {

    @Test
    void testHttpsCallWithGlobalSsl() {
        RestAssured.given()
                .get("/tls-registry/http/call-with-global-ssl")
                .then()
                .statusCode(200)
                .body(equalTo("pong"));
    }

    public static class HttpsGlobalTestProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "quarkus.camel.tls-registry.enabled", "true",
                    "quarkus.camel.tls-registry.quarkus-default-as-global", "true",
                    "quarkus.tls.key-store.p12.path", "target/certs/tls-global-keystore.p12",
                    "quarkus.tls.key-store.p12.password", "changeit",
                    "quarkus.tls.trust-store.p12.path", "target/certs/tls-global-keystore.p12",
                    "quarkus.tls.trust-store.p12.password", "changeit");
        }
    }
}
