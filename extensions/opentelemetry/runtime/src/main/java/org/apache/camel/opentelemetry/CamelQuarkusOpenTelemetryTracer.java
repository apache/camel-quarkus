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
package org.apache.camel.opentelemetry;

import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import org.apache.camel.Exchange;
import org.apache.camel.tracing.SpanAdapter;
import org.apache.camel.tracing.SpanDecorator;
import org.apache.camel.tracing.SpanKind;
import org.apache.camel.tracing.decorators.PlatformHttpSpanDecorator;
import org.apache.camel.tracing.decorators.ServletSpanDecorator;

/**
 * Custom {@link OpenTelemetryTracer} to integrate better with the existing Vert.x tracing configured by the
 * Quarkus OpenTelemetry extension
 */
public class CamelQuarkusOpenTelemetryTracer extends OpenTelemetryTracer {

    @Override
    protected SpanAdapter startExchangeBeginSpan(Exchange exchange, SpanDecorator sd, String operationName, SpanKind kind,
            SpanAdapter parent) {
        // Quarkus configures Vert.x for OpenTelemetry HTTP tracing. Therefore, avoid creating duplicate spans and
        // augment the existing incoming one with additional camel specific tags & attributes
        if (sd instanceof PlatformHttpSpanDecorator || sd instanceof ServletSpanDecorator) {
            Span span = Span.fromContext(Context.current());
            Baggage baggage = Baggage.fromContext(Context.current());
            return new OpenTelemetrySpanAdapter(span, baggage);
        }
        return super.startExchangeBeginSpan(exchange, sd, operationName, kind, parent);
    }
}
