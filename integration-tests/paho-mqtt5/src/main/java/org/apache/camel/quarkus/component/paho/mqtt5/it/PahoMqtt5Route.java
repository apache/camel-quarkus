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

package org.apache.camel.quarkus.component.paho.mqtt5.it;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.Route;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.spi.SupervisingRouteController;
import org.apache.camel.support.RoutePolicySupport;
import org.eclipse.microprofile.config.ConfigProvider;

@ApplicationScoped
public class PahoMqtt5Route extends RouteBuilder {
    public static final String TESTING_ROUTE_ID = "testingRoute";

    @Inject
    Counter counter;

    @Override
    public void configure() throws Exception {
        SupervisingRouteController supervising = getCamelContext().getRouteController().supervising();
        supervising.setBackOffDelay(200);
        supervising.setIncludeRoutes("paho-mqtt5:*");

        from("direct:test").to("paho-mqtt5:queue?lazyStartProducer=true&brokerUrl=" + brokerUrl("tcp"));
        from("paho-mqtt5:queue?brokerUrl=" + brokerUrl("tcp"))
                .id(TESTING_ROUTE_ID)
                .routePolicy(new RoutePolicySupport() {
                    @Override
                    public void onStart(Route route) {
                        counter.countDown();
                    }
                })
                .to("mock:test");
    }

    private String brokerUrl(String protocol) {
        return ConfigProvider.getConfig().getValue("paho5.broker." + protocol + ".url", String.class);
    }
}
