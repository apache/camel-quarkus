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
import org.apache.camel.NamedNode;
import org.apache.camel.component.microprofile.metrics.route.policy.MicroProfileMetricsRoutePolicyFactory;
import org.apache.camel.spi.RoutePolicy;

/**
 * Handle MicroProfile metrics API incompatibility between versions used
 * by Camel <= 3.0.0 RC3 and Quarkus >= 0.26
 *
 * TODO: Remove this when upgrading to Camel > 3.0.0 RC3
 */
public class CamelQuarkusMicroProfileMetricsRoutePolicyFactory extends MicroProfileMetricsRoutePolicyFactory {

    @Override
    public RoutePolicy createRoutePolicy(CamelContext camelContext, String routeId, NamedNode routeDefinition) {
        CamelQuarkusMicroProfileMetricsRoutePolicy answer = new CamelQuarkusMicroProfileMetricsRoutePolicy();
        answer.setMetricRegistry(getMetricRegistry());
        answer.setNamingStrategy(getNamingStrategy());
        return answer;
    }
}
