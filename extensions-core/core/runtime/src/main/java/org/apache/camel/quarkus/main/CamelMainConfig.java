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
package org.apache.camel.quarkus.main;

import java.time.Duration;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import org.apache.camel.quarkus.core.CamelConfig.FailureRemedy;

@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
@ConfigMapping(prefix = "quarkus.camel.main")
public interface CamelMainConfig {
    /**
     * Build time configuration options for `CamelMain` shutdown.
     *
     * @asciidoclet
     */
    ShutdownConfig shutdown();

    /**
     * Build time configuration options for `CamelMain` arguments
     *
     * @asciidoclet
     */
    ArgumentConfig arguments();

    interface ShutdownConfig {
        /**
         * A timeout (with millisecond precision) to wait for `CamelMain++#++stop()` to finish
         *
         * @asciidoclet
         */
        @WithDefault("PT3S")
        Duration timeout();
    }

    interface ArgumentConfig {
        /**
         * The action to take when `CamelMain` encounters an unknown argument. fail - Prints the `CamelMain` usage statement and
         * throws a `RuntimeException` ignore - Suppresses any warnings and the application startup proceeds as normal warn -
         * Prints the `CamelMain` usage statement but allows the application startup to proceed as normal
         *
         * @asciidoclet
         */
        @WithDefault("warn")
        FailureRemedy onUnknown();
    }
}
