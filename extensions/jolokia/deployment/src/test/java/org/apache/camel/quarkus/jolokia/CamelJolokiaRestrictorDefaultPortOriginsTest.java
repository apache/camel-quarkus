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
 * An origin listed with the default port for its scheme has to work, since that is how an address bar and the
 * `<allow-origin>` rules of an access policy may spell it, while a browser sends the origin without it. Both
 * denote the same origin, so a configured value that includes the port must not silently match nothing.
 */
class CamelJolokiaRestrictorDefaultPortOriginsTest {

    @RegisterExtension
    static final QuarkusUnitTest CONFIG = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.camel.jolokia.allowed-origins",
                    "https://secure.example.com:443,http://plain.example.com:80,*://*.wild.example.com:443");

    @Test
    void listedWithDefaultPortMatchesBrowserOrigin() {
        CamelJolokiaRestrictor restrictor = new CamelJolokiaRestrictor();
        assertTrue(restrictor.isOriginAllowed("https://secure.example.com", true));
        assertTrue(restrictor.isOriginAllowed("http://plain.example.com", true));
    }

    @Test
    void listedWithDefaultPortMatchesTheSameSpelling() {
        CamelJolokiaRestrictor restrictor = new CamelJolokiaRestrictor();
        assertTrue(restrictor.isOriginAllowed("https://secure.example.com:443", true));
        assertTrue(restrictor.isOriginAllowed("http://plain.example.com:80", true));
    }

    /**
     * A wildcard entry cannot be reduced to a URI, so the request has to be matched against both spellings
     * rather than the configured value being rewritten.
     */
    @Test
    void wildcardEntriesWithDefaultPortMatchToo() {
        CamelJolokiaRestrictor restrictor = new CamelJolokiaRestrictor();
        assertTrue(restrictor.isOriginAllowed("https://console.wild.example.com", true));
        assertTrue(restrictor.isOriginAllowed("https://console.wild.example.com:443", true));
    }

    /**
     * Only the default port for the scheme is interchangeable. Everything else stays a distinct origin.
     */
    @Test
    void otherPortsRemainDistinctOrigins() {
        CamelJolokiaRestrictor restrictor = new CamelJolokiaRestrictor();
        assertFalse(restrictor.isOriginAllowed("https://secure.example.com:8443", true));
        assertFalse(restrictor.isOriginAllowed("http://plain.example.com:8080", true));
        // The default port of the other scheme is not the default of this one
        assertFalse(restrictor.isOriginAllowed("https://secure.example.com:80", true));
        assertFalse(restrictor.isOriginAllowed("http://plain.example.com:443", true));
        // A matching port does not make a foreign host listed
        assertFalse(restrictor.isOriginAllowed("https://untrusted.example:443", true));
    }

    /**
     * Matching both spellings must not let a scheme through that was never listed.
     */
    @Test
    void schemeStillMatters() {
        CamelJolokiaRestrictor restrictor = new CamelJolokiaRestrictor();
        assertFalse(restrictor.isOriginAllowed("http://secure.example.com", true));
        assertFalse(restrictor.isOriginAllowed("https://plain.example.com", true));
    }
}
