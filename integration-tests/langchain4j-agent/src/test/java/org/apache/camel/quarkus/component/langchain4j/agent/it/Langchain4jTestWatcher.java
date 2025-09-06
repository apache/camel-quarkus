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
package org.apache.camel.quarkus.component.langchain4j.agent.it;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;

public class Langchain4jTestWatcher implements TestWatcher {
    private static final String TEST_TO_WATCH = "simpleRag";
    private static final AtomicBoolean RAG_TEST_EXECUTED = new AtomicBoolean(false);

    public static boolean isRagTestExecuted() {
        return RAG_TEST_EXECUTED.get();
    }

    public static void reset() {
        RAG_TEST_EXECUTED.set(false);
    }

    @Override
    public void testSuccessful(ExtensionContext context) {
        if (isTargetTest(context)) {
            RAG_TEST_EXECUTED.set(true);
        }
    }

    @Override
    public void testFailed(ExtensionContext context, Throwable cause) {
        if (isTargetTest(context)) {
            RAG_TEST_EXECUTED.set(true);
        }
    }

    private boolean isTargetTest(ExtensionContext context) {
        return context.getDisplayName().equals(TEST_TO_WATCH);
    }
}
