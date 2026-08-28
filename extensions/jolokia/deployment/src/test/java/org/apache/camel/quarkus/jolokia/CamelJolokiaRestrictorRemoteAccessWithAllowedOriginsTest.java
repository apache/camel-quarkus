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

import io.quarkus.test.QuarkusExtensionTest;
import org.apache.camel.quarkus.jolokia.restrictor.CamelJolokiaRestrictor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Remote access is a control over client addresses, so enabling it must not discard the origins an operator
 * configured. Without a listed origin the two would otherwise be set together and only one of them take effect.
 */
class CamelJolokiaRestrictorRemoteAccessWithAllowedOriginsTest {

    @RegisterExtension
    static final QuarkusExtensionTest CONFIG = new QuarkusExtensionTest()
            .withEmptyApplication()
            .overrideConfigKey("quarkus.camel.jolokia.remote-access-allowed", "true")
            .overrideConfigKey("quarkus.camel.jolokia.allowed-origins", "https://domain.example.com");

    @Test
    void remoteAddressesStillAllowed() {
        CamelJolokiaRestrictor restrictor = new CamelJolokiaRestrictor();
        assertTrue(restrictor.isRemoteAccessAllowed("192.168.1.1"));
        assertTrue(restrictor.isRemoteAccessAllowed("10.0.0.1"));
        assertTrue(restrictor.isRemoteAccessAllowed("127.0.0.1"));
        // Remote access is not conditional on the chain being loopback, so a proxied chain passes too
        assertTrue(restrictor.isRemoteAccessAllowed("10.0.0.1", "127.0.0.1"));
    }

    @Test
    void configuredOriginAllowed() {
        CamelJolokiaRestrictor restrictor = new CamelJolokiaRestrictor();
        assertTrue(restrictor.isOriginAllowed("https://domain.example.com", true));
        assertTrue(restrictor.isOriginAllowed("https://domain.example.com", false));
    }

    @Test
    void unlistedOriginDenied() {
        CamelJolokiaRestrictor restrictor = new CamelJolokiaRestrictor();
        assertFalse(restrictor.isOriginAllowed("https://untrusted.example", true));
        assertFalse(restrictor.isOriginAllowed("http://192.168.1.1:8080", true));
    }

    @Test
    void loopbackAndNullOriginsStillAllowed() {
        CamelJolokiaRestrictor restrictor = new CamelJolokiaRestrictor();
        assertTrue(restrictor.isOriginAllowed(null, true));
        assertTrue(restrictor.isOriginAllowed("http://localhost:8080", true));
    }

    @Test
    void mbeanDomainFilteringStillEnforced() throws MalformedObjectNameException {
        CamelJolokiaRestrictor restrictor = new CamelJolokiaRestrictor();
        ObjectName camelMBean = new ObjectName("org.apache.camel:context=camel-1,type=context,name=\"camel-1\"");
        assertTrue(restrictor.isOperationAllowed(camelMBean, "getUptime"));

        ObjectName disallowedMBean = new ObjectName("com.example:type=Test");
        assertFalse(restrictor.isOperationAllowed(disallowedMBean, "doSomething"));
    }
}
