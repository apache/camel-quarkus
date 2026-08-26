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

import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CamelDebugEnabledTest {

    private static final String CAMEL_DEBUG_ENABLED = "camel.debug.enabled";
    private static final String CAMEL_DEBUG_OTHER = "camel.debug.logging-level";

    @AfterEach
    public void clearProperties() {
        System.clearProperty(CAMEL_DEBUG_ENABLED);
        System.clearProperty(CAMEL_DEBUG_OTHER);
        releaseConfig();
    }

    @Test
    public void notEnabledWhenUnconfigured() {
        assertFalse(camelDebugEnabled());
    }

    @Test
    public void enabledWhenTrue() {
        setProperty(CAMEL_DEBUG_ENABLED, "true");
        assertTrue(camelDebugEnabled());
    }

    @Test
    public void notEnabledWhenExplicitlyFalse() {
        setProperty(CAMEL_DEBUG_ENABLED, "false");
        assertFalse(camelDebugEnabled());
    }

    @Test
    public void notEnabledByAnUnrelatedCamelDebugProperty() {
        // A camel.debug.* key being present says nothing about whether debugging is on
        setProperty(CAMEL_DEBUG_OTHER, "INFO");
        assertFalse(camelDebugEnabled());
    }

    @Test
    public void notEnabledWhenFalseAlongsideAnotherCamelDebugProperty() {
        setProperty(CAMEL_DEBUG_ENABLED, "false");
        setProperty(CAMEL_DEBUG_OTHER, "INFO");
        assertFalse(camelDebugEnabled());
    }

    private static boolean camelDebugEnabled() {
        return new CamelDebugProcessor.CamelDebugEnabled().getAsBoolean();
    }

    private static void setProperty(String key, String value) {
        System.setProperty(key, value);
        // The config is cached per classloader, so drop it to pick the new value up
        releaseConfig();
    }

    private static void releaseConfig() {
        ConfigProviderResolver resolver = ConfigProviderResolver.instance();
        resolver.releaseConfig(resolver.getConfig());
    }
}
