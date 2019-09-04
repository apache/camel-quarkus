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
package org.apache.camel.quarkus.component.microprofile.metrics.it;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.microprofile.metrics.event.notifier.exchange.MicroProfileMetricsExchangeEventNotifier;
import org.apache.camel.component.microprofile.metrics.event.notifier.route.MicroProfileMetricsRouteEventNotifier;
import org.apache.camel.component.microprofile.metrics.message.history.MicroProfileMetricsMessageHistoryFactory;
import org.apache.camel.component.microprofile.metrics.route.policy.MicroProfileMetricsRoutePolicyFactory;
import static org.apache.camel.component.microprofile.metrics.MicroProfileMetricsConstants.HEADER_HISTOGRAM_VALUE;
import static org.apache.camel.component.microprofile.metrics.MicroProfileMetricsConstants.HEADER_METER_MARK;

public class MicroProfileMetricsRouteBuilder extends RouteBuilder {
    @Override
    public void configure() {
        // Configure (optional) factories & event notifiers
        getContext().addRoutePolicyFactory(new MicroProfileMetricsRoutePolicyFactory());
        getContext().setMessageHistoryFactory(new MicroProfileMetricsMessageHistoryFactory());
        getContext().getManagementStrategy().addEventNotifier(new MicroProfileMetricsExchangeEventNotifier());
        getContext().getManagementStrategy().addEventNotifier(new MicroProfileMetricsRouteEventNotifier());

        from("direct:counter")
            .to("microprofile-metrics:counter:camel-quarkus-counter");

        from("direct:gaugeIncrement")
            .to("microprofile-metrics:concurrent gauge:camel-quarkus-gauge?gaugeIncrement=true");

        from("direct:gaugeDecrement")
            .to("microprofile-metrics:concurrent gauge:camel-quarkus-gauge?gaugeDecrement=true");

        from("direct:histogram")
            .setHeader(HEADER_HISTOGRAM_VALUE, simple("${body}"))
            .to("microprofile-metrics:histogram:camel-quarkus-histogram");

        from("direct:meter")
            .setHeader(HEADER_METER_MARK, simple("${body}"))
            .to("microprofile-metrics:meter:camel-quarkus-meter");

        from("direct:timer")
            .to("microprofile-metrics:timer:camel-quarkus-timer?action=start")
            .delay(100)
            .to("microprofile-metrics:timer:camel-quarkus-timer?action=stop");
    }
}
