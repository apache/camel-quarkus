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
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jolokia.server.core.util.HttpMethod;
import org.jolokia.server.core.util.RequestType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CamelJolokiaRestrictorPolicyDelegationTest {

    private static final String JOLOKIA_ACCESS_XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <restrict>
                <remote>
                    <host>10.0.0.0/8</host>
                </remote>
                <cors>
                    <allow-origin>http://example.host.com</allow-origin>
                </cors>
                <commands>
                    <command>list</command>
                    <command>version</command>
                    <command>search</command>
                </commands>
                <http>
                    <method>get</method>
                </http>
            </restrict>
            """;

    @RegisterExtension
    static final QuarkusUnitTest CONFIG = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addAsResource(new StringAsset(JOLOKIA_ACCESS_XML), "jolokia-access.xml"));

    /**
     * The policy restricts addresses to 10.0.0.0/8, which loopback is not in. The framework must not widen a
     * restriction the operator asked for, so loopback is refused like any other address outside the subnet.
     */
    @Test
    void loopbackDeniedByPolicySubnet() {
        CamelJolokiaRestrictor restrictor = new CamelJolokiaRestrictor();
        assertFalse(restrictor.isRemoteAccessAllowed("127.0.0.1"));
        assertFalse(restrictor.isRemoteAccessAllowed("::1"));
    }

    @Test
    void policyAllowedSubnetAllowed() {
        CamelJolokiaRestrictor restrictor = new CamelJolokiaRestrictor();
        assertTrue(restrictor.isRemoteAccessAllowed("10.0.0.1"));
        assertTrue(restrictor.isRemoteAccessAllowed("10.255.255.255"));
    }

    @Test
    void nonPolicyRemoteStillDenied() {
        CamelJolokiaRestrictor restrictor = new CamelJolokiaRestrictor();
        assertFalse(restrictor.isRemoteAccessAllowed("192.168.1.1"));
        assertFalse(restrictor.isRemoteAccessAllowed("172.16.0.1"));
    }

    /**
     * The cors section of the policy is not consulted, so listing an origin there does not grant it access. It
     * has to be listed in quarkus.camel.jolokia.allowed-origins.
     */
    @Test
    void policyAllowOriginDoesNotGrantAccess() {
        CamelJolokiaRestrictor restrictor = new CamelJolokiaRestrictor();
        assertFalse(restrictor.isOriginAllowed("http://example.host.com", false));
        assertFalse(restrictor.isOriginAllowed("http://example.host.com", true));
    }

    @Test
    void nonPolicyCorsOriginDenied() {
        CamelJolokiaRestrictor restrictor = new CamelJolokiaRestrictor();
        assertFalse(restrictor.isOriginAllowed("http://untrusted.example", false));
    }

    @Test
    void nonPolicyCorsOriginDeniedWithStrictCheck() {
        CamelJolokiaRestrictor restrictor = new CamelJolokiaRestrictor();
        assertFalse(restrictor.isOriginAllowed("http://untrusted.example", true));
    }

    @Test
    void userInfoOriginCannotBypassPolicyCors() {
        CamelJolokiaRestrictor restrictor = new CamelJolokiaRestrictor();
        assertFalse(restrictor.isOriginAllowed("http://untrusted.example@localhost", true));
    }

    @Test
    void spoofedLoopbackInAddressChainDeniedByPolicy() {
        CamelJolokiaRestrictor restrictor = new CamelJolokiaRestrictor();
        assertFalse(restrictor.isRemoteAccessAllowed("127.0.0.1", "192.168.1.1"));
        assertTrue(restrictor.isRemoteAccessAllowed("10.0.0.1", "10.0.0.2"));
    }

    @Test
    void nullOriginStillAllowed() {
        CamelJolokiaRestrictor restrictor = new CamelJolokiaRestrictor();
        assertTrue(restrictor.isOriginAllowed(null, false));
        assertTrue(restrictor.isOriginAllowed(null, true));
    }

    @Test
    void loopbackOriginStillAllowed() {
        CamelJolokiaRestrictor restrictor = new CamelJolokiaRestrictor();
        assertTrue(restrictor.isOriginAllowed("http://localhost:8080", true));
        assertTrue(restrictor.isOriginAllowed("http://localhost:8080", false));
        assertTrue(restrictor.isOriginAllowed("http://127.0.0.1:9090", true));
    }

    @Test
    void policyAllowedRequestTypesAllowed() {
        CamelJolokiaRestrictor restrictor = new CamelJolokiaRestrictor();
        assertTrue(restrictor.isTypeAllowed(RequestType.LIST));
        assertTrue(restrictor.isTypeAllowed(RequestType.VERSION));
        assertTrue(restrictor.isTypeAllowed(RequestType.SEARCH));
    }

    @Test
    void policyDeniedRequestTypesDenied() {
        CamelJolokiaRestrictor restrictor = new CamelJolokiaRestrictor();
        assertFalse(restrictor.isTypeAllowed(RequestType.READ));
        assertFalse(restrictor.isTypeAllowed(RequestType.WRITE));
        assertFalse(restrictor.isTypeAllowed(RequestType.EXEC));
    }

    @Test
    void policyDeniedReadBlocksAttributeRead() throws MalformedObjectNameException {
        CamelJolokiaRestrictor restrictor = new CamelJolokiaRestrictor();
        ObjectName camelMBean = new ObjectName("org.apache.camel:context=camel-1,type=context,name=\"camel-1\"");
        assertFalse(restrictor.isAttributeReadAllowed(camelMBean, "Uptime"));
    }

    @Test
    void policyDeniedWriteBlocksAttributeWrite() throws MalformedObjectNameException {
        CamelJolokiaRestrictor restrictor = new CamelJolokiaRestrictor();
        ObjectName camelMBean = new ObjectName("org.apache.camel:context=camel-1,type=context,name=\"camel-1\"");
        assertFalse(restrictor.isAttributeWriteAllowed(camelMBean, "Tracing"));
    }

    @Test
    void policyDeniedExecBlocksOperation() throws MalformedObjectNameException {
        CamelJolokiaRestrictor restrictor = new CamelJolokiaRestrictor();
        ObjectName camelMBean = new ObjectName("org.apache.camel:context=camel-1,type=context,name=\"camel-1\"");
        assertFalse(restrictor.isOperationAllowed(camelMBean, "getUptime"));
    }

    @Test
    void policyAllowedHttpMethodAllowed() {
        CamelJolokiaRestrictor restrictor = new CamelJolokiaRestrictor();
        assertTrue(restrictor.isHttpMethodAllowed(HttpMethod.GET));
    }

    @Test
    void policyDeniedHttpMethodDenied() {
        CamelJolokiaRestrictor restrictor = new CamelJolokiaRestrictor();
        assertFalse(restrictor.isHttpMethodAllowed(HttpMethod.POST));
    }
}
