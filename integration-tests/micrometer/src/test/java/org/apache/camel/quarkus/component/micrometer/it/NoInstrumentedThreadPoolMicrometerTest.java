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
package org.apache.camel.quarkus.component.micrometer.it;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test class runs only in JVM mode, because profile changes some application.properties which is not allowed in native.
 * (native Exception: "java.lang.IllegalStateException: Build time property cannot be changed at runtime:")
 * Test verify that instrumented thread pool metric is not present, if the feature is turned off.
 */
@QuarkusTest
@TestProfile(NoInstrumentedThreadPoolProfile.class)
class NoInstrumentedThreadPoolMicrometerTest extends AbstractMicrometerTest {

    @Test
    public void testInstrumentedThreadPoolFactory() {
        assertEquals("Metric does not exist", getMetricValue(String.class, "timer", "executor", null, 500));
    }
}
