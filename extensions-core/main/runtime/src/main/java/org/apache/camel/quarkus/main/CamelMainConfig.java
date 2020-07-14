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

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "camel.main", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class CamelMainConfig {

    /**
     * Build time configuration options for {@link CamelMain} shutdown.
     */
    @ConfigItem
    public ShutdownConfig shutdown;

    @ConfigGroup
    public static class ShutdownConfig {
        /**
         * A timeout (with millisecond precision) to wait for {@link CamelMain#stop()} to finish
         */
        @ConfigItem(defaultValue = "PT3S")
        public Duration timeout;
    }
}
