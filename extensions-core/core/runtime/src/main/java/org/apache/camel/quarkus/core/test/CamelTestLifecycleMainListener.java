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

import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;

import org.apache.camel.CamelContext;
import org.apache.camel.main.BaseMainSupport;
import org.apache.camel.main.MainListenerSupport;

/**
 * Delegates the Camel Main lifecycle events that precede route creation to every {@link CamelTestLifecycleHook}.
 */
public class CamelTestLifecycleMainListener extends MainListenerSupport {
    private final TestApplicationInfo info;
    private List<CamelTestLifecycleHook> hooks;

    public CamelTestLifecycleMainListener(String testProfileClassName) {
        this.info = new TestApplicationInfo(Optional.ofNullable(testProfileClassName).filter(name -> !name.isEmpty()));
    }

    @Override
    public void beforeConfigure(BaseMainSupport main) {
        invoke(main, CamelTestLifecycleHook::beforeConfigure);
    }

    @Override
    public void afterConfigure(BaseMainSupport main) {
        invoke(main, CamelTestLifecycleHook::afterConfigure);
    }

    @Override
    public void beforeStart(BaseMainSupport main) {
        invoke(main, CamelTestLifecycleHook::beforeStart);
    }

    private void invoke(BaseMainSupport main, TriConsumer action) {
        if (hooks == null) {
            hooks = ServiceLoader.load(CamelTestLifecycleHook.class, Thread.currentThread().getContextClassLoader())
                    .stream()
                    .map(ServiceLoader.Provider::get)
                    .toList();
        }
        for (CamelTestLifecycleHook hook : hooks) {
            action.accept(hook, main.getCamelContext(), info);
        }
    }

    @FunctionalInterface
    private interface TriConsumer {
        void accept(CamelTestLifecycleHook hook, CamelContext context, TestApplicationInfo info);
    }
}
