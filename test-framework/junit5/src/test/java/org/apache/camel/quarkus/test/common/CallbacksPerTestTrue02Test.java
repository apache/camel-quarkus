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

import java.util.concurrent.TimeUnit;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import static org.awaitility.Awaitility.await;

// replaces CreateCamelContextPerTestTrueTest
@QuarkusTest
@TestProfile(CallbacksPerTestTrue01Test.class)
public class CallbacksPerTestTrue02Test {

    @Test
    public void testAfter01Class() {

        await().atMost(5, TimeUnit.SECONDS).until(() -> AbstractCallbacksTest.testFromAnotherClass(
                CallbacksPerTestTrue02Test.class.getSimpleName(),
                CallbacksPerTestTrue01Test.createAssertionConsumer()),
                Matchers.is(AbstractCallbacksTest.Callback.values().length));

    }
}
