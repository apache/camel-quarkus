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
package org.apache.camel.quarkus.test.common;

import java.util.function.BiConsumer;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.TestInstance;

// replaces CreateCamelContextPerTestTrueTest
@QuarkusTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestProfile(CallbacksPerTestTrue01Test.class)
public class CallbacksPerTestTrue01Test extends AbstractCallbacksTest {

    public CallbacksPerTestTrue01Test() {
        super(CallbacksPerTestTrue01Test.class.getSimpleName(), CallbacksPerTestTrue02Test.class.getSimpleName());
    }

    @AfterAll
    public static void shouldTearDown() {
        testAfterAll(CallbacksPerTestTrue01Test.class.getSimpleName(), createAssertionConsumer());
    }

    protected static BiConsumer<Callback, Long> createAssertionConsumer() {
        return (callback, count) -> {
            switch (callback) {
            case doSetup:
                assertCount(1, count, callback, CallbacksPerTestTrue01Test.class.getSimpleName());
                break;
            case contextCreation:
                assertCount(1, count, callback, CallbacksPerTestTrue01Test.class.getSimpleName());
                break;
            case postSetup:
                assertCount(1, count, callback, CallbacksPerTestTrue01Test.class.getSimpleName());
                break;
            case postTearDown:
                assertCount(1, count, callback, CallbacksPerTestTrue01Test.class.getSimpleName());
                break;
            case preSetup:
                assertCount(1, count, callback, CallbacksPerTestTrue01Test.class.getSimpleName());
                break;
            case afterAll:
                assertCount(1, count, callback, CallbacksPerTestFalse01Test.class.getSimpleName());
                break;
            case afterConstruct:
                assertCount(1, count, callback, CallbacksPerTestFalse01Test.class.getSimpleName());
                break;
            case afterEach:
                assertCount(3, count, callback, CallbacksPerTestFalse01Test.class.getSimpleName());
                break;
            case beforeEach:
                assertCount(3, count, callback, CallbacksPerTestFalse01Test.class.getSimpleName());
                break;
            default:
                throw new IllegalArgumentException("Unknown callback type");
            }
        };
    }
}
