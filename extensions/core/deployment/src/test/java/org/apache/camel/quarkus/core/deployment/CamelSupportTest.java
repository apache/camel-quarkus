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

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import static org.apache.camel.quarkus.core.deployment.CamelSupport.isPathIncluded;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CamelSupportTest {

    @Test
    public void testPathFiltering() {
        assertFalse(isPathIncluded(
                "org/acme/MyClass",
                Arrays.asList("org/**"),
                Arrays.asList()));
        assertFalse(isPathIncluded(
                "org/acme/MyClass",
                Arrays.asList("org/**"),
                Arrays.asList("org/**", "org/acme/MyClass")));
        assertFalse(isPathIncluded(
                "org/acme/MyClass",
                Arrays.asList("org/acme/M*"),
                Arrays.asList("org/acme/MyClass")));
        assertFalse(isPathIncluded(
                "org/acme/MyClass",
                Arrays.asList(),
                Arrays.asList("org/acme/A*")));

        assertTrue(isPathIncluded(
                "org/acme/MyClass",
                Arrays.asList(),
                Arrays.asList()));
        assertTrue(isPathIncluded(
                "org/acme/MyClass",
                Arrays.asList(),
                Arrays.asList("org/**")));
        assertTrue(isPathIncluded(
                "org/acme/MyClass",
                Arrays.asList("org/acme/A*"),
                Arrays.asList("org/acme/MyClass")));
    }
}
