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

class CamelJolokiaRestrictorRemoteAccessEnabledTest {

    @RegisterExtension
    static final QuarkusExtensionTest CONFIG = new QuarkusExtensionTest()
            .withEmptyApplication()
            .overrideConfigKey("quarkus.camel.jolokia.remote-access-allowed", "true");

    @Test
    void allRemoteAddressesAllowed() {
        CamelJolokiaRestrictor restrictor = new CamelJolokiaRestrictor();
        assertTrue(restrictor.isRemoteAccessAllowed("192.168.1.1"));
        assertTrue(restrictor.isRemoteAccessAllowed("10.0.0.1"));
        assertTrue(restrictor.isRemoteAccessAllowed("172.16.0.1"));
    }

    @Test
    void loopbackStillAllowed() {
        CamelJolokiaRestrictor restrictor = new CamelJolokiaRestrictor();
        assertTrue(restrictor.isRemoteAccessAllowed("127.0.0.1"));
        assertTrue(restrictor.isRemoteAccessAllowed("::1"));
    }

    /**
     * Remote access is a control over client addresses. Lifting it must not also stop a browser on another site
     * from driving the agent, which is a separate question answered by `allowed-origins`.
     */
    @Test
    void originsStillRestricted() {
        CamelJolokiaRestrictor restrictor = new CamelJolokiaRestrictor();
        assertFalse(restrictor.isOriginAllowed("http://untrusted.example", false));
        assertFalse(restrictor.isOriginAllowed("http://192.168.1.1:8080", false));
        assertFalse(restrictor.isOriginAllowed("http://example.com", true));
    }

    @Test
    void loopbackOriginsStillAllowed() {
        CamelJolokiaRestrictor restrictor = new CamelJolokiaRestrictor();
        assertTrue(restrictor.isOriginAllowed("http://localhost:8080", true));
        assertTrue(restrictor.isOriginAllowed("http://127.0.0.1:9090", true));
    }

    @Test
    void nullOriginAllowed() {
        CamelJolokiaRestrictor restrictor = new CamelJolokiaRestrictor();
        assertTrue(restrictor.isOriginAllowed(null, false));
        assertTrue(restrictor.isOriginAllowed(null, true));
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
