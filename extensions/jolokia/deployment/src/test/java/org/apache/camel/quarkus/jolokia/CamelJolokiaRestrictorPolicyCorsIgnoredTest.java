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
 * The cors section of an access policy is not consulted, so that the origins which may reach the agent are
 * configured in one place. Everything else in the policy still applies.
 */
class CamelJolokiaRestrictorPolicyCorsIgnoredTest {

    private static final String JOLOKIA_ACCESS_XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <restrict>
                <cors>
                    <allow-origin>https://policy-only.example.com</allow-origin>
                    <strict-checking/>
                    <ignore-scheme/>
                </cors>
                <commands>
                    <command>read</command>
                </commands>
            </restrict>
            """;

    @RegisterExtension
    static final QuarkusUnitTest CONFIG = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.camel.jolokia.allowed-origins", "https://domain.example.com")
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addAsResource(new StringAsset(JOLOKIA_ACCESS_XML), "jolokia-access.xml"));

    /**
     * Listing an origin in the policy does not grant it access, even with strict-checking enabled.
     */
    @Test
    void policyAllowOriginDoesNotGrantAccess() {
        CamelJolokiaRestrictor restrictor = new CamelJolokiaRestrictor();
        assertFalse(restrictor.isOriginAllowed("https://policy-only.example.com", true));
        assertFalse(restrictor.isOriginAllowed("https://policy-only.example.com", false));
    }

    /**
     * A policy enabling strict-checking would otherwise reject every origin it does not list, including a null
     * origin. The configured origins are unaffected by it.
     */
    @Test
    void policyStrictCheckingDoesNotNarrowTheConfiguredOrigins() {
        CamelJolokiaRestrictor restrictor = new CamelJolokiaRestrictor();
        assertTrue(restrictor.isOriginAllowed("https://domain.example.com", true));
        assertTrue(restrictor.isOriginAllowed("http://localhost:8080", true));
        assertTrue(restrictor.isOriginAllowed(null, true));
    }

    @Test
    void unlistedOriginsStillDenied() {
        CamelJolokiaRestrictor restrictor = new CamelJolokiaRestrictor();
        assertFalse(restrictor.isOriginAllowed("https://untrusted.example", true));
    }

    /**
     * The whole section is skipped, including ignore-scheme, so it can be deleted from a policy without
     * changing anything. `quarkus.camel.jolokia.ignore-origin-scheme` replaces it.
     */
    @Test
    void policyIgnoreSchemeHasNoEffect() {
        CamelJolokiaRestrictor restrictor = new CamelJolokiaRestrictor();
        assertFalse(restrictor.ignoreScheme());
    }

    /**
     * Only the cors section is skipped. The rest of the policy is still delegated to.
     */
    @Test
    void remainderOfThePolicyStillApplies() {
        CamelJolokiaRestrictor restrictor = new CamelJolokiaRestrictor();
        assertTrue(restrictor.isTypeAllowed(RequestType.READ));
        assertFalse(restrictor.isTypeAllowed(RequestType.EXEC));
    }
}
