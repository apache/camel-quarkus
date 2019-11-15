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

import org.apache.camel.Exchange;
import org.apache.camel.component.microprofile.metrics.MicroProfileMetricsExchangeRecorder;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetadataBuilder;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricType;
import org.eclipse.microprofile.metrics.Tag;
import static org.apache.camel.component.microprofile.metrics.MicroProfileMetricsConstants.EXCHANGES_INFLIGHT_DESCRIPTION;
import static org.apache.camel.component.microprofile.metrics.MicroProfileMetricsConstants.EXCHANGES_INFLIGHT_DISPLAY_NAME;
import static org.apache.camel.component.microprofile.metrics.MicroProfileMetricsConstants.EXCHANGES_INFLIGHT_METRIC_NAME;

/**
 * Handle MicroProfile metrics API incompatibility between versions used
 * by Camel <= 3.0.0 RC3 and Quarkus >= 0.26
 *
 * TODO: Remove this when upgrading to Camel > 3.0.0 RC3
 */
public class CamelQurakusMicroProfileMetricsExchangeRecorder extends MicroProfileMetricsExchangeRecorder {

    private CamelQuarkusAtomicIntegerGauge exchangesInflight = new CamelQuarkusAtomicIntegerGauge();

    public CamelQurakusMicroProfileMetricsExchangeRecorder(MetricRegistry metricRegistry, String metricName, Tag... tags) {
        super(metricRegistry, metricName, tags);
    }

    @Override
    protected void configureMetrics(MetricRegistry metricRegistry, String metricName, Tag... tags) {
        super.configureMetrics(metricRegistry, metricName, tags);

        Metadata exchangesInflightMetadata = new MetadataBuilder()
                .withName(metricName + EXCHANGES_INFLIGHT_METRIC_NAME)
                .withDisplayName(EXCHANGES_INFLIGHT_DISPLAY_NAME)
                .withDescription(EXCHANGES_INFLIGHT_DESCRIPTION)
                .withType(MetricType.GAUGE)
                .build();

        metricRegistry.remove(exchangesInflightMetadata.getName());

        this.exchangesInflight = metricRegistry.register(exchangesInflightMetadata, new CamelQuarkusAtomicIntegerGauge(), tags);
    }

    @Override
    public void recordExchangeBegin() {
        super.recordExchangeBegin();
        exchangesInflight.increment();
    }

    @Override
    public void recordExchangeComplete(Exchange exchange) {
        super.recordExchangeComplete(exchange);
        exchangesInflight.decrement();
    }
}
