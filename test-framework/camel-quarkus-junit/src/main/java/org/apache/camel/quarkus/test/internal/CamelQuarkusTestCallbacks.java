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
package org.apache.camel.quarkus.test.internal;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import io.quarkus.arc.Arc;
import io.quarkus.test.junit.callback.QuarkusTestAfterAllCallback;
import io.quarkus.test.junit.callback.QuarkusTestAfterConstructCallback;
import io.quarkus.test.junit.callback.QuarkusTestAfterEachCallback;
import io.quarkus.test.junit.callback.QuarkusTestBeforeEachCallback;
import io.quarkus.test.junit.callback.QuarkusTestContext;
import io.quarkus.test.junit.callback.QuarkusTestMethodContext;
import org.apache.camel.CamelContext;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.quarkus.test.CamelQuarkusTest;
import org.apache.camel.quarkus.test.CamelTestHelper;
import org.apache.camel.test.junit6.util.ExtensionHelper;
import org.apache.camel.test.junit6.util.RouteCoverageDumperExtension;
import org.apache.camel.test.junit6.util.RouteDumperExtension;

/**
 * Applies the {@link CamelQuarkusTest} plan around the test classes and methods that use it.
 */
public class CamelQuarkusTestCallbacks implements QuarkusTestAfterConstructCallback, QuarkusTestBeforeEachCallback,
        QuarkusTestAfterEachCallback, QuarkusTestAfterAllCallback {

    @Override
    public void afterConstruct(Object testInstance) {
        Class<?> owner = findPlanOwner(testInstance.getClass());
        if (owner == null) {
            return;
        }

        CamelContext context = camelContext();
        TestClassState state = TestClassState.get(owner);
        if (state.classPlanFailure != null) {
            // The plan is applied once per class, so every test of the class fails for the same reason
            throw state.classPlanFailure;
        }
        boolean applyClassPlan = !state.classPlanApplied && !state.isolated;
        if (!state.classPlanApplied) {
            state.classPlanApplied = true;
            state.existingComponents = new HashSet<>(context.getComponentNames());
            if (!state.isolated) {
                state.classSnapshot = RouteDefinitionSnapshot.take(context);
                state.previousShutdownTimeout = context.getShutdownStrategy().getTimeout();
                context.getShutdownStrategy().setTimeout(state.annotation.shutdownTimeout());
            }
        }
        // Bound before the routes of the test class are added, so that they can refer to the beans
        TestPlanSupport.replaceInRegistry(context, state, testInstance);
        if (applyClassPlan) {
            try {
                TestPlanSupport.unchecked(() -> TestPlanSupport.applyRoutesAndAdvice(context, state));
            } catch (RuntimeException e) {
                state.classPlanFailure = e;
                try {
                    TestPlanSupport.revert(context, state);
                } catch (Exception revertFailure) {
                    e.addSuppressed(revertFailure);
                }
                throw e;
            }
        }
    }

    @Override
    public void beforeEach(QuarkusTestMethodContext context) {
        Class<?> testClass = TestClassState.beanClass(context.getTestInstance().getClass());
        Class<?> owner = findPlanOwner(testClass);
        if (owner == null) {
            return;
        }

        TestClassState state = TestClassState.get(owner);
        try {
            camelTestHelper().startRouteDefinitions();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        List<Object> instances = new ArrayList<>(context.getOuterInstances());
        instances.add(context.getTestInstance());
        state.testStarted(instances);
        EndpointInjections.refresh(camelContext(), instances);
        state.methodSnapshot = RouteDefinitionSnapshot.take(camelContext());
        state.currentTestName = displayName(context.getTestMethod());
        ExtensionHelper.testStartHeader(testClass, state.currentTestName);
        state.watch.restart();
    }

    @Override
    public void afterEach(QuarkusTestMethodContext context) {
        Class<?> testClass = TestClassState.beanClass(context.getTestInstance().getClass());
        Class<?> owner = findPlanOwner(testClass);
        if (owner == null) {
            return;
        }

        TestClassState state = TestClassState.get(owner);
        CamelContext camelContext = camelContext();
        long taken = state.watch.taken();
        try {
            if (state.annotation.dumpRouteCoverage()) {
                ExtensionHelper.testEndFooter(testClass, state.currentTestName, taken,
                        new RouteCoverageDumperExtension((ModelCamelContext) camelContext));
            } else {
                ExtensionHelper.testEndFooter(testClass, state.currentTestName, taken);
            }
            if (!state.annotation.dumpRoute().isEmpty()) {
                new RouteDumperExtension((ModelCamelContext) camelContext).dumpRoute(testClass, state.currentTestName,
                        state.annotation.dumpRoute());
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        } finally {
            if (state.methodSnapshot != null) {
                state.methodSnapshot.restoreAndReload(camelContext);
                state.methodSnapshot = null;
            }
            MockEndpoint.resetMocks(camelContext);
            state.testFinished();
        }
    }

    /**
     * Re-resolves the injected endpoints of the running test, after its routes were changed.
     */
    public static void routesChanged(CamelContext context) {
        TestClassState.running().ifPresent(state -> EndpointInjections.refresh(context, state.currentInstances));
    }

    @Override
    public void afterAll(QuarkusTestContext context) {
        Object testInstance = context.getTestInstance();
        if (testInstance == null) {
            return;
        }
        Class<?> testClass = TestClassState.beanClass(testInstance.getClass());
        if (findPlanOwner(testClass) != testClass) {
            // Not a CamelQuarkusTest or a @Nested class whose plan is owned by an enclosing class
            return;
        }

        TestClassState state = TestClassState.remove(testClass);
        if (state != null && state.classPlanFailure == null) {
            TestPlanSupport.unchecked(() -> TestPlanSupport.revert(camelContext(), state));
        }
    }

    private static Class<?> findPlanOwner(Class<?> testClass) {
        if (Arc.container() == null) {
            // The application does not run in the JVM of the test, which is the case for @QuarkusIntegrationTest
            return null;
        }
        return TestClassState.findPlanOwner(testClass);
    }

    private static CamelContext camelContext() {
        return Arc.container().instance(CamelContext.class).get();
    }

    private static CamelTestHelper camelTestHelper() {
        return Arc.container().instance(CamelTestHelper.class).get();
    }

    private static String displayName(Method method) {
        return String.format("%s(%s)",
                method.getName(),
                Arrays.stream(method.getParameterTypes()).map(Class::getSimpleName).collect(Collectors.joining(", ")));
    }
}
