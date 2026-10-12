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

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

import org.apache.camel.CamelContext;
import org.apache.camel.quarkus.core.test.CamelTestLifecycleHook;
import org.apache.camel.quarkus.core.test.TestApplicationInfo;
import org.apache.camel.quarkus.test.CamelQuarkusTest;

/**
 * Applies the parts of the {@link CamelQuarkusTest} plan that must happen before routes are created, for the test
 * class that owns the application (isolated mode).
 */
public class CamelQuarkusTestLifecycleHook implements CamelTestLifecycleHook {
    /** Holds the name of the test class that owns the application, generated at build time */
    public static final String ISOLATED_TEST_CLASS_RESOURCE = "META-INF/camel-quarkus-junit/isolated-test-class";

    private TestClassState state;

    @Override
    public void beforeConfigure(CamelContext context, TestApplicationInfo info) {
        if (info.testProfileClassName().isEmpty()) {
            return;
        }
        Class<?> testClass = isolatedTestClass();
        if (testClass == null) {
            return;
        }

        state = TestClassState.get(testClass);
        state.isolated = true;
        TestPlanSupport.unchecked(() -> TestPlanSupport.enableMocksAndStubs(context, state.annotation));
        TestPlanSupport.enableDebugger(context, testClass);
    }

    @Override
    public void afterConfigure(CamelContext context, TestApplicationInfo info) {
        if (state == null) {
            return;
        }
        state.classSnapshot = RouteDefinitionSnapshot.take(context);
        TestPlanSupport.unchecked(() -> TestPlanSupport.applyRoutesAndAdvice(context, state));
    }

    @Override
    public void beforeStart(CamelContext context, TestApplicationInfo info) {
        if (state == null) {
            return;
        }
        if (!state.annotation.autoStartupExclude().isEmpty()) {
            state.autoStartupExcludeReplaced = true;
            state.previousAutoStartupExclude = context.getAutoStartupExcludePattern();
            context.setAutoStartupExcludePattern(state.annotation.autoStartupExclude());
        }
        // Not restored after the last test method, so that it also applies when the application shuts down
        context.getShutdownStrategy().setTimeout(state.annotation.shutdownTimeout());
    }

    private static Class<?> isolatedTestClass() {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        try (InputStream in = classLoader.getResourceAsStream(ISOLATED_TEST_CLASS_RESOURCE)) {
            if (in == null) {
                return null;
            }
            String className = new String(in.readAllBytes(), StandardCharsets.UTF_8).trim();
            return Class.forName(className, false, classLoader);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }
}
