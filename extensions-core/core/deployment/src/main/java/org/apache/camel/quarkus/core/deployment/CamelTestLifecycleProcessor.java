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
package org.apache.camel.quarkus.core.deployment;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Optional;

import io.quarkus.deployment.IsTest;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.TestProfileBuildItem;
import org.apache.camel.quarkus.core.deployment.spi.CamelMainListenerBuildItem;
import org.apache.camel.quarkus.core.test.CamelTestLifecycleHook;
import org.apache.camel.quarkus.main.CamelMainRecorder;

public class CamelTestLifecycleProcessor {
    private static final String HOOK_SERVICE_RESOURCE = "META-INF/services/" + CamelTestLifecycleHook.class.getName();

    /**
     * Registers a {@link org.apache.camel.main.MainListener} that delegates to every {@link CamelTestLifecycleHook}
     * implementation, when the application is built for tests and at least one implementation is present.
     */
    @BuildStep(onlyIf = IsTest.class)
    @Record(ExecutionTime.STATIC_INIT)
    void testLifecycleHooks(
            Optional<TestProfileBuildItem> testProfile,
            CamelMainRecorder recorder,
            BuildProducer<CamelMainListenerBuildItem> listeners) {
        if (!hasHookImplementations()) {
            return;
        }
        String testProfileClassName = testProfile.map(TestProfileBuildItem::getTestProfileClassName).orElse("");
        listeners.produce(new CamelMainListenerBuildItem(recorder.createTestLifecycleListener(testProfileClassName)));
    }

    static boolean hasHookImplementations() {
        try {
            return Thread.currentThread().getContextClassLoader().getResources(HOOK_SERVICE_RESOURCE).hasMoreElements();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
