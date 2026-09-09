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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Cross-origin access is granted by Camel Quarkus configuration rather than by reinterpreting the allow-origin
 * rules of a Jolokia access policy, which do not grant access on a plain Jolokia agent either.
 */
class CamelJolokiaRestrictorAllowedOriginsTest {

    @RegisterExtension
    static final QuarkusUnitTest CONFIG = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.camel.jolokia.allowed-origins",
                    "https://domain.example.com,https://monitoring.example.com");

    @Test
    void configuredOriginsAllowed() {
        CamelJolokiaRestrictor restrictor = new CamelJolokiaRestrictor();
        assertTrue(restrictor.isOriginAllowed("https://domain.example.com", true));
        assertTrue(restrictor.isOriginAllowed("https://monitoring.example.com", true));
    }

    @Test
    void originMatchingIsCaseInsensitive() {
        CamelJolokiaRestrictor restrictor = new CamelJolokiaRestrictor();
        assertTrue(restrictor.isOriginAllowed("HTTPS://domain.Example.COM", true));
    }

    @Test
    void unlistedOriginsDenied() {
        CamelJolokiaRestrictor restrictor = new CamelJolokiaRestrictor();
        assertFalse(restrictor.isOriginAllowed("https://untrusted.example", true));
        // Matched in full, so neither a prefix nor a suffix of a listed origin is enough
        assertFalse(restrictor.isOriginAllowed("https://domain.example.com.untrusted.example", true));
        assertFalse(restrictor.isOriginAllowed("https://bad.domain.example.com", true));
        assertFalse(restrictor.isOriginAllowed("https://domain.example.com:8443", true));
        // A different scheme is a different origin
        assertFalse(restrictor.isOriginAllowed("http://domain.example.com", true));
    }

    /**
     * Jolokia falls back to the Referer header when a request carries no Origin, which happens for any
     * same-origin GET a console proxies through. A Referer carries a path, so the value has to be reduced to an
     * origin before being matched, or a listed origin would be refused.
     */
    @Test
    void refererStyleValuesMatchTheListedOrigin() {
        CamelJolokiaRestrictor restrictor = new CamelJolokiaRestrictor();
        assertTrue(restrictor.isOriginAllowed("https://domain.example.com/", true));
        assertTrue(restrictor.isOriginAllowed("https://domain.example.com/jolokia/read/java.lang:type=Memory", true));
        assertTrue(restrictor.isOriginAllowed("https://domain.example.com/x?y=z#frag", true));
        // Reducing to an origin must not turn a foreign host into a listed one
        assertFalse(restrictor.isOriginAllowed("https://untrusted.example/https://domain.example.com", true));
    }

    /**
     * A port that is the default for the scheme is left out, matching how a browser serialises an origin.
     */
    @Test
    void defaultPortsAreEquivalentToNoPort() {
        CamelJolokiaRestrictor restrictor = new CamelJolokiaRestrictor();
        assertTrue(restrictor.isOriginAllowed("https://domain.example.com:443", true));
        assertFalse(restrictor.isOriginAllowed("https://domain.example.com:80", true));
    }

    @Test
    void valuesThatAreNotOriginsDenied() {
        CamelJolokiaRestrictor restrictor = new CamelJolokiaRestrictor();
        // Opaque origin, sent by sandboxed frames and some redirects
        assertFalse(restrictor.isOriginAllowed("null", true));
        assertFalse(restrictor.isOriginAllowed("", true));
        assertFalse(restrictor.isOriginAllowed("//domain.example.com", true));
        assertFalse(restrictor.isOriginAllowed("domain.example.com", true));
    }

    @Test
    void loopbackAndNullOriginsStillAllowed() {
        CamelJolokiaRestrictor restrictor = new CamelJolokiaRestrictor();
        assertTrue(restrictor.isOriginAllowed(null, true));
        assertTrue(restrictor.isOriginAllowed("http://localhost:8080", true));
    }

    @Test
    void remoteAccessUnaffected() {
        CamelJolokiaRestrictor restrictor = new CamelJolokiaRestrictor();
        assertFalse(restrictor.isRemoteAccessAllowed("10.0.0.1"));
    }
}
