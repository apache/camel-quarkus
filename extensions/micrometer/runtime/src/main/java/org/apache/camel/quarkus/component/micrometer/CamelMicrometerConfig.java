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
package org.apache.camel.quarkus.component.micrometer;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "camel.metrics", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class CamelMicrometerConfig {

    /**
     * Set whether to enable the MicrometerRoutePolicyFactory for capturing metrics on route processing times.
     *
     * @asciidoclet
     */
    @ConfigItem(defaultValue = "true")
    public boolean enableRoutePolicy;

    /**
     * Set whether to enable the MicrometerMessageHistoryFactory for capturing metrics on individual route node processing
     * times. Depending on the number of configured route nodes, there is the potential to create a large volume of metrics.
     * Therefore, this option is disabled by default.
     *
     * @asciidoclet
     */
    @ConfigItem(defaultValue = "false")
    public boolean enableMessageHistory;

    /**
     * Set whether to enable the MicrometerExchangeEventNotifier for capturing metrics on exchange processing times.
     *
     * @asciidoclet
     */
    @ConfigItem(defaultValue = "true")
    public boolean enableExchangeEventNotifier;

    /**
     * Set whether to enable the MicrometerRouteEventNotifier for capturing metrics on the total number of routes and total
     * number of routes running.
     *
     * @asciidoclet
     */
    @ConfigItem(defaultValue = "true")
    public boolean enableRouteEventNotifier;

    /**
     * Set whether to gather performance information about Camel Thread Pools by injecting an InstrumentedThreadPoolFactory.
     *
     * @asciidoclet
     */
    @ConfigItem(defaultValue = "false")
    public boolean enableInstrumentedThreadPoolFactory;

    /**
     * Controls the naming style to use for metrics. The available values are `default` and `legacy`. `default` uses the
     * default Micrometer naming convention. `legacy` uses the legacy camel-case naming style.
     *
     * @asciidoclet
     */
    @ConfigItem(defaultValue = "default")
    public MetricsNamingStrategy namingStrategy;

    /**
     * Sets the level of metrics to capture. The available values are `all` ,`context` and `route`. `all` captures metrics
     * for both the camel context and routes. `route` captures metrics for routes only. `context` captures metrics for the
     * camel context only.
     *
     * @asciidoclet
     */
    @ConfigItem(defaultValue = "all")
    public RoutePolicyLevel routePolicyLevel;

    /**
     * Comma separated list of route IDs to exclude from metrics collection.
     *
     * @asciidoclet
     */
    @ConfigItem
    public Optional<String> routePolicyExcludePattern;

    public enum MetricsNamingStrategy {

        DEFAULT, LEGACY
    }

    public enum RoutePolicyLevel {

        ALL, CONTEXT, ROUTE
    }
}
