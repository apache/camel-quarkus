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
 * Kubernetes SSL client authentication authenticates every client at the transport, so the restrictor does not
 * also confine them to loopback. Consoles reaching the agent on the pod IP are the reason client authentication
 * is configured at all.
 *
 * A client principal is configured here, so the authenticated peer is a known identity rather than any holder of
 * a service CA signed certificate. Origins would therefore be lifted too, except that `allowed-origins` is set
 * and the explicit list wins.
 */
class JolokiaKubernetesClientAuthRemoteAccessTest {

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
            .overrideConfigKey("quarkus.camel.jolokia.kubernetes.client-principal", "cn=hawtio-online.hawtio.svc")
            .overrideConfigKey("quarkus.camel.jolokia.allowed-origins", "https://hawtio.example.com");

    @Test
    void remoteAddressesAllowed() {
        CamelJolokiaRestrictor restrictor = new CamelJolokiaRestrictor();
        assertTrue(restrictor.isRemoteAccessAllowed("10.128.4.7"));
        assertTrue(restrictor.isRemoteAccessAllowed("127.0.0.1"));
    }

    @Test
    void originsStillRestricted() {
        CamelJolokiaRestrictor restrictor = new CamelJolokiaRestrictor();
        assertTrue(restrictor.isOriginAllowed("https://hawtio.example.com", true));
        assertFalse(restrictor.isOriginAllowed("https://untrusted.example", true));
    }

    @Test
    void mbeanDomainsStillRestricted() throws Exception {
        CamelJolokiaRestrictor restrictor = new CamelJolokiaRestrictor();
        assertTrue(restrictor.isObjectNameHidden(new javax.management.ObjectName("com.example:type=Test")));
    }
}
