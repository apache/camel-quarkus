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

import io.quarkus.test.QuarkusExtensionTest;
import org.apache.camel.quarkus.jolokia.restrictor.CamelJolokiaRestrictor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Asking for remote clients must not withdraw the origins a client principal allows.
 *
 * `remote-access-allowed` is a control over client addresses and nothing else, so setting it on Kubernetes says
 * nothing about origins. It is also a reasonable thing to set there, being what the documentation prescribes
 * everywhere else, and a deployment that sets both must not find its console refused for having asked for
 * something unrelated.
 */
class JolokiaKubernetesClientPrincipalWithRemoteAccessOriginsTest {

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
    static final QuarkusExtensionTest CONFIG = new QuarkusExtensionTest()
            .withEmptyApplication()
            .overrideConfigKey("kubernetes.service.host", "fake-host")
            .overrideConfigKey("quarkus.camel.jolokia.kubernetes.service-ca-cert", CA_CERT.getAbsolutePath())
            .overrideConfigKey("quarkus.camel.jolokia.kubernetes.client-principal", "cn=hawtio-online.hawtio.svc")
            .overrideConfigKey("quarkus.camel.jolokia.remote-access-allowed", "true");

    @Test
    void originLiftSurvivesRemoteAccessAllowed() {
        CamelJolokiaRestrictor restrictor = new CamelJolokiaRestrictor();
        assertTrue(restrictor.isOriginAllowed("https://hawtio-online.apps.example.com", true));
        assertTrue(restrictor.isOriginAllowed("https://anything.example", true));
    }

    @Test
    void remoteAddressesAllowed() {
        CamelJolokiaRestrictor restrictor = new CamelJolokiaRestrictor();
        assertTrue(restrictor.isRemoteAccessAllowed("10.128.4.7"));
    }
}
