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

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
@ConfigMapping(prefix = "quarkus.camel.console")
public interface CamelConsoleConfig {
    /**
     * Whether the Camel developer console is enabled.
     */
    @WithDefault("false")
    boolean enabled();

    /**
     * The context path under which the Camel developer console is deployed (default `/q/camel/dev-console`).
     */
    @WithDefault("camel/dev-console")
    String path();

    /**
     * The modes in which the Camel developer console is available. The default `dev-test` enables the developer
     * console only in dev mode and test modes.
     * A value of `all` enables agent discovery in dev, test and prod modes. Setting the value to `none` will
     * not expose the developer console HTTP endpoint.
     */
    @WithDefault("DEV_TEST")
    ExposureMode exposureMode();

    enum ExposureMode {
        ALL,
        DEV_TEST,
        NONE,
    }
}
