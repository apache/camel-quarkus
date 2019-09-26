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
package org.apache.camel.quarkus.component.microprofile.metrics.runtime;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "camel.metrics", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public final class CamelMicroProfileMetricsConfig {

    /**
     * Set whether to enable the MicroProfileMetricsRoutePolicyFactory for capturing metrics
     * on route processing times.
     */
    @ConfigItem(defaultValue = "true")
    public boolean enableRoutePolicy;

    /**
     * Set whether to enable the MicroProfileMetricsMessageHistoryFactory for capturing metrics
     * on individual route node processing times.
     *
     * Depending on the number of configured route nodes, there is the potential to create a large
     * volume of metrics. Therefore, this option is disabled by default.
     */
    @ConfigItem(defaultValue = "false")
    public boolean enableMessageHistory;

    /**
     * Set whether to enable the MicroProfileMetricsExchangeEventNotifier for capturing metrics
     * on exchange processing times.
     */
    @ConfigItem(defaultValue = "true")
    public boolean enableExchangeEventNotifier;

    /**
     * Set whether to enable the MicroProfileMetricsRouteEventNotifier for capturing metrics
     * on the total number of routes and total number of routes running.
     */
    @ConfigItem(defaultValue = "true")
    public boolean enableRouteEventNotifier;
}
