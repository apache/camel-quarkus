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

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.annotations.Recorder;
import io.smallrye.metrics.MetricRegistries;

import org.apache.camel.CamelContext;
import org.apache.camel.component.microprofile.metrics.event.notifier.exchange.MicroProfileMetricsExchangeEventNotifier;
import org.apache.camel.component.microprofile.metrics.event.notifier.route.MicroProfileMetricsRouteEventNotifier;
import org.apache.camel.component.microprofile.metrics.message.history.MicroProfileMetricsMessageHistoryFactory;
import org.apache.camel.component.microprofile.metrics.route.policy.MicroProfileMetricsRoutePolicyFactory;
import org.apache.camel.spi.ManagementStrategy;
import org.eclipse.microprofile.metrics.MetricRegistry;

@Recorder
public class CamelMicroProfileMetricsRecorder {

    public MetricRegistry createApplicationRegistry() {
        return MetricRegistries.get(MetricRegistry.Type.APPLICATION);
    }

    public void configureCamelContext(CamelMicroProfileMetricsConfig config, BeanContainer beanContainer) {
        CamelContext camelContext = beanContainer.instance(CamelContext.class);
        ManagementStrategy managementStrategy = camelContext.getManagementStrategy();

        if (config.enableRoutePolicy) {
            camelContext.addRoutePolicyFactory(new MicroProfileMetricsRoutePolicyFactory());
        }

        if (config.enableMessageHistory) {
            camelContext.setMessageHistory(true);
            camelContext.setMessageHistoryFactory(new MicroProfileMetricsMessageHistoryFactory());
        }

        if (config.enableExchangeEventNotifier) {
            managementStrategy.addEventNotifier(new MicroProfileMetricsExchangeEventNotifier());
        }

        if (config.enableRouteEventNotifier) {
            managementStrategy.addEventNotifier(new MicroProfileMetricsRouteEventNotifier());
        }
    }
}
