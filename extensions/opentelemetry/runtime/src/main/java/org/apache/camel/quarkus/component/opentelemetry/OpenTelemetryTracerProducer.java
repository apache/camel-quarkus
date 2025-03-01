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
package org.apache.camel.quarkus.component.opentelemetry;

import io.opentelemetry.api.trace.Tracer;
import io.quarkus.arc.DefaultBean;
import io.quarkus.opentelemetry.runtime.config.runtime.OTelRuntimeConfig;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.apache.camel.opentelemetry.CamelQuarkusOpenTelemetryTracer;
import org.apache.camel.opentelemetry.OpenTelemetryTracer;
import org.apache.camel.opentelemetry.OpenTelemetryTracingStrategy;

@Singleton
public class OpenTelemetryTracerProducer {

    @Inject
    CamelOpenTelemetryConfig config;

    @Inject
    OTelRuntimeConfig oTelRuntimeConfig;

    @Inject
    Tracer tracer;

    @Produces
    @Singleton
    @DefaultBean
    public OpenTelemetryTracer getOpenTelemetry() {
        if (!oTelRuntimeConfig.sdkDisabled()) {
            OpenTelemetryTracer openTelemetryTracer = new CamelQuarkusOpenTelemetryTracer();
            if (tracer != null) {
                openTelemetryTracer.setTracer(tracer);
                if (config.excludePatterns().isPresent()) {
                    openTelemetryTracer.setExcludePatterns(config.excludePatterns().get());
                }

                if (config.traceProcessors()) {
                    OpenTelemetryTracingStrategy tracingStrategy = new OpenTelemetryTracingStrategy(openTelemetryTracer);
                    tracingStrategy.setPropagateContext(true);
                    openTelemetryTracer.setTracingStrategy(tracingStrategy);
                }

                openTelemetryTracer.setEncoding(config.encoding());
            }
            return openTelemetryTracer;
        }
        return null;
    }
}
