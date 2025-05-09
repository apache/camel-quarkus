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
package org.apache.camel.quarkus.test;

import java.util.Optional;
import java.util.Set;

import org.apache.camel.component.mock.MockEndpoint;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.engine.execution.NamespaceAwareStore;
import org.junit.platform.engine.support.store.NamespacedHierarchicalStore;

public class CallbackUtil {
    private CallbackUtil() {
        // Utility class
    }

    static boolean isPerClass(CamelQuarkusTestSupport testSupport) {
        return getLifecycle(testSupport).filter(lc -> lc.equals(TestInstance.Lifecycle.PER_CLASS)).isPresent();
    }

    static Optional<TestInstance.Lifecycle> getLifecycle(CamelQuarkusTestSupport testSupport) {
        if (testSupport.getClass().getAnnotation(TestInstance.class) != null) {
            return Optional.of(testSupport.getClass().getAnnotation(TestInstance.class).value());
        }

        return Optional.empty();
    }

    static void resetContext(CamelQuarkusTestSupport testInstance) {
        //if routeBuilder (from the test) was used, all routes from that builder has to be stopped and removed
        //because routes will be created again (in case of TestInstance.Lifecycle.PER_CLASS, this method is not executed)
        Set<String> createdRoutes = testInstance.getCreatedRoutes();
        if (testInstance.isUseRouteBuilder() && createdRoutes != null) {

            try {
                for (String r : createdRoutes) {
                    testInstance.context().getRouteController().stopRoute(r);
                    testInstance.context().removeRoute(r);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        testInstance.context().getComponentNames().forEach(cn -> testInstance.context().removeComponent(cn));
        MockEndpoint.resetMocks(testInstance.context());
    }

    static class MockExtensionContext {
        private final Optional<TestInstance.Lifecycle> lifecycle;
        private final String currentTestName;
        private final ExtensionContext.Store globalStore;

        public MockExtensionContext(Optional<TestInstance.Lifecycle> lifecycle, String currentTestName) {
            this.lifecycle = lifecycle;
            this.currentTestName = currentTestName;
            this.globalStore = new NamespaceAwareStore(new NamespacedHierarchicalStore<>(null),
                    ExtensionContext.Namespace.GLOBAL);
        }

        public String getDisplayName() {
            return currentTestName;
        }

        public Optional<TestInstance.Lifecycle> getTestInstanceLifecycle() {
            return lifecycle;
        }

        public ExtensionContext.Store getStore(ExtensionContext.Namespace namespace) {
            if (namespace == ExtensionContext.Namespace.GLOBAL) {
                return globalStore;
            }
            return null;
        }
    }
}
