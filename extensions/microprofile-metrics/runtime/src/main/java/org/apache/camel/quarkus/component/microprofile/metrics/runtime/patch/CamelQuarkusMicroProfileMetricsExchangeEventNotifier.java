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
package org.apache.camel.quarkus.component.microprofile.metrics.runtime.patch;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.component.microprofile.metrics.MicroProfileMetricsExchangeRecorder;
import org.apache.camel.component.microprofile.metrics.event.notifier.exchange.MicroProfileMetricsExchangeEventNotifier;
import org.apache.camel.spi.CamelEvent;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Tag;
import org.eclipse.microprofile.metrics.Timer;
import static org.apache.camel.component.microprofile.metrics.MicroProfileMetricsConstants.CAMEL_CONTEXT_METRIC_NAME;
import static org.apache.camel.component.microprofile.metrics.MicroProfileMetricsConstants.CAMEL_CONTEXT_TAG;

/**
 * Handle MicroProfile metrics API incompatibility between versions used
 * by Camel <= 3.0.0 RC3 and Quarkus >= 0.26
 *
 * TODO: Remove this when upgrading to Camel > 3.0.0 RC3
 */
public class CamelQuarkusMicroProfileMetricsExchangeEventNotifier extends MicroProfileMetricsExchangeEventNotifier {

    private MicroProfileMetricsExchangeRecorder exchangeRecorder;

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        CamelContext camelContext = getCamelContext();
        MetricRegistry metricRegistry = getMetricRegistry();
        Tag tag = new Tag(CAMEL_CONTEXT_TAG, camelContext.getName());

        metricRegistry.removeMatching((metricID, metric) -> metricID.getName().startsWith(CAMEL_CONTEXT_METRIC_NAME));

        exchangeRecorder = new CamelQurakusMicroProfileMetricsExchangeRecorder(metricRegistry, CAMEL_CONTEXT_METRIC_NAME, tag);
    }

    @Override
    protected void handleCreatedEvent(CamelEvent.ExchangeCreatedEvent createdEvent) {
        String name = getNamingStrategy().getName(createdEvent.getExchange(), createdEvent.getExchange().getFromEndpoint());
        Tag[] tags = getNamingStrategy().getTags(createdEvent, createdEvent.getExchange().getFromEndpoint());
        Timer timer = getMetricRegistry().timer(name + ".processing", tags);
        createdEvent.getExchange().setProperty("eventTimer:" + name, timer);
        createdEvent.getExchange().setProperty("eventTimerContext:" + name, timer.time());
        this.exchangeRecorder.recordExchangeBegin();
    }

    @Override
    protected void handleDoneEvent(CamelEvent.ExchangeEvent doneEvent) {
        Exchange exchange = doneEvent.getExchange();
        String name = getNamingStrategy().getName(exchange, exchange.getFromEndpoint());
        exchange.removeProperty("eventTimer:" + name);
        Timer.Context context = (Timer.Context) exchange.removeProperty("eventTimerContext:" + name);
        if (context != null) {
            context.stop();
        }

        this.exchangeRecorder.recordExchangeComplete(exchange);
    }
}
