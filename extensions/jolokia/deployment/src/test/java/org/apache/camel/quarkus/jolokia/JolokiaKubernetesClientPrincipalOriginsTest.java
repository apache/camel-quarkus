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

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * With a client principal pinning which identity may connect, the authenticated peer is a known console rather
 * than any holder of a certificate the service CA happened to sign, so cross-origin requests it forwards are
 * accepted without the origin having to be listed.
 *
 * This is what lets a Hawtio Online deployment upgrade untouched. 3.38.0 already required the client principal
 * and applied no origin check at all, so the one property such a deployment already carries is the one that
 * keeps its console working.
 */
class JolokiaKubernetesClientPrincipalOriginsTest {

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
            .overrideConfigKey("quarkus.camel.jolokia.kubernetes.service-ca-cert", CA_CERT.getAbsolutePath())
            .overrideConfigKey("quarkus.camel.jolokia.kubernetes.client-principal", "cn=hawtio-online.hawtio.svc");

    @Test
    void anyOriginAllowedWhenClientIdentityIsPinned() {
        CamelJolokiaRestrictor restrictor = new CamelJolokiaRestrictor();
        assertTrue(restrictor.isOriginAllowed("https://hawtio-online.apps.example.com", true));
        assertTrue(restrictor.isOriginAllowed("https://anything.example", true));
    }

    @Test
    void remoteAddressesAllowed() {
        CamelJolokiaRestrictor restrictor = new CamelJolokiaRestrictor();
        assertTrue(restrictor.isRemoteAccessAllowed("10.128.4.7"));
    }

    @Test
    void mbeanDomainsStillRestricted() throws Exception {
        CamelJolokiaRestrictor restrictor = new CamelJolokiaRestrictor();
        assertTrue(restrictor.isObjectNameHidden(new javax.management.ObjectName("com.example:type=Test")));
    }
}
