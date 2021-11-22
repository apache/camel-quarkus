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
package org.apache.camel.quarkus.main;

import javax.enterprise.context.ApplicationScoped;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;

@ApplicationScoped
public class MainDisabledRoutesCDI extends RouteBuilder {

    private static final String EXCEPTION_MESSAGE = "Dummy exception thrown to trigger main-disabled-route-configuration-cdi";

    @Override
    public void configure() {
        from("direct:main-disabled-cdi")
                .routeConfigurationId("main-disabled-route-configuration-cdi")
                .id("main-disabled-cdi")
                .process(new Processor() {
                    @Override
                    public void process(Exchange exchange) throws MainDisabledRouteConfigurationException {
                        String body = exchange.getIn().getBody(String.class);
                        if ("main-disabled-cdi-exception".equals(body)) {
                            throw new MainDisabledRouteConfigurationException(EXCEPTION_MESSAGE);
                        }
                        exchange.getMessage()
                                .setBody("onException has NOT been triggered in main-disabled-route-configuration-cdi");
                    }
                });
    }
}
