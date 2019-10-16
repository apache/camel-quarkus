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
package org.apache.camel.quarkus.core;

import java.util.List;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

public class CamelConfig {

    @ConfigRoot(name = "camel", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
    public static class BuildTime {
        /**
         * Disable camel-main.
         * When main is disabled, routes won't be automatically be loaded and
         * started and the entire lifecycle of the Camel Context is under user
         * control.
         */
        @ConfigItem(defaultValue = "false")
        public boolean disableMain;
    }

    @ConfigRoot(name = "camel", phase = ConfigPhase.RUN_TIME)
    public static class Runtime {

        /**
         * Dump loaded routes when starting
         */
        @ConfigItem(defaultValue = "false")
        public boolean dumpRoutes;

        /**
         * A list of URIs containing the camel routes in the XML format to be
         * loaded at runtime. Files can be loaded from either classpath or file
         * system by prefixing the URI with {@code classpath:} or {@code file:}
         * respectively.
         */
        @ConfigItem
        public List<String> routesUris;
    }

}
