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
 * Accepting any origin is asked for explicitly rather than arrived at by leaving `allowed-origins` unset, so
 * that the configuration says what it does.
 */
class CamelJolokiaRestrictorAnyOriginTest {

    @RegisterExtension
    static final QuarkusExtensionTest CONFIG = new QuarkusExtensionTest()
            .withEmptyApplication()
            .overrideConfigKey("quarkus.camel.jolokia.remote-access-allowed", "true")
            .overrideConfigKey("quarkus.camel.jolokia.allowed-origins", "*");

    @Test
    void anyOriginAllowed() {
        CamelJolokiaRestrictor restrictor = new CamelJolokiaRestrictor();
        assertTrue(restrictor.isOriginAllowed("https://untrusted.example", true));
        assertTrue(restrictor.isOriginAllowed("http://192.168.1.1:8080", true));
        assertTrue(restrictor.isOriginAllowed("https://anything.example.com/with/a/path", true));
        assertTrue(restrictor.isOriginAllowed(null, true));
    }

    /**
     * Still an origin check, so a value that is not an origin is refused even here.
     */
    @Test
    void valuesThatAreNotOriginsStillDenied() {
        CamelJolokiaRestrictor restrictor = new CamelJolokiaRestrictor();
        assertFalse(restrictor.isOriginAllowed("null", true));
        assertFalse(restrictor.isOriginAllowed("https://untrusted.example@localhost", true));
    }

    @Test
    void mbeanDomainFilteringStillEnforced() throws MalformedObjectNameException {
        CamelJolokiaRestrictor restrictor = new CamelJolokiaRestrictor();
        assertFalse(restrictor.isOperationAllowed(new ObjectName("com.example:type=Test"), "doSomething"));
    }
}
