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
import org.jolokia.server.core.util.RequestType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A policy that says nothing about client addresses leaves `remote-access-allowed` to answer them, so bringing a
 * policy to restrict something else does not take the property away from an operator who is also relying on it.
 */
class CamelJolokiaRestrictorPolicyWithoutRemoteAllowsRemoteAccessTest {

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
            .overrideConfigKey("quarkus.camel.jolokia.remote-access-allowed", "true")
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addAsResource(new StringAsset(JOLOKIA_ACCESS_XML), "jolokia-access.xml"));

    @Test
    void remoteAccessAllowedStillApplies() {
        CamelJolokiaRestrictor restrictor = new CamelJolokiaRestrictor();
        assertTrue(restrictor.isRemoteAccessAllowed("10.0.0.1"));
        assertTrue(restrictor.isRemoteAccessAllowed("127.0.0.1"));
    }

    /**
     * The rest of the policy is unaffected by who answers addresses.
     */
    @Test
    void policyCommandsStillNarrowAccess() {
        CamelJolokiaRestrictor restrictor = new CamelJolokiaRestrictor();
        assertTrue(restrictor.isTypeAllowed(RequestType.LIST));
        assertFalse(restrictor.isTypeAllowed(RequestType.EXEC));
    }
}
