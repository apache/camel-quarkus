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
package org.apache.camel.quarkus.component.console;

import io.quarkus.test.QuarkusExtensionTest;
import jakarta.inject.Inject;
import org.apache.camel.CamelContext;
import org.apache.camel.console.DevConsoleRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConsoleEnabledWithDevProfileTest {
    @RegisterExtension
    static final QuarkusExtensionTest CONFIG = new QuarkusExtensionTest()
            .withEmptyApplication()
            .overrideConfigKey("camel.main.profile", "dev");

    @Inject
    CamelContext context;

    @Test
    void devConsoleRegistryLoaded() {
        DevConsoleRegistry registry = context.getCamelContextExtension().getContextPlugin(DevConsoleRegistry.class);
        assertNotNull(registry);
        assertTrue(registry.getConsole("properties").isPresent());
    }

    @Test
    void propertiesConsoleIsQuarkusOverride() {
        DevConsoleRegistry registry = context.getCamelContextExtension().getContextPlugin(DevConsoleRegistry.class);
        assertTrue(registry.getConsole("properties").get() instanceof QuarkusPropertiesDevConsole);
    }
}
