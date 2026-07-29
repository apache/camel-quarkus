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

import org.junit.jupiter.api.Test;

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
