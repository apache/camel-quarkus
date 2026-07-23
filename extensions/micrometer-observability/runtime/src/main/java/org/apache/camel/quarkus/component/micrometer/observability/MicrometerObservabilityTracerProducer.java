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
package org.apache.camel.quarkus.component.micrometer.observability;

import java.util.List;

import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.otel.bridge.OtelBaggageManager;
import io.micrometer.tracing.otel.bridge.OtelCurrentTraceContext;
import io.micrometer.tracing.otel.bridge.OtelPropagator;
import io.micrometer.tracing.otel.bridge.OtelTracer;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.quarkus.arc.DefaultBean;
import io.quarkus.opentelemetry.runtime.config.runtime.OTelRuntimeConfig;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.apache.camel.CamelContext;
import org.apache.camel.micrometer.observability.MicrometerObservabilityTracer;

@Singleton
public class MicrometerObservabilityTracerProducer {

    @Inject
    CamelMicrometerObservabilityConfig config;

    @Inject
    OTelRuntimeConfig oTelRuntimeConfig;

    @Inject
    OpenTelemetry openTelemetry;

    @Produces
    @Singleton
    @DefaultBean
    public MicrometerObservabilityTracer getMicrometerObservabilityTracer(CamelContext camelContext) {
        if (oTelRuntimeConfig.sdkDisabled()) {
            return null;
        }

        // Bridge from the Quarkus-provided OpenTelemetry bean to Micrometer Tracing
        Tracer nativeOtelTracer = openTelemetry.getTracer("camel");
        OtelCurrentTraceContext currentTraceContext = new OtelCurrentTraceContext();
        OtelTracer otelTracer = new OtelTracer(
                nativeOtelTracer,
                currentTraceContext,
                event -> {
                },
                new OtelBaggageManager(currentTraceContext, List.of(), List.of()));
        OtelPropagator otelPropagator = new OtelPropagator(openTelemetry.getPropagators(), nativeOtelTracer);

        MicrometerObservabilityTracer tracer = new MicrometerObservabilityTracer();
        tracer.setTracer(otelTracer);
        tracer.setPropagator(otelPropagator);
        tracer.setObservationRegistry(ObservationRegistry.create());

        config.excludePatterns().ifPresent(tracer::setExcludePatterns);
        config.includePatterns().ifPresent(tracer::setIncludePatterns);
        tracer.setTraceProcessors(config.traceProcessors());
        tracer.setDisableCoreProcessors(config.disableCoreProcessors());
        tracer.setTraceHeadersInclusion(config.traceHeadersInclusion());

        tracer.init(camelContext);
        return tracer;
    }
}
