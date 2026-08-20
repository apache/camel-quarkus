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

import org.apache.camel.quarkus.jolokia.restrictor.CamelJolokiaRestrictor;
import org.jolokia.server.core.service.api.Restrictor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Camel restrictor implements Restrictor directly, so a method added to that interface with a permissive
 * default implementation would be inherited rather than delegated, and the restriction would silently not apply.
 * Nothing in the language prevents that, so it is asserted here. A failure after a Jolokia upgrade means the new
 * method has to be implemented, not that this test is wrong.
 */
class CamelJolokiaRestrictorInterfaceCoverageTest {

    @Test
    void everyRestrictorMethodIsImplemented() {
        List<String> missing = new ArrayList<>();
        for (Class<?> implementation : restrictorImplementations()) {
            for (Method method : Restrictor.class.getMethods()) {
                if (method.isSynthetic() || Modifier.isStatic(method.getModifiers())) {
                    continue;
                }
                if (!isImplementedBy(implementation, method)) {
                    missing.add(implementation.getSimpleName() + "." + method.getName());
                }
            }
        }
        assertTrue(missing.isEmpty(), "Restrictor methods not implemented, so the inherited default applies: " + missing);
    }

    /**
     * The Camel restrictor and the delegates it declares. The delegates are where the access decisions are made,
     * so they carry the same risk.
     */
    private static List<Class<?>> restrictorImplementations() {
        List<Class<?>> implementations = new ArrayList<>();
        implementations.add(CamelJolokiaRestrictor.class);
        for (Class<?> nested : CamelJolokiaRestrictor.class.getDeclaredClasses()) {
            if (Restrictor.class.isAssignableFrom(nested)) {
                implementations.add(nested);
            }
        }
        return implementations;
    }

    private static boolean isImplementedBy(Class<?> implementation, Method method) {
        for (Class<?> type = implementation; type != null && type != Object.class; type = type.getSuperclass()) {
            try {
                type.getDeclaredMethod(method.getName(), method.getParameterTypes());
                return true;
            } catch (NoSuchMethodException e) {
                // Declared further up the hierarchy, or not at all
            }
        }
        return false;
    }
}
