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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CamelJolokiaRestrictorFilePolicyTest {

    private static final String JOLOKIA_ACCESS_XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <restrict>
                <remote>
                    <host>10.0.0.0/8</host>
                </remote>
            </restrict>
            """;

    static final File POLICY_FILE;

    static {
        try {
            POLICY_FILE = Files.createTempFile("jolokia-access", ".xml").toFile();
            POLICY_FILE.deleteOnExit();
            Files.writeString(POLICY_FILE.toPath(), JOLOKIA_ACCESS_XML);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @RegisterExtension
    static final QuarkusExtensionTest CONFIG = new QuarkusExtensionTest()
            .overrideConfigKey("quarkus.camel.jolokia.additional-properties.policyLocation",
                    POLICY_FILE.toURI().toString());

    @Test
    void filePolicyAllowsConfiguredSubnet() {
        CamelJolokiaRestrictor restrictor = new CamelJolokiaRestrictor();
        assertTrue(restrictor.isRemoteAccessAllowed("10.0.0.1"));
        assertTrue(restrictor.isRemoteAccessAllowed("10.255.255.255"));
    }

    @Test
    void filePolicyDeniesOtherAddresses() {
        CamelJolokiaRestrictor restrictor = new CamelJolokiaRestrictor();
        assertFalse(restrictor.isRemoteAccessAllowed("192.168.1.1"));
        assertFalse(restrictor.isRemoteAccessAllowed("172.16.0.1"));
    }

    /**
     * Loopback is not in the subnet the policy allows, and the policy decides addresses outright, so it is
     * refused like any other address outside it.
     */
    @Test
    void loopbackDeniedByPolicy() {
        CamelJolokiaRestrictor restrictor = new CamelJolokiaRestrictor();
        assertFalse(restrictor.isRemoteAccessAllowed("127.0.0.1"));
        assertFalse(restrictor.isRemoteAccessAllowed("::1"));
    }
}
