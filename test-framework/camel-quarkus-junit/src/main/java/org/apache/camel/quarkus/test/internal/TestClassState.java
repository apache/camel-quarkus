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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import io.quarkus.arc.Subclass;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.quarkus.test.CamelQuarkusTest;
import org.apache.camel.util.StopWatch;
import org.junit.jupiter.api.Nested;

/**
 * The state of a {@link CamelQuarkusTest} class while its tests run.
 */
final class TestClassState {
    private static final Map<String, TestClassState> STATES = new ConcurrentHashMap<>();
    private static volatile TestClassState running;

    final Class<?> testClass;
    final CamelQuarkusTest annotation;

    /** Whether the parts of the plan that need to run before routes are created were applied by the lifecycle hook */
    boolean isolated;
    boolean classPlanApplied;
    RuntimeException classPlanFailure;
    RouteDefinitionSnapshot classSnapshot;
    Set<String> existingComponents;
    final List<RouteDefinition> addedRoutes = new ArrayList<>();
    final Map<String, Optional<Object>> replacedRegistryBeans = new LinkedHashMap<>();
    Long previousShutdownTimeout;
    boolean autoStartupExcludeReplaced;
    String previousAutoStartupExclude;

    RouteDefinitionSnapshot methodSnapshot;
    /** The test instance and its enclosing instances, while a test method runs */
    List<Object> currentInstances = List.of();
    String currentTestName;
    final StopWatch watch = new StopWatch();

    private TestClassState(Class<?> testClass) {
        this.testClass = testClass;
        this.annotation = testClass.getAnnotation(CamelQuarkusTest.class);
    }

    static TestClassState get(Class<?> testClass) {
        return STATES.computeIfAbsent(testClass.getName(), name -> new TestClassState(testClass));
    }

    /**
     * @return the state of the test class whose test method is running, if any
     */
    static Optional<TestClassState> running() {
        return Optional.ofNullable(running);
    }

    void testStarted(List<Object> instances) {
        currentInstances = instances;
        running = this;
    }

    void testFinished() {
        currentInstances = List.of();
        if (running == this) {
            running = null;
        }
    }

    static TestClassState remove(Class<?> testClass) {
        TestClassState state = STATES.remove(testClass.getName());
        if (state != null) {
            state.testFinished();
        }
        return state;
    }

    /**
     * Finds the class that owns the {@link CamelQuarkusTest} plan for the given test class: the outermost class
     * carrying the annotation, walking from {@link Nested} classes to their enclosing classes.
     *
     * @param  testClass the test class
     * @return           the owner class or {@code null} if the class does not use {@link CamelQuarkusTest}
     */
    static Class<?> findPlanOwner(Class<?> testClass) {
        Class<?> owner = null;
        Class<?> current = beanClass(testClass);
        while (current != null) {
            if (current.isAnnotationPresent(CamelQuarkusTest.class)) {
                owner = current;
            }
            current = current.isAnnotationPresent(Nested.class) ? current.getEnclosingClass() : null;
        }
        return owner;
    }

    /**
     * @param  testClass the class of a test instance
     * @return           the test class itself, if the given class is the subclass generated for an intercepted bean
     */
    static Class<?> beanClass(Class<?> testClass) {
        Class<?> current = testClass;
        while (Subclass.class.isAssignableFrom(current)) {
            current = current.getSuperclass();
        }
        return current;
    }
}
