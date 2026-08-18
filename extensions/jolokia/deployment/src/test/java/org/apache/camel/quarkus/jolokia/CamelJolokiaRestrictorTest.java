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

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import io.quarkus.test.QuarkusUnitTest;
import org.apache.camel.quarkus.jolokia.restrictor.CamelJolokiaRestrictor;
import org.jolokia.server.core.util.HttpMethod;
import org.jolokia.server.core.util.RequestType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CamelJolokiaRestrictorTest {
    @RegisterExtension
    static final QuarkusUnitTest CONFIG = new QuarkusUnitTest()
            .withEmptyApplication();

    @Test
    void ipv4LoopbackAllowed() {
        CamelJolokiaRestrictor restrictor = new CamelJolokiaRestrictor();
        assertTrue(restrictor.isRemoteAccessAllowed("127.0.0.1"));
    }

    @Test
    void ipv6LoopbackAllowed() {
        CamelJolokiaRestrictor restrictor = new CamelJolokiaRestrictor();
        assertTrue(restrictor.isRemoteAccessAllowed("::1"));
    }

    @Test
    void nonLoopbackRejectedByDefault() {
        CamelJolokiaRestrictor restrictor = new CamelJolokiaRestrictor();
        assertFalse(restrictor.isRemoteAccessAllowed("192.168.1.1"));
    }

    @Test
    void allInterfacesRejectedByDefault() {
        CamelJolokiaRestrictor restrictor = new CamelJolokiaRestrictor();
        assertFalse(restrictor.isRemoteAccessAllowed("0.0.0.0"));
    }

    @Test
    void privateNetworkRejectedByDefault() {
        CamelJolokiaRestrictor restrictor = new CamelJolokiaRestrictor();
        assertFalse(restrictor.isRemoteAccessAllowed("10.0.0.1"));
    }

    @Test
    void loopbackOnlyAddressChainAllowed() {
        CamelJolokiaRestrictor restrictor = new CamelJolokiaRestrictor();
        assertTrue(restrictor.isRemoteAccessAllowed("localhost", "127.0.0.1"));
    }

    @Test
    void spoofedLoopbackInAddressChainRejected() {
        CamelJolokiaRestrictor restrictor = new CamelJolokiaRestrictor();
        assertFalse(restrictor.isRemoteAccessAllowed("127.0.0.1", "192.168.1.1"));
        assertFalse(restrictor.isRemoteAccessAllowed("192.168.1.1", "127.0.0.1"));
        assertFalse(restrictor.isRemoteAccessAllowed("localhost", "192.168.1.1"));
        assertFalse(restrictor.isRemoteAccessAllowed("192.168.1.1", "::1"));
    }

    @Test
    void ipv6LoopbackLiteralsAllowed() {
        CamelJolokiaRestrictor restrictor = new CamelJolokiaRestrictor();
        assertTrue(restrictor.isRemoteAccessAllowed("::1"));
        assertTrue(restrictor.isRemoteAccessAllowed("0:0:0:0:0:0:0:1"));
        assertTrue(restrictor.isRemoteAccessAllowed("::ffff:127.0.0.1"));
        assertTrue(restrictor.isRemoteAccessAllowed("::1%lo0"));
        assertTrue(restrictor.isRemoteAccessAllowed("::1%lo"));
    }

    /**
     * A colon does not make a value an IPv6 literal. InetAddress.getByName falls back to the name service for
     * anything it cannot read as a literal, so a client supplied host name carrying a colon must be rejected
     * without a lookup rather than being handed to the resolver.
     */
    @Test
    void hostNamesCarryingAColonRejectedWithoutResolving() {
        CamelJolokiaRestrictor restrictor = new CamelJolokiaRestrictor();
        assertFalse(restrictor.isRemoteAccessAllowed("untrusted.example:1"));
        assertFalse(restrictor.isRemoteAccessAllowed("localhost:80"));
        assertFalse(restrictor.isRemoteAccessAllowed("::ffff:8.8.8.8"));
        assertFalse(restrictor.isRemoteAccessAllowed(".::1"));
        assertFalse(restrictor.isRemoteAccessAllowed(".0:0:0:0:0:0:0:1"));
    }

    @Test
    void nullArgsRejected() {
        CamelJolokiaRestrictor restrictor = new CamelJolokiaRestrictor();
        assertFalse(restrictor.isRemoteAccessAllowed((String[]) null));
    }

    @Test
    void emptyArgsRejected() {
        CamelJolokiaRestrictor restrictor = new CamelJolokiaRestrictor();
        assertFalse(restrictor.isRemoteAccessAllowed());
    }

    @Test
    void remoteOriginRejectedByDefault() {
        CamelJolokiaRestrictor restrictor = new CamelJolokiaRestrictor();
        assertFalse(restrictor.isOriginAllowed("http://untrusted.example", false));
        assertFalse(restrictor.isOriginAllowed("http://192.168.1.1:8080", false));
    }

    @Test
    void loopbackOriginAllowed() {
        CamelJolokiaRestrictor restrictor = new CamelJolokiaRestrictor();
        assertTrue(restrictor.isOriginAllowed("http://localhost:8080", false));
        assertTrue(restrictor.isOriginAllowed("http://127.0.0.1:9090", false));
    }

    @Test
    void noOriginHeaderAllowedWithoutStrictCheck() {
        CamelJolokiaRestrictor restrictor = new CamelJolokiaRestrictor();
        assertTrue(restrictor.isOriginAllowed(null, false));
    }

    @Test
    void noOriginHeaderAllowedWithStrictCheck() {
        CamelJolokiaRestrictor restrictor = new CamelJolokiaRestrictor();
        assertTrue(restrictor.isOriginAllowed(null, true));
    }

    @Test
    void loopbackOriginAllowedWithStrictCheck() {
        CamelJolokiaRestrictor restrictor = new CamelJolokiaRestrictor();
        assertTrue(restrictor.isOriginAllowed("http://localhost:8080", true));
        assertTrue(restrictor.isOriginAllowed("http://127.0.0.1:9090", true));
    }

    @Test
    void remoteOriginRejectedWithStrictCheck() {
        CamelJolokiaRestrictor restrictor = new CamelJolokiaRestrictor();
        assertFalse(restrictor.isOriginAllowed("http://untrusted.example", true));
    }

    @Test
    void originWithUserInfoRejected() {
        CamelJolokiaRestrictor restrictor = new CamelJolokiaRestrictor();
        assertFalse(restrictor.isOriginAllowed("http://untrusted.example@localhost", true));
        assertFalse(restrictor.isOriginAllowed("http://untrusted.example@127.0.0.1", true));
    }

    @Test
    void malformedOriginRejected() {
        CamelJolokiaRestrictor restrictor = new CamelJolokiaRestrictor();
        assertFalse(restrictor.isOriginAllowed("not a valid uri", false));
    }

    @Test
    void httpGetAllowed() {
        CamelJolokiaRestrictor restrictor = new CamelJolokiaRestrictor();
        assertTrue(restrictor.isHttpMethodAllowed(HttpMethod.GET));
    }

    @Test
    void httpPostAllowed() {
        CamelJolokiaRestrictor restrictor = new CamelJolokiaRestrictor();
        assertTrue(restrictor.isHttpMethodAllowed(HttpMethod.POST));
    }

    @Test
    void requestTypesAllowed() {
        CamelJolokiaRestrictor restrictor = new CamelJolokiaRestrictor();
        assertTrue(restrictor.isTypeAllowed(RequestType.READ));
        assertTrue(restrictor.isTypeAllowed(RequestType.EXEC));
        assertTrue(restrictor.isTypeAllowed(RequestType.LIST));
        assertTrue(restrictor.isTypeAllowed(RequestType.SEARCH));
        assertTrue(restrictor.isTypeAllowed(RequestType.VERSION));
    }

    @Test
    void allowedDomainReadWriteExecAllowed() throws MalformedObjectNameException {
        CamelJolokiaRestrictor restrictor = new CamelJolokiaRestrictor();
        ObjectName camelMBean = new ObjectName("org.apache.camel:context=camel-1,type=context,name=\"camel-1\"");
        assertTrue(restrictor.isAttributeReadAllowed(camelMBean, "Uptime"));
        assertTrue(restrictor.isAttributeWriteAllowed(camelMBean, "Tracing"));
        assertTrue(restrictor.isOperationAllowed(camelMBean, "sendStringBody"));
    }

    @Test
    void disallowedDomainReadWriteExecDenied() throws MalformedObjectNameException {
        CamelJolokiaRestrictor restrictor = new CamelJolokiaRestrictor();
        ObjectName disallowed = new ObjectName("com.example:type=Test");
        assertFalse(restrictor.isAttributeReadAllowed(disallowed, "Value"));
        assertFalse(restrictor.isAttributeWriteAllowed(disallowed, "Value"));
        assertFalse(restrictor.isOperationAllowed(disallowed, "doSomething"));
    }

    @Test
    void allowedDomainNotHidden() throws MalformedObjectNameException {
        CamelJolokiaRestrictor restrictor = new CamelJolokiaRestrictor();
        assertFalse(restrictor.isObjectNameHidden(
                new ObjectName("org.apache.camel:context=camel-1,type=context,name=\"camel-1\"")));
        assertFalse(restrictor.isObjectNameHidden(new ObjectName("java.lang:type=Runtime")));
        assertFalse(restrictor.isObjectNameHidden(new ObjectName("java.nio:type=BufferPool,name=direct")));
    }

    @Test
    void disallowedDomainIsHidden() throws MalformedObjectNameException {
        CamelJolokiaRestrictor restrictor = new CamelJolokiaRestrictor();
        assertTrue(restrictor.isObjectNameHidden(new ObjectName("com.example:type=Test")));
        assertTrue(restrictor.isObjectNameHidden(new ObjectName("java.util.logging:type=Logging")));
    }
}
