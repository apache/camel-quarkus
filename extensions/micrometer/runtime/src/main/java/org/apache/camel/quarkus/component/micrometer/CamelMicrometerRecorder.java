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

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import org.apache.camel.CamelContext;
import org.apache.camel.component.micrometer.MicrometerUtils;
import org.apache.camel.component.micrometer.eventnotifier.MicrometerExchangeEventNotifier;
import org.apache.camel.component.micrometer.eventnotifier.MicrometerExchangeEventNotifierNamingStrategyDefault;
import org.apache.camel.component.micrometer.eventnotifier.MicrometerExchangeEventNotifierNamingStrategyLegacy;
import org.apache.camel.component.micrometer.eventnotifier.MicrometerRouteEventNotifier;
import org.apache.camel.component.micrometer.eventnotifier.MicrometerRouteEventNotifierNamingStrategy;
import org.apache.camel.component.micrometer.messagehistory.MicrometerMessageHistoryFactory;
import org.apache.camel.component.micrometer.messagehistory.MicrometerMessageHistoryNamingStrategy;
import org.apache.camel.component.micrometer.routepolicy.MicrometerRoutePolicyConfiguration;
import org.apache.camel.component.micrometer.routepolicy.MicrometerRoutePolicyFactory;
import org.apache.camel.component.micrometer.routepolicy.MicrometerRoutePolicyNamingStrategy;
import org.apache.camel.component.micrometer.spi.InstrumentedThreadPoolFactory;
import org.apache.camel.quarkus.component.micrometer.CamelMicrometerConfig.MetricsNamingStrategy;
import org.apache.camel.quarkus.component.micrometer.CamelMicrometerConfig.RoutePolicyLevel;
import org.apache.camel.spi.CamelContextCustomizer;
import org.apache.camel.spi.ManagementStrategy;

@Recorder
public class CamelMicrometerRecorder {

    public RuntimeValue<CamelContextCustomizer> createContextCustomizer(CamelMicrometerConfig config) {
        return new RuntimeValue<>(new MicrometerContextCustomizer(config));
    }

    public RuntimeValue<CamelContextCustomizer> createRuntimeContextCustomizer(CamelMicrometerConfig config,
            RuntimeValue<MeterRegistry> meterRegistry) {
        return new RuntimeValue<>(new MicrometerRuntimeContextCustomizer(config, meterRegistry.getValue()));
    }

    public void configureDefaultRegistry(RuntimeValue<MeterRegistry> rootMeterRegistry) {
        // Add SimpleMeterRegistry to the Quarkus composite one
        if (rootMeterRegistry.getValue() instanceof CompositeMeterRegistry) {
            ((CompositeMeterRegistry) rootMeterRegistry.getValue()).add(MicrometerUtils.createMeterRegistry());
        }
    }

    private static class MicrometerContextCustomizer implements CamelContextCustomizer {
        private final CamelMicrometerConfig config;

        public MicrometerContextCustomizer(CamelMicrometerConfig config) {
            this.config = config;
        }

        @Override
        public void configure(CamelContext camelContext) {
            if (config.enableRoutePolicy()) {
                MicrometerRoutePolicyFactory factory = new MicrometerRoutePolicyFactory();
                factory.setCamelContext(camelContext);
                camelContext.addRoutePolicyFactory(factory);

                if (config.namingStrategy().equals(MetricsNamingStrategy.LEGACY)) {
                    factory.setNamingStrategy(MicrometerRoutePolicyNamingStrategy.LEGACY);
                }

                MicrometerRoutePolicyConfiguration policyConfiguration = factory.getPolicyConfiguration();
                if (config.routePolicyLevel().equals(RoutePolicyLevel.ALL)) {
                    factory.getPolicyConfiguration().setContextEnabled(true);
                    factory.getPolicyConfiguration().setRouteEnabled(true);
                } else if (config.routePolicyLevel().equals(RoutePolicyLevel.CONTEXT)) {
                    factory.getPolicyConfiguration().setContextEnabled(true);
                    factory.getPolicyConfiguration().setRouteEnabled(false);
                } else {
                    policyConfiguration.setContextEnabled(false);
                    policyConfiguration.setRouteEnabled(true);
                }

                config.routePolicyExcludePattern().ifPresent(policyConfiguration::setExcludePattern);
            }

            ManagementStrategy managementStrategy = camelContext.getManagementStrategy();
            if (config.enableExchangeEventNotifier()) {
                MicrometerExchangeEventNotifier eventNotifier = new MicrometerExchangeEventNotifier();
                eventNotifier.setBaseEndpointURI(config.baseEndpointURIExchangeEventNotifier());

                if (config.namingStrategy().equals(MetricsNamingStrategy.LEGACY)) {
                    eventNotifier.setNamingStrategy(
                            new MicrometerExchangeEventNotifierNamingStrategyLegacy(
                                    config.baseEndpointURIExchangeEventNotifier()));
                } else {
                    eventNotifier.setNamingStrategy(
                            new MicrometerExchangeEventNotifierNamingStrategyDefault(
                                    config.baseEndpointURIExchangeEventNotifier()));
                }
                managementStrategy.addEventNotifier(eventNotifier);
            }

            if (config.enableRouteEventNotifier()) {
                MicrometerRouteEventNotifier eventNotifier = new MicrometerRouteEventNotifier();
                if (config.namingStrategy().equals(MetricsNamingStrategy.LEGACY)) {
                    eventNotifier.setNamingStrategy(MicrometerRouteEventNotifierNamingStrategy.LEGACY);
                }
                managementStrategy.addEventNotifier(eventNotifier);
            }
        }
    }

    private static class MicrometerRuntimeContextCustomizer implements CamelContextCustomizer {
        private final CamelMicrometerConfig config;
        private final MeterRegistry meterRegistry;

        public MicrometerRuntimeContextCustomizer(CamelMicrometerConfig config, MeterRegistry meterRegistry) {
            this.config = config;
            this.meterRegistry = meterRegistry;
        }

        @Override
        public void configure(CamelContext camelContext) {
            if (config.enableInstrumentedThreadPoolFactory()) {
                InstrumentedThreadPoolFactory instrumentedThreadPoolFactory = new InstrumentedThreadPoolFactory(meterRegistry,
                        camelContext.getExecutorServiceManager().getThreadPoolFactory());
                camelContext.getExecutorServiceManager().setThreadPoolFactory(instrumentedThreadPoolFactory);
            }

            if (!config.enableMessageHistory()) {
                return;
            }

            if (!camelContext.isMessageHistory()) {
                camelContext.setMessageHistory(true);
            }

            MicrometerMessageHistoryFactory messageHistoryFactory = new MicrometerMessageHistoryFactory();
            if (config.namingStrategy().equals(MetricsNamingStrategy.LEGACY)) {
                messageHistoryFactory.setNamingStrategy(MicrometerMessageHistoryNamingStrategy.LEGACY);
            }
            camelContext.setMessageHistoryFactory(messageHistoryFactory);
        }
    }
}
