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

import io.quarkus.test.QuarkusExtensionTest;
import org.apache.camel.quarkus.jolokia.restrictor.CamelJolokiaRestrictor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Origins are compiled the same way Jolokia compiles an access policy `allow-origin` rule, so the values of an
 * existing policy file can be moved to `allowed-origins` unchanged.
 */
class CamelJolokiaRestrictorWildcardOriginsTest {

    @RegisterExtension
    static final QuarkusExtensionTest CONFIG = new QuarkusExtensionTest()
            .overrideConfigKey("quarkus.camel.jolokia.allowed-origins",
                    "*://*.example.com,https://fixed.example.org");

    @Test
    void wildcardMatchesAnySchemeAndSubdomain() {
        CamelJolokiaRestrictor restrictor = new CamelJolokiaRestrictor();
        assertTrue(restrictor.isOriginAllowed("https://hawtio.example.com", true));
        assertTrue(restrictor.isOriginAllowed("http://monitoring.example.com", true));
        assertTrue(restrictor.isOriginAllowed("https://a.b.example.com", true));
    }

    @Test
    void wildcardStillAnchoredAtBothEnds() {
        CamelJolokiaRestrictor restrictor = new CamelJolokiaRestrictor();
        assertFalse(restrictor.isOriginAllowed("https://example.com.untrusted.example", true));
        assertFalse(restrictor.isOriginAllowed("https://hawtio.example.com.evil.test", true));
        // The wildcard covers a subdomain label, not the bare domain
        assertFalse(restrictor.isOriginAllowed("https://example.com", true));
    }

    @Test
    void wildcardAppliesToRefererStyleValues() {
        CamelJolokiaRestrictor restrictor = new CamelJolokiaRestrictor();
        assertTrue(restrictor.isOriginAllowed("https://hawtio.example.com/hawtio/jmx", true));
    }

    @Test
    void nonWildcardEntriesUnaffected() {
        CamelJolokiaRestrictor restrictor = new CamelJolokiaRestrictor();
        assertTrue(restrictor.isOriginAllowed("https://fixed.example.org", true));
        assertFalse(restrictor.isOriginAllowed("https://other.example.org", true));
    }
}
