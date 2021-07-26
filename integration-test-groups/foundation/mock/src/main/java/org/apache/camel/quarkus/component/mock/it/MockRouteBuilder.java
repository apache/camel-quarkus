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
package org.apache.camel.quarkus.component.mock.it;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockComponent;
import org.jboss.logging.Logger;

@ApplicationScoped
public class MockRouteBuilder extends RouteBuilder {
    private static final Logger LOG = Logger.getLogger(MockRouteBuilder.class);

    @Inject
    MockComponent mock;

    @Override
    public void configure() {
        from("direct:mockStart")
                .routeId("forMocking")
                .to("direct:mockFoo")
                .to("log:mockFoo")
                .to("mock:result");

        from("direct:mockFoo")
                .process(e -> LOG.info("mockFoo:" + e.getMessage().getBody(String.class)))
                .transform(constant("Bye World"));

        from("direct:cdiConfig")
                .setBody(e -> "mockComponent.log = " + mock.isLog());

    }
}
