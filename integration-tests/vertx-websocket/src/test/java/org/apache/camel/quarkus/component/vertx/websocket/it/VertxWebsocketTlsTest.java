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
package org.apache.camel.quarkus.component.vertx.websocket.it;

import java.util.Map;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.smallrye.certs.Format;
import io.smallrye.certs.junit5.Certificate;
import org.apache.camel.quarkus.test.support.certificate.TestCertificates;

@TestCertificates(certificates = {
        @Certificate(name = "vertx-websocket", formats = {
                Format.PKCS12, Format.PEM }, password = "changeit") })
@TestProfile(VertxWebsocketTlsTest.VertxWebsocketTlsTestProfile.class)
@QuarkusTest
class VertxWebsocketTlsTest extends VertxWebsocketSslTest {
    public static class VertxWebsocketTlsTestProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "quarkus.tls.key-store.pem.0.cert", "target/certs/vertx-websocket.crt",
                    "quarkus.tls.key-store.pem.0.key", "target/certs/vertx-websocket.key",
                    "quarkus.http.insecure-requests", "disabled");
        }
    }
}
