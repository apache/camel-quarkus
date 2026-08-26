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
package org.apache.camel.quarkus.component.cli.connector.deployment;

import io.quarkus.runtime.LaunchMode;
import org.apache.camel.quarkus.component.cli.connector.CamelCliConnectorConfig;
import org.apache.camel.quarkus.component.cli.connector.CamelCliConnectorConfig.ExposureMode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the launch mode dimension of the gate, which {@code QuarkusUnitTest} cannot reach because it always runs in
 * test mode.
 */
public class CliConnectorEnabledSupplierTest {

    @Test
    public void devTestModeEnabledInDevAndTest() {
        assertTrue(enabled(true, ExposureMode.DEV_TEST, LaunchMode.DEVELOPMENT));
        assertTrue(enabled(true, ExposureMode.DEV_TEST, LaunchMode.TEST));
    }

    @Test
    public void devTestModeNotEnabledInProd() {
        assertFalse(enabled(true, ExposureMode.DEV_TEST, LaunchMode.NORMAL));
    }

    @Test
    public void allModeEnabledEverywhere() {
        assertTrue(enabled(true, ExposureMode.ALL, LaunchMode.NORMAL));
        assertTrue(enabled(true, ExposureMode.ALL, LaunchMode.DEVELOPMENT));
        assertTrue(enabled(true, ExposureMode.ALL, LaunchMode.TEST));
    }

    @Test
    public void noneModeNeverEnabled() {
        assertFalse(enabled(true, ExposureMode.NONE, LaunchMode.DEVELOPMENT));
        assertFalse(enabled(true, ExposureMode.NONE, LaunchMode.NORMAL));
    }

    @Test
    public void disabledWinsOverEveryExposureMode() {
        for (ExposureMode mode : ExposureMode.values()) {
            assertFalse(enabled(false, mode, LaunchMode.DEVELOPMENT));
            assertFalse(enabled(false, mode, LaunchMode.NORMAL));
        }
    }

    private static boolean enabled(boolean enabled, ExposureMode exposureMode, LaunchMode launchMode) {
        CliConnectorProcessor.CliConnectorEnabled supplier = new CliConnectorProcessor.CliConnectorEnabled();
        supplier.config = new CamelCliConnectorConfig() {
            @Override
            public boolean enabled() {
                return enabled;
            }

            @Override
            public ExposureMode exposureMode() {
                return exposureMode;
            }
        };
        supplier.launchMode = launchMode;
        return supplier.getAsBoolean();
    }
}
