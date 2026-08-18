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
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * An access policy governs client addresses and what a client may do. It says nothing about origins, since the
 * `cors` section is not consulted, so bringing one must not withdraw the origins a client principal allows.
 *
 * The policy here restricts addresses, and that restriction is expected to take effect while origins stay lifted.
 * The two controls are answered independently.
 */
class JolokiaKubernetesClientPrincipalWithPolicyOriginsTest {

    private static final String JOLOKIA_ACCESS_XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <restrict>
                <remote>
                    <host>10.0.0.0/8</host>
                </remote>
            </restrict>
            """;

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
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addAsResource(new StringAsset(JOLOKIA_ACCESS_XML), "jolokia-access.xml"))
            .overrideConfigKey("kubernetes.service.host", "fake-host")
            .overrideConfigKey("quarkus.camel.jolokia.kubernetes.service-ca-cert", CA_CERT.getAbsolutePath())
            .overrideConfigKey("quarkus.camel.jolokia.kubernetes.client-principal", "cn=hawtio-online.hawtio.svc");

    @Test
    void originLiftSurvivesAnAccessPolicy() {
        CamelJolokiaRestrictor restrictor = new CamelJolokiaRestrictor();
        assertTrue(restrictor.isOriginAllowed("https://hawtio-online.apps.example.com", true));
        assertTrue(restrictor.isOriginAllowed("https://anything.example", true));
    }

    @Test
    void policyStillDecidesAddresses() {
        CamelJolokiaRestrictor restrictor = new CamelJolokiaRestrictor();
        assertTrue(restrictor.isRemoteAccessAllowed("10.0.0.1"));
        assertFalse(restrictor.isRemoteAccessAllowed("192.168.1.1"));
    }
}
