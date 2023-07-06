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
import org.apache.camel.component.micrometer.eventnotifier.MicrometerRouteEventNotifier;
import org.apache.camel.component.micrometer.messagehistory.MicrometerMessageHistoryFactory;
import org.apache.camel.component.micrometer.routepolicy.MicrometerRoutePolicyFactory;
import org.apache.camel.component.micrometer.spi.InstrumentedThreadPoolFactory;
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
            if (config.enableRoutePolicy) {
                camelContext.addRoutePolicyFactory(new MicrometerRoutePolicyFactory());
            }

            ManagementStrategy managementStrategy = camelContext.getManagementStrategy();
            if (config.enableExchangeEventNotifier) {
                managementStrategy.addEventNotifier(new MicrometerExchangeEventNotifier());
            }

            if (config.enableRouteEventNotifier) {
                managementStrategy.addEventNotifier(new MicrometerRouteEventNotifier());
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
            if (config.enableInstrumentedThreadPoolFactory) {
                InstrumentedThreadPoolFactory instrumentedThreadPoolFactory = new InstrumentedThreadPoolFactory(meterRegistry,
                        camelContext.getExecutorServiceManager().getThreadPoolFactory());
                camelContext.getExecutorServiceManager().setThreadPoolFactory(instrumentedThreadPoolFactory);
            }

            if (!config.enableMessageHistory) {
                return;
            }

            if (!camelContext.isMessageHistory()) {
                camelContext.setMessageHistory(true);
            }

            camelContext.setMessageHistoryFactory(new MicrometerMessageHistoryFactory());
        }
    }
}
