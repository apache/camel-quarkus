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

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import org.apache.camel.component.mock.MockEndpoint;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExecutableInvoker;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestInstances;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.engine.execution.ExtensionValuesStore;
import org.junit.jupiter.engine.execution.NamespaceAwareStore;

public class CallbackUtil {

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
        if (testInstance.isUseRouteBuilder() && testInstance.createdRoutes != null) {

            try {
                for (String r : testInstance.createdRoutes) {
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

    static class MockExtensionContext implements ExtensionContext {

        private final Optional<TestInstance.Lifecycle> lifecycle;
        private final String currentTestName;
        private final ExtensionContext.Store globalStore;

        public MockExtensionContext(Optional<TestInstance.Lifecycle> lifecycle, String currentTestName) {
            this.lifecycle = lifecycle;
            this.currentTestName = currentTestName;
            this.globalStore = new NamespaceAwareStore(new ExtensionValuesStore(null), ExtensionContext.Namespace.GLOBAL);
        }

        @Override
        public Optional<ExtensionContext> getParent() {
            return Optional.empty();
        }

        @Override
        public ExtensionContext getRoot() {
            return null;
        }

        @Override
        public String getUniqueId() {
            return null;
        }

        @Override
        public String getDisplayName() {
            return currentTestName;
        }

        @Override
        public Set<String> getTags() {
            return null;
        }

        @Override
        public Optional<AnnotatedElement> getElement() {
            return Optional.empty();
        }

        @Override
        public Optional<Class<?>> getTestClass() {
            return Optional.empty();
        }

        @Override
        public Optional<TestInstance.Lifecycle> getTestInstanceLifecycle() {
            return lifecycle;
        }

        @Override
        public Optional<Object> getTestInstance() {
            return Optional.empty();
        }

        @Override
        public Optional<TestInstances> getTestInstances() {
            return Optional.empty();
        }

        @Override
        public Optional<Method> getTestMethod() {
            return Optional.empty();
        }

        @Override
        public Optional<Throwable> getExecutionException() {
            return Optional.empty();
        }

        @Override
        public Optional<String> getConfigurationParameter(String key) {
            return Optional.empty();
        }

        @Override
        public <T> Optional<T> getConfigurationParameter(String key, Function<String, T> transformer) {
            return Optional.empty();
        }

        @Override
        public void publishReportEntry(Map<String, String> map) {

        }

        @Override
        public Store getStore(Namespace namespace) {
            if (namespace == Namespace.GLOBAL) {
                return globalStore;
            }
            return null;
        }

        @Override
        public ExecutionMode getExecutionMode() {
            return null;
        }

        @Override
        public ExecutableInvoker getExecutableInvoker() {
            return null;
        }
    }
}
