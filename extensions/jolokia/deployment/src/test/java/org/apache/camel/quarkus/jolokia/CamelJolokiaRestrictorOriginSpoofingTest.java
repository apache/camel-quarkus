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
 * The origin check is what stops a page on another site from driving the agent through a visitor's browser, so
 * a request must not be able to dress itself up as a listed origin. The request is matched against the origin
 * both without and with the default port for its scheme, and this asserts that the extra spelling grants
 * nothing beyond the origin the request already had.
 */
class CamelJolokiaRestrictorOriginSpoofingTest {

    @RegisterExtension
    static final QuarkusUnitTest CONFIG = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.camel.jolokia.allowed-origins",
                    "https://good.example.com,http://plain.example.com:80,*://*.wild.example.com");

    /**
     * The two spellings of one origin, which is the whole point of matching both.
     */
    @Test
    void bothSpellingsOfTheListedOriginAllowed() {
        CamelJolokiaRestrictor restrictor = new CamelJolokiaRestrictor();
        assertTrue(restrictor.isOriginAllowed("https://good.example.com", true));
        assertTrue(restrictor.isOriginAllowed("https://good.example.com:443", true));
        assertTrue(restrictor.isOriginAllowed("http://plain.example.com", true));
        assertTrue(restrictor.isOriginAllowed("http://plain.example.com:80", true));
    }

    /**
     * A default port belongs to its own scheme only. Appending one must never let a request satisfy a rule
     * written for the other scheme.
     */
    @Test
    void defaultPortDoesNotCrossSchemes() {
        CamelJolokiaRestrictor restrictor = new CamelJolokiaRestrictor();
        // 443 is not the default for http, so no `:443` spelling is produced for an http request
        assertFalse(restrictor.isOriginAllowed("http://good.example.com", true));
        assertFalse(restrictor.isOriginAllowed("http://good.example.com:443", true));
        // 80 is not the default for https
        assertFalse(restrictor.isOriginAllowed("https://plain.example.com", true));
        assertFalse(restrictor.isOriginAllowed("https://plain.example.com:80", true));
    }

    /**
     * The extra spelling is only produced when the port is absent or already the default, so a request on any
     * other port keeps exactly one spelling and cannot borrow a rule meant for the default port.
     */
    @Test
    void otherPortsGainNothing() {
        CamelJolokiaRestrictor restrictor = new CamelJolokiaRestrictor();
        assertFalse(restrictor.isOriginAllowed("https://good.example.com:8443", true));
        assertFalse(restrictor.isOriginAllowed("https://good.example.com:4433", true));
        assertFalse(restrictor.isOriginAllowed("https://good.example.com:44", true));
        assertFalse(restrictor.isOriginAllowed("https://good.example.com:0", true));
        assertFalse(restrictor.isOriginAllowed("http://plain.example.com:8080", true));
    }

    /**
     * A foreign host is still foreign however the origin is spelled.
     */
    @Test
    void foreignHostsDeniedInEitherSpelling() {
        CamelJolokiaRestrictor restrictor = new CamelJolokiaRestrictor();
        assertFalse(restrictor.isOriginAllowed("https://untrusted.example", true));
        assertFalse(restrictor.isOriginAllowed("https://untrusted.example:443", true));
        // Suffix and prefix of a listed host
        assertFalse(restrictor.isOriginAllowed("https://good.example.com.untrusted.example", true));
        assertFalse(restrictor.isOriginAllowed("https://good.example.com.untrusted.example:443", true));
        assertFalse(restrictor.isOriginAllowed("https://evil-good.example.com", true));
        // A trailing dot is a different host, and must not be normalised into the listed one
        assertFalse(restrictor.isOriginAllowed("https://good.example.com.", true));
        assertFalse(restrictor.isOriginAllowed("https://good.example.com.:443", true));
    }

    /**
     * Credentials never appear in an origin, so their presence is an attempt to make a foreign host read as a
     * listed one. Appending a default port must not give such a value a second chance to match.
     */
    @Test
    void userInfoTricksDeniedInEitherSpelling() {
        CamelJolokiaRestrictor restrictor = new CamelJolokiaRestrictor();
        assertFalse(restrictor.isOriginAllowed("https://good.example.com@untrusted.example", true));
        assertFalse(restrictor.isOriginAllowed("https://good.example.com@untrusted.example:443", true));
        assertFalse(restrictor.isOriginAllowed("https://good.example.com:443@untrusted.example", true));
        assertFalse(restrictor.isOriginAllowed("https://untrusted.example@good.example.com", true));
    }

    /**
     * A Referer is reduced to its origin, which must not let a listed origin appearing in a path or query grant
     * access to the host that actually sent the request.
     */
    @Test
    void listedOriginInsideAPathGrantsNothing() {
        CamelJolokiaRestrictor restrictor = new CamelJolokiaRestrictor();
        assertFalse(restrictor.isOriginAllowed("https://untrusted.example/https://good.example.com", true));
        assertFalse(restrictor.isOriginAllowed("https://untrusted.example/?x=https://good.example.com:443", true));
        assertFalse(restrictor.isOriginAllowed("https://untrusted.example#https://good.example.com", true));
        // The listed origin with a path is still the listed origin
        assertTrue(restrictor.isOriginAllowed("https://good.example.com/jolokia/read", true));
        assertTrue(restrictor.isOriginAllowed("https://good.example.com:443/jolokia/read", true));
    }

    /**
     * A wildcard entry carries no port, so both spellings of a matching request have to be accepted while a
     * host outside the pattern stays denied.
     */
    @Test
    void wildcardEntriesBoundedByTheirPattern() {
        CamelJolokiaRestrictor restrictor = new CamelJolokiaRestrictor();
        assertTrue(restrictor.isOriginAllowed("https://console.wild.example.com", true));
        assertTrue(restrictor.isOriginAllowed("https://console.wild.example.com:443", true));
        assertTrue(restrictor.isOriginAllowed("http://console.wild.example.com", true));
        // Outside the wildcard
        assertFalse(restrictor.isOriginAllowed("https://wild.example.com.untrusted.example", true));
        assertFalse(restrictor.isOriginAllowed("https://console.wild.example.com.evil.example", true));
        // The wildcard covers the host, not the port
        assertFalse(restrictor.isOriginAllowed("https://console.wild.example.com:8443", true));
    }

    /**
     * Values that are not origins must not acquire one by having a default port appended.
     */
    @Test
    void malformedValuesStillDenied() {
        CamelJolokiaRestrictor restrictor = new CamelJolokiaRestrictor();
        assertFalse(restrictor.isOriginAllowed("good.example.com", true));
        assertFalse(restrictor.isOriginAllowed("//good.example.com", true));
        assertFalse(restrictor.isOriginAllowed("https://", true));
        assertFalse(restrictor.isOriginAllowed("https://good.example.com:443:443", true));
        assertFalse(restrictor.isOriginAllowed("https://good.example.com:notaport", true));
        assertFalse(restrictor.isOriginAllowed("null", true));
        assertFalse(restrictor.isOriginAllowed("", true));
        assertFalse(restrictor.isOriginAllowed("   ", true));
        // A wildcard is not a host a request can come from
        assertFalse(restrictor.isOriginAllowed("https://*.wild.example.com", true));
    }

    /**
     * Case folding applies to both spellings, and cannot be used to smuggle a foreign host past the check.
     */
    @Test
    void caseFoldingIsBounded() {
        CamelJolokiaRestrictor restrictor = new CamelJolokiaRestrictor();
        assertTrue(restrictor.isOriginAllowed("HTTPS://GOOD.EXAMPLE.COM", true));
        assertTrue(restrictor.isOriginAllowed("HttPs://Good.Example.Com:443", true));
        assertFalse(restrictor.isOriginAllowed("HTTPS://UNTRUSTED.EXAMPLE", true));
    }
}
