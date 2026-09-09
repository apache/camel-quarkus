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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import io.quarkus.test.QuarkusUnitTest;
import org.apache.camel.quarkus.jolokia.restrictor.CamelJolokiaRestrictor;
import org.jolokia.server.core.service.api.Restrictor;
import org.jolokia.server.core.util.HttpMethod;
import org.jolokia.server.core.util.RequestType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A subclass of the Camel restrictor must be able to restrict further, and nothing else. The access checks are
 * final and a subclass contributes through the `allows*` hooks, so a custom restrictor cannot give up the
 * defaults by overriding a check and not calling `super`.
 */
class CamelJolokiaRestrictorSubclassNarrowsOnlyTest {
    @RegisterExtension
    static final QuarkusUnitTest CONFIG = new QuarkusUnitTest()
            .withEmptyApplication();

    /**
     * The invariant the hooks rest on. A check left non-final could be overridden to return true, which would
     * discard the MBean domain restriction and the whole delegate along with it.
     */
    @Test
    void everyAccessCheckIsFinal() {
        List<String> overridable = new ArrayList<>();
        for (Method method : Restrictor.class.getMethods()) {
            if (method.isSynthetic() || Modifier.isStatic(method.getModifiers())) {
                continue;
            }
            try {
                Method declared = CamelJolokiaRestrictor.class.getDeclaredMethod(method.getName(),
                        method.getParameterTypes());
                if (!Modifier.isFinal(declared.getModifiers())) {
                    overridable.add(declared.getName());
                }
            } catch (NoSuchMethodException e) {
                // Reported by CamelJolokiaRestrictorInterfaceCoverageTest
            }
        }
        assertTrue(overridable.isEmpty(),
                "Access checks a subclass could override and widen: " + overridable);
    }

    /**
     * Hooks that permit everything leave the inherited restrictions exactly as they were.
     */
    @Test
    void permissiveHooksCannotWiden() throws MalformedObjectNameException {
        CamelJolokiaRestrictor restrictor = new PermissiveSubclass();
        ObjectName foreignDomain = new ObjectName("com.example:type=Foreign");

        assertFalse(restrictor.isRemoteAccessAllowed("192.168.1.1"));
        assertFalse(restrictor.isOriginAllowed("http://untrusted.example", false));
        assertFalse(restrictor.isOperationAllowed(foreignDomain, "anything"));
        assertFalse(restrictor.isAttributeReadAllowed(foreignDomain, "anything"));
        assertFalse(restrictor.isAttributeWriteAllowed(foreignDomain, "anything"));
        assertTrue(restrictor.isObjectNameHidden(foreignDomain));
    }

    /**
     * Hooks that refuse take effect on top of what the inherited checks allowed.
     */
    @Test
    void restrictiveHooksNarrow() throws MalformedObjectNameException {
        CamelJolokiaRestrictor restrictor = new RestrictiveSubclass();
        ObjectName camelDomain = new ObjectName("org.apache.camel:type=context");

        assertFalse(restrictor.isRemoteAccessAllowed("127.0.0.1"));
        assertFalse(restrictor.isOriginAllowed("http://localhost:8080", false));
        assertFalse(restrictor.isHttpMethodAllowed(HttpMethod.GET));
        assertFalse(restrictor.isTypeAllowed(RequestType.READ));
        assertFalse(restrictor.isOperationAllowed(camelDomain, "dumpRoutesAsXml"));
        assertFalse(restrictor.isAttributeReadAllowed(camelDomain, "CamelId"));
        assertFalse(restrictor.isAttributeWriteAllowed(camelDomain, "CamelId"));
        assertTrue(restrictor.isObjectNameHidden(camelDomain));
    }

    /**
     * A subclass that adds nothing behaves exactly like the restrictor it extends.
     */
    @Test
    void defaultHooksChangeNothing() throws MalformedObjectNameException {
        CamelJolokiaRestrictor restrictor = new CamelJolokiaRestrictor() {
        };
        ObjectName camelDomain = new ObjectName("org.apache.camel:type=context");

        assertTrue(restrictor.isRemoteAccessAllowed("127.0.0.1"));
        assertTrue(restrictor.isOperationAllowed(camelDomain, "dumpRoutesAsXml"));
        assertFalse(restrictor.isObjectNameHidden(camelDomain));
    }

    static final class PermissiveSubclass extends CamelJolokiaRestrictor {
        @Override
        protected boolean allowsRemoteAccess(String... hostOrAddress) {
            return true;
        }

        @Override
        protected boolean allowsOrigin(String origin, boolean strictCheck) {
            return true;
        }

        @Override
        protected boolean allowsOperation(ObjectName objectName, String operation) {
            return true;
        }

        @Override
        protected boolean allowsAttributeRead(ObjectName objectName, String attribute) {
            return true;
        }

        @Override
        protected boolean allowsAttributeWrite(ObjectName objectName, String attribute) {
            return true;
        }

        @Override
        protected boolean allowsObjectName(ObjectName objectName) {
            return true;
        }
    }

    static final class RestrictiveSubclass extends CamelJolokiaRestrictor {
        @Override
        protected boolean allowsRemoteAccess(String... hostOrAddress) {
            return false;
        }

        @Override
        protected boolean allowsOrigin(String origin, boolean strictCheck) {
            return false;
        }

        @Override
        protected boolean allowsHttpMethod(HttpMethod method) {
            return false;
        }

        @Override
        protected boolean allowsRequestType(RequestType type) {
            return false;
        }

        @Override
        protected boolean allowsOperation(ObjectName objectName, String operation) {
            return false;
        }

        @Override
        protected boolean allowsAttributeRead(ObjectName objectName, String attribute) {
            return false;
        }

        @Override
        protected boolean allowsAttributeWrite(ObjectName objectName, String attribute) {
            return false;
        }

        @Override
        protected boolean allowsObjectName(ObjectName objectName) {
            return false;
        }
    }
}
