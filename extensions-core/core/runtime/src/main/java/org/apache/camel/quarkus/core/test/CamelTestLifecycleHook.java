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
package org.apache.camel.quarkus.core.test;

import org.apache.camel.CamelContext;

/**
 * A hook into the Camel Main lifecycle of a test application, invoked before any route is created.
 * <p>
 * Implementations are discovered with {@link java.util.ServiceLoader} from
 * {@code META-INF/services/org.apache.camel.quarkus.core.test.CamelTestLifecycleHook}. Hooks are only invoked for
 * applications built for tests, never in production or native mode.
 */
public interface CamelTestLifecycleHook {
    /**
     * Invoked from {@code MainListener.beforeConfigure}: the {@link CamelContext} exists and no routes have been added.
     *
     * @param context the {@link CamelContext}
     * @param info    what the build knew about the test application
     */
    default void beforeConfigure(CamelContext context, TestApplicationInfo info) {
    }

    /**
     * Invoked from {@code MainListener.afterConfigure}: route definitions have been added but no route has been
     * created.
     *
     * @param context the {@link CamelContext}
     * @param info    what the build knew about the test application
     */
    default void afterConfigure(CamelContext context, TestApplicationInfo info) {
    }

    /**
     * Invoked from {@code MainListener.beforeStart}: the last chance to act before routes are created and started.
     *
     * @param context the {@link CamelContext}
     * @param info    what the build knew about the test application
     */
    default void beforeStart(CamelContext context, TestApplicationInfo info) {
    }
}
