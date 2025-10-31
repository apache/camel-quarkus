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

import java.io.File;
import java.util.Map;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.restassured.RestAssured;
import io.restassured.config.SSLConfig;
import io.smallrye.certs.Format;
import io.smallrye.certs.junit5.Certificate;
import io.smallrye.certs.junit5.Certificates;
import org.apache.http.NoHttpResponseException;
import org.apache.http.client.ClientProtocolException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.equalTo;

@Certificates(baseDir = "target/certs", certificates = {
        @Certificate(name = "kubernetes-service-cert", formats = { Format.PKCS12,
                Format.PEM }, password = "2s3cr3t")
})
@TestProfile(JolokiaKubernetesClientSSLTest.JolokiaAdditionalPropertiesProfile.class)
@QuarkusTest
class JolokiaKubernetesClientSSLTest {
    private static final File SERVER_CERT = new File("target/certs/kubernetes-service-cert.crt");
    private static final File SERVER_KEY = new File("target/certs/kubernetes-service-cert.key");
    private static final File CA_CERT = new File("target/certs/kubernetes-service-cert-ca.crt");

    @BeforeEach
    public void beforeEach() {
        RestAssured.port = 8778;
    }

    @Test
    void clientSSLAuthentication() {
        // Plain HTTP should be disabled
        assertThatThrownBy(() -> {
            RestAssured.given()
                    .get("/jolokia/")
                    .then()
                    .statusCode(200);
        }).isInstanceOfAny(NoHttpResponseException.class, ClientProtocolException.class);

        RestAssured.config = RestAssured.config().with().sslConfig(getSSLConfig());
        RestAssured.given()
                .get("https://localhost:8778/jolokia/")
                .then()
                .statusCode(200)
                .body(
                        "status", equalTo(200),
                        "value.details.secured", equalTo(true));
    }

    private SSLConfig getSSLConfig() {
        return new SSLConfig()
                .keystoreType("PKCS12")
                .keyStore("target/certs/kubernetes-service-cert-keystore.p12", "2s3cr3t")
                .trustStoreType("PKCS12")
                .trustStore("target/certs/kubernetes-service-cert-truststore.p12", "2s3cr3t");
    }

    public static final class JolokiaAdditionalPropertiesProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "kubernetes.service.host", "fake-host",
                    "quarkus.camel.jolokia.kubernetes.service-ca-cert", CA_CERT.getAbsolutePath(),
                    "quarkus.camel.jolokia.kubernetes.client-principal", "cn=localhost",
                    "quarkus.camel.jolokia.additional-properties.serverCert", SERVER_CERT.getAbsolutePath(),
                    "quarkus.camel.jolokia.additional-properties.serverKey", SERVER_KEY.getAbsolutePath(),
                    "quarkus.camel.jolokia.additional-properties.caCert", CA_CERT.getAbsolutePath());
        }
    }
}
