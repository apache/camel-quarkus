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

import org.jolokia.server.core.config.ConfigKey;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JolokiaRecorderTest {

    @Test
    void localhostIsLoopback() {
        assertTrue(JolokiaRecorder.isLoopbackHost("localhost"));
    }

    @Test
    void ipv4LoopbackIsLoopback() {
        assertTrue(JolokiaRecorder.isLoopbackHost("127.0.0.1"));
    }

    @Test
    void ipv6LoopbackIsLoopback() {
        assertTrue(JolokiaRecorder.isLoopbackHost("::1"));
    }

    @Test
    void allInterfacesIsNotLoopback() {
        assertFalse(JolokiaRecorder.isLoopbackHost("0.0.0.0"));
    }

    @Test
    void nonLoopbackIpIsNotLoopback() {
        assertFalse(JolokiaRecorder.isLoopbackHost("192.168.1.1"));
    }

    @Test
    void ipv4NonLoopbackRangeIsNotLoopback() {
        assertFalse(JolokiaRecorder.isLoopbackHost("10.0.0.1"));
    }

    @Test
    void ipv6BracketWrappedIsLoopback() {
        assertTrue(JolokiaRecorder.isLoopbackHost("[::1]"));
    }

    @Test
    void localhostLocaldomainIsLoopback() {
        assertTrue(JolokiaRecorder.isLoopbackHost("localhost.localdomain"));
    }

    @Test
    void ipv4FullLoopbackRangeIsLoopback() {
        assertTrue(JolokiaRecorder.isLoopbackHost("127.255.255.255"));
    }

    @Test
    void ipv6LongFormLoopbackIsLoopback() {
        assertTrue(JolokiaRecorder.isLoopbackHost("0:0:0:0:0:0:0:1"));
    }

    @Test
    void ipv4MappedIpv6LoopbackIsLoopback() {
        assertTrue(JolokiaRecorder.isLoopbackHost("::ffff:127.0.0.1"));
    }

    @Test
    void ipv6ScopedLoopbackIsLoopback() {
        // No scope id is resolved, so none of these depend on the interfaces this host has
        assertTrue(JolokiaRecorder.isLoopbackHost("::1%lo0"));
        assertTrue(JolokiaRecorder.isLoopbackHost("::1%lo"));
        assertTrue(JolokiaRecorder.isLoopbackHost("::1%1"));
        assertTrue(JolokiaRecorder.isLoopbackHost("[::1%eth0]"));
    }

    @Test
    void ipv6ScopedNonLoopbackIsNotLoopback() {
        assertFalse(JolokiaRecorder.isLoopbackHost("fe80::1%lo0"));
    }

    @Test
    void ipv6MalformedScopeIsNotLoopback() {
        assertFalse(JolokiaRecorder.isLoopbackHost("::1%"));
        assertFalse(JolokiaRecorder.isLoopbackHost("::1%lo0/x"));
    }

    @Test
    void ipv6LeadingZeroLoopbackIsLoopback() {
        assertTrue(JolokiaRecorder.isLoopbackHost("0:0:0:0:0:0:0:0001"));
    }

    /**
     * Resolving a malformed literal would let the local name service decide whether the agent counts as exposed.
     */
    @Test
    void malformedAddressLiteralIsNotLoopbackWithoutResolving() {
        assertFalse(JolokiaRecorder.isLoopbackHost(".::1"));
        assertFalse(JolokiaRecorder.isLoopbackHost("127.0.0.1.5"));
        assertFalse(JolokiaRecorder.isLoopbackHost("127.0.0"));
    }

    @Test
    void ipv6NonLoopbackIsNotLoopback() {
        assertFalse(JolokiaRecorder.isLoopbackHost("fe80::1"));
        assertFalse(JolokiaRecorder.isLoopbackHost("foo:bar"));
    }

    @Test
    void ipv4LoopbackWithSignedOctetIsNotLoopback() {
        assertFalse(JolokiaRecorder.isLoopbackHost("127.0.0.+1"));
    }

    @Test
    void loopbackNameInRootFormIsLoopback() {
        assertTrue(JolokiaRecorder.isLoopbackHost("localhost."));
        assertTrue(JolokiaRecorder.isLoopbackHost("localhost.localdomain."));
    }

    @Test
    void ipv4LoopbackWithTrailingDotIsNotLoopback() {
        assertFalse(JolokiaRecorder.isLoopbackHost("127.0.0.1."));
    }

    /**
     * Jolokia prepends the name the client address resolves back to when this is on, and a host whose loopback
     * address does not resolve to `localhost` then refuses local clients. If this fails after a Jolokia
     * upgrade, that lookup became the default and the extension documentation needs to say so.
     */
    @Test
    void dnsReverseLookupIsOffByDefault() {
        assertEquals("false", ConfigKey.ALLOW_DNS_REVERSE_LOOKUP.getDefaultValue());
    }

    @Test
    void nullIsNotLoopback() {
        assertFalse(JolokiaRecorder.isLoopbackHost(null));
    }

    @Test
    void emptyStringIsNotLoopback() {
        assertFalse(JolokiaRecorder.isLoopbackHost(""));
    }

    @Test
    void ipv4LoopbackWithExtraOctetsIsNotLoopback() {
        assertFalse(JolokiaRecorder.isLoopbackHost("127.0.0.1.untrusted.example"));
    }

    @Test
    void ipv4LoopbackOctetOverflowIsNotLoopback() {
        assertFalse(JolokiaRecorder.isLoopbackHost("127.0.0.256"));
    }
}
