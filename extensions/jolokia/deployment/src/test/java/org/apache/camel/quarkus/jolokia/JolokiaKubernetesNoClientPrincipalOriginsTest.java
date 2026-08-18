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
package org.apache.camel.quarkus.jolokia;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import io.quarkus.test.QuarkusUnitTest;
import org.apache.camel.quarkus.jolokia.restrictor.CamelJolokiaRestrictor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Without a client principal, any certificate the service CA signed is accepted, so the authenticated peer is
 * not a known identity and cross-origin requests stay restricted. Addresses are still lifted, since the client
 * has at least been authenticated.
 */
class JolokiaKubernetesNoClientPrincipalOriginsTest {

    static final File CA_CERT;

    static {
        try {
            CA_CERT = Files.createTempFile("fake-ca", ".crt").toFile();
            CA_CERT.deleteOnExit();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @RegisterExtension
    static final QuarkusUnitTest CONFIG = new QuarkusUnitTest()
            .withEmptyApplication()
            .overrideConfigKey("kubernetes.service.host", "fake-host")
            .overrideConfigKey("quarkus.camel.jolokia.kubernetes.service-ca-cert", CA_CERT.getAbsolutePath());

    @Test
    void unlistedOriginsDenied() {
        CamelJolokiaRestrictor restrictor = new CamelJolokiaRestrictor();
        assertFalse(restrictor.isOriginAllowed("https://hawtio-online.apps.example.com", true));
    }

    @Test
    void loopbackOriginsAndNoOriginStillAllowed() {
        CamelJolokiaRestrictor restrictor = new CamelJolokiaRestrictor();
        assertTrue(restrictor.isOriginAllowed(null, true));
        assertTrue(restrictor.isOriginAllowed("http://localhost:8080", true));
    }

    @Test
    void remoteAddressesStillAllowed() {
        CamelJolokiaRestrictor restrictor = new CamelJolokiaRestrictor();
        assertTrue(restrictor.isRemoteAccessAllowed("10.128.4.7"));
    }
}
