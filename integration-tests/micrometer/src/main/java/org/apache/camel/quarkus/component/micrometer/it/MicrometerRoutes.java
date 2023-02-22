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
package org.apache.camel.quarkus.component.micrometer.it;

import org.apache.camel.builder.RouteBuilder;

import static org.apache.camel.component.micrometer.MicrometerConstants.HEADER_HISTOGRAM_VALUE;

public class MicrometerRoutes extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        from("direct:counter")
                .to("micrometer:counter:camel-quarkus-counter");

        from("direct:summary")
                .setHeader(HEADER_HISTOGRAM_VALUE, simple("${body}"))
                .to("micrometer:summary:camel-quarkus-summary");

        from("direct:timer")
                .id("micrometer-metrics-timer")
                .to("micrometer:timer:camel-quarkus-timer?action=start")
                .delay(100)
                .to("micrometer:timer:camel-quarkus-timer?action=stop");

        from("direct:log").routeId("log")
                .log("Camel Quarkus Micrometer");

        from("direct:annotatedBean")
                .choice()
                .when(simple("${header.number} == 1")).bean("testMetric", "call1")
                .otherwise().bean("testMetric", "call2")
                .end();

    }
}
