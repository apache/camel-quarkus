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
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "camel", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class CamelConfig {
    /**
     * Build time configuration options for {@code camel-main}.
     */
    @ConfigItem
    public MainConfig main;

    @ConfigGroup
    public static class MainConfig {
        /**
         * Enable {@code camel-main}. If {@code true}, routes are automatically
         * loaded and started and the entire lifecycle of the Camel Context is
         * under the control of the {@code camel-main} component. Otherwise, the
         * application developer is responsible for performing all the mentioned
         * tasks.
         */
        @ConfigItem(defaultValue = "true")
        public boolean enabled;

        /**
         * Build time configuration options for routes discovery.
         */
        @ConfigItem
        public RoutesDiscoveryConfig routesDiscovery;
    }

    @ConfigGroup
    public static class RoutesDiscoveryConfig {
        /**
         * Enable automatic discovery of routes during static initialization.
         */
        @ConfigItem(defaultValue = "true")
        public boolean enabled;

        /**
         * Used for exclusive filtering scanning of RouteBuilder classes.
         * The exclusive filtering takes precedence over inclusive filtering.
         * The pattern is using Ant-path style pattern.
         * Multiple patterns can be specified separated by comma.
         *
         * For example to exclude all classes starting with Bar use: &#42;&#42;/Bar&#42;
         * To exclude all routes form a specific package use: com/mycompany/bar/&#42;
         * To exclude all routes form a specific package and its sub-packages use double wildcards: com/mycompany/bar/&#42;&#42;
         * And to exclude all routes from two specific packages use: com/mycompany/bar/&#42;,com/mycompany/stuff/&#42;
         */
        @ConfigItem
        public Optional<List<String>> excludePatterns;

        /**
         * Used for inclusive filtering scanning of RouteBuilder classes.
         * The exclusive filtering takes precedence over inclusive filtering.
         * The pattern is using Ant-path style pattern.
         *
         * Multiple patterns can be specified separated by comma.
         * For example to include all classes starting with Foo use: &#42;&#42;/Foo*
         * To include all routes form a specific package use: com/mycompany/foo/&#42;
         * To include all routes form a specific package and its sub-packages use double wildcards: com/mycompany/foo/&#42;&#42;
         * And to include all routes from two specific packages use: com/mycompany/foo/&#42;,com/mycompany/stuff/&#42;
         */
        @ConfigItem
        public Optional<List<String>> includePatterns;
    }
}
