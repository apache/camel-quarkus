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

import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import io.quarkus.test.QuarkusExtensionTest;
import org.apache.camel.quarkus.jolokia.restrictor.CamelJolokiaRestrictor;
import org.jolokia.server.core.restrictor.policy.CorsChecker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The documentation says the `<allow-origin>` values of a Jolokia access policy can be moved to
 * `allowed-origins` unchanged, which holds only while both compile a value the same way. Jolokia is not
 * obliged to keep that syntax, so this compares the two rather than trusting them to stay aligned. A failure
 * means the wildcard syntax has diverged upstream and the documentation no longer holds.
 *
 * Only the subset where the two are meant to agree is compared. The restrictor deliberately differs elsewhere:
 * it reduces a value to a scheme, host and port before matching, matches a default port in either spelling,
 * lowercases the request, and allows loopback origins outright, none of which Jolokia does. So the origins
 * below are lowercase, carry no path, no credentials, no default port, and no loopback host.
 */
class CamelJolokiaRestrictorAllowOriginParityTest {

    private static final List<String> ALLOW_ORIGINS = List.of(
            "https://console.example.com",
            "*://*.wild.example.com",
            "https://*.example.org",
            "http://fixed.example.net:8080");

    private static final List<String> ORIGINS = List.of(
            // Exact matches and near misses
            "https://console.example.com",
            "https://other.example.com",
            "http://console.example.com",
            "https://console.example.com.evil.example",
            "https://evil.example",
            // Wildcard scheme and host
            "https://a.wild.example.com",
            "http://a.wild.example.com",
            "https://a.b.wild.example.com",
            "https://wild.example.com",
            "https://wild.example.com.evil.example",
            // Wildcard host only
            "https://x.example.org",
            "http://x.example.org",
            "https://example.org",
            // Explicit non default port
            "http://fixed.example.net:8080",
            "http://fixed.example.net:8081",
            "http://fixed.example.net");

    @RegisterExtension
    static final QuarkusExtensionTest CONFIG = new QuarkusExtensionTest()
            .overrideConfigKey("quarkus.camel.jolokia.allowed-origins", String.join(",", ALLOW_ORIGINS));

    @Test
    void wildcardSyntaxMatchesJolokia() throws Exception {
        CamelJolokiaRestrictor restrictor = new CamelJolokiaRestrictor();
        CorsChecker corsChecker = corsCheckerFor(ALLOW_ORIGINS);

        for (String origin : ORIGINS) {
            assertEquals(corsChecker.check(origin, false), restrictor.isOriginAllowed(origin, true),
                    "Jolokia and the Camel restrictor disagree on " + origin
                            + ", so an <allow-origin> value no longer means the same in allowed-origins");
        }
    }

    /**
     * A corpus that only ever produced one verdict would agree with anything, including a restrictor that
     * refused everything.
     */
    @Test
    void corpusCoversBothVerdicts() {
        CamelJolokiaRestrictor restrictor = new CamelJolokiaRestrictor();
        long allowed = ORIGINS.stream().filter(origin -> restrictor.isOriginAllowed(origin, true)).count();

        assertTrue(allowed > 0, "No origin in the corpus is allowed");
        assertTrue(allowed < ORIGINS.size(), "Every origin in the corpus is allowed");
    }

    /**
     * Builds the policy document Jolokia's own checker expects, so that the comparison runs against the real
     * implementation rather than a copy of it. The document is assembled rather than parsed, so no XML is read.
     */
    private static CorsChecker corsCheckerFor(List<String> allowOrigins) throws Exception {
        Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        Element restrict = document.createElement("restrict");
        document.appendChild(restrict);

        Element cors = document.createElement("cors");
        restrict.appendChild(cors);

        for (String allowOrigin : allowOrigins) {
            Element element = document.createElement("allow-origin");
            element.setTextContent(allowOrigin);
            cors.appendChild(element);
        }

        return new CorsChecker(document);
    }
}
