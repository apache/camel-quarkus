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

import java.util.Map;

import io.smallrye.config.PropertiesConfigSource;
import io.smallrye.config.SmallRyeConfigBuilder;
import org.eclipse.microprofile.config.Config;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Each case builds its own {@link Config} rather than registering one for the class loader, so that nothing here
 * disturbs the configuration the rest of the tests in this JVM rely on.
 */
public class CamelDebugEnabledTest {

    private static final String CAMEL_DEBUG_ENABLED = "camel.debug.enabled";
    private static final String CAMEL_DEBUG_OTHER = "camel.debug.logging-level";

    @Test
    public void notEnabledWhenUnconfigured() {
        assertFalse(isDebugEnabled(Map.of()));
    }

    @Test
    public void enabledWhenTrue() {
        assertTrue(isDebugEnabled(Map.of(CAMEL_DEBUG_ENABLED, "true")));
    }

    @Test
    public void notEnabledWhenExplicitlyFalse() {
        assertFalse(isDebugEnabled(Map.of(CAMEL_DEBUG_ENABLED, "false")));
    }

    @Test
    public void notEnabledByAnUnrelatedCamelDebugProperty() {
        // A camel.debug.* key being present says nothing about whether debugging is on
        assertFalse(isDebugEnabled(Map.of(CAMEL_DEBUG_OTHER, "INFO")));
    }

    @Test
    public void notEnabledWhenFalseAlongsideAnotherCamelDebugProperty() {
        assertFalse(isDebugEnabled(Map.of(CAMEL_DEBUG_ENABLED, "false", CAMEL_DEBUG_OTHER, "INFO")));
    }

    private static boolean isDebugEnabled(Map<String, String> properties) {
        Config config = new SmallRyeConfigBuilder()
                .withSources(new PropertiesConfigSource(properties, "CamelDebugEnabledTest", 100))
                .build();
        return CamelDebugProcessor.CamelDebugEnabled.isDebugEnabled(config);
    }
}
