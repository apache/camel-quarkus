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

import java.util.Set;

import org.apache.camel.CamelContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Resets {@link CamelContext} state created by tests.
 * <p>
 * For internal use by the Camel Quarkus test framework.
 */
public final class TestContextCleanup {
    private static final Logger LOG = LoggerFactory.getLogger(TestContextCleanup.class);

    private TestContextCleanup() {
        // Utility class
    }

    /**
     * Removes every component that is not in the given set of component names.
     */
    public static void removeComponentsNotIn(CamelContext context, Set<String> existingComponents) {
        context.getComponentNames().forEach(cn -> {
            if (!existingComponents.contains(cn)) {
                LOG.debug("Removing test-created component: {}", cn);
                context.removeComponent(cn);
            } else {
                LOG.debug("Preserving existing component: {}", cn);
            }
        });
    }
}
