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
 * A policy with no `remote` section says nothing about client addresses, so the configured rules still answer
 * them and the loopback default holds.
 *
 * Jolokia reads an absent section as allowing every address. Applying that reading here would mean a policy
 * brought to restrict something else, such as the commands in use, silently withdrew the loopback default while
 * `remote-access-allowed` stayed `false` and read as though it were still in force. A policy that does carry a
 * `remote` section decides addresses outright, which
 * {@link CamelJolokiaRestrictorPolicyOverridesRemoteAccessTest} covers.
 */
class CamelJolokiaRestrictorPolicyWithoutRemoteTest {

    private static final String JOLOKIA_ACCESS_XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <restrict>
                <commands>
                    <command>list</command>
                    <command>read</command>
                </commands>
            </restrict>
            """;

    @RegisterExtension
    static final QuarkusUnitTest CONFIG = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addAsResource(new StringAsset(JOLOKIA_ACCESS_XML), "jolokia-access.xml"));

    @Test
    void loopbackDefaultStillApplies() {
        CamelJolokiaRestrictor restrictor = new CamelJolokiaRestrictor();
        assertTrue(restrictor.isRemoteAccessAllowed("127.0.0.1"));
        assertFalse(restrictor.isRemoteAccessAllowed("10.0.0.1"));
        assertFalse(restrictor.isRemoteAccessAllowed("192.168.1.1"));
    }

    @Test
    void policyCommandsStillNarrowAccess() {
        CamelJolokiaRestrictor restrictor = new CamelJolokiaRestrictor();
        assertTrue(restrictor.isTypeAllowed(org.jolokia.server.core.util.RequestType.LIST));
        assertFalse(restrictor.isTypeAllowed(org.jolokia.server.core.util.RequestType.EXEC));
    }
}
