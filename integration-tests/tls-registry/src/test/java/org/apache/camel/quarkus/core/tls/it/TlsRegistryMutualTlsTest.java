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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

/**
 * The {@link MutualTlsServer} requires a client certificate, so a call only succeeds when the Camel component presents
 * the key from the bridged SSLContextParameters bean.
 */
@TestCertificates(certificates = {
        @Certificate(name = "tls-mtls", formats = { Format.PKCS12, Format.PEM }, password = "changeit", client = true)
})
@QuarkusTest
@TestProfile(TlsRegistryMutualTlsTest.MutualTlsProfile.class)
class TlsRegistryMutualTlsTest {

    /**
     * camel-http uses SSLContextParameters.createSSLContext.
     */
    @ParameterizedTest
    @ValueSource(strings = { "client-p12", "client-pem", "client-trust-all" })
    void httpPresentsClientCertificate(String sslBeanName) {
        RestAssured.given()
                .get("/tls-registry/mtls/http/" + sslBeanName)
                .then()
                .statusCode(200)
                .body(equalTo("pong"));
    }

    /**
     * camel-vertx-http builds its own KeyManagerFactory from SSLContextParameters.getKeyManagers.
     */
    @ParameterizedTest
    @ValueSource(strings = { "client-p12", "client-pem", "client-trust-all" })
    void vertxHttpPresentsClientCertificate(String sslBeanName) {
        RestAssured.given()
                .get("/tls-registry/mtls/vertx-http/" + sslBeanName)
                .then()
                .statusCode(200)
                .body(equalTo("pong"));
    }

    /**
     * Proves the server enforces client authentication, so that the tests above are meaningful. Asserting the
     * handshake failure, rather than any error, also rules out the server not having started.
     */
    @ParameterizedTest
    @ValueSource(strings = { "http", "vertx-http" })
    void callWithoutClientCertificateIsRejected(String component) {
        RestAssured.given()
                .get("/tls-registry/mtls/" + component + "/client-no-key")
                .then()
                .statusCode(500)
                // With TLS 1.3 the client finishes its handshake before the server rejects it, so a client already
                // writing the request may see the closed socket instead of the TLS alert
                .body(anyOf(
                        containsString("javax.net.ssl.SSLHandshakeException: (certificate_required)"),
                        containsString("java.net.SocketException: An established connection was aborted"),
                        containsString("java.net.SocketException: Broken pipe"),
                        containsString("java.net.SocketException: Connection reset")));
    }

    public static class MutualTlsProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.ofEntries(
                    Map.entry("quarkus.camel.tls-registry.enabled", "true"),
                    Map.entry("quarkus.camel.tls-registry.quarkus-default-as-global", "false"),
                    // Server, see MutualTlsServer
                    Map.entry("quarkus.tls.mtls-server.key-store.p12.path", "target/certs/tls-mtls-keystore.p12"),
                    Map.entry("quarkus.tls.mtls-server.key-store.p12.password", "changeit"),
                    Map.entry("quarkus.tls.mtls-server.trust-store.p12.path", "target/certs/tls-mtls-server-truststore.p12"),
                    Map.entry("quarkus.tls.mtls-server.trust-store.p12.password", "changeit"),
                    // Client with PKCS12 key material
                    Map.entry("quarkus.tls.client-p12.key-store.p12.path", "target/certs/tls-mtls-client-keystore.p12"),
                    Map.entry("quarkus.tls.client-p12.key-store.p12.password", "changeit"),
                    Map.entry("quarkus.tls.client-p12.trust-store.p12.path", "target/certs/tls-mtls-client-truststore.p12"),
                    Map.entry("quarkus.tls.client-p12.trust-store.p12.password", "changeit"),
                    // Client with PEM key material
                    Map.entry("quarkus.tls.client-pem.key-store.pem.0.cert", "target/certs/tls-mtls-client.crt"),
                    Map.entry("quarkus.tls.client-pem.key-store.pem.0.key", "target/certs/tls-mtls-client.key"),
                    Map.entry("quarkus.tls.client-pem.trust-store.pem.certs", "target/certs/tls-mtls-client-ca.crt"),
                    // Client trusting every server, so it only succeeds when trust-all is carried over
                    Map.entry("quarkus.tls.client-trust-all.key-store.p12.path", "target/certs/tls-mtls-client-keystore.p12"),
                    Map.entry("quarkus.tls.client-trust-all.key-store.p12.password", "changeit"),
                    Map.entry("quarkus.tls.client-trust-all.trust-all", "true"),
                    // Client without key material
                    Map.entry("quarkus.tls.client-no-key.trust-store.p12.path",
                            "target/certs/tls-mtls-client-truststore.p12"),
                    Map.entry("quarkus.tls.client-no-key.trust-store.p12.password", "changeit"));
        }
    }
}
