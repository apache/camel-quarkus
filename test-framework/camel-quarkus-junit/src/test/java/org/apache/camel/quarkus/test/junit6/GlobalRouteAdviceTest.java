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
package org.apache.camel.quarkus.test.junit6;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.junit.callback.QuarkusTestMethodContext;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.quarkus.test.CamelQuarkusTestSupport;
import org.junit.jupiter.api.Test;

/**
 * Test to reproduce issue #7264 - AdviceWith with global routes fails on second test.
 * This test uses doBeforeEach to apply advice, which should work across multiple tests.
 */
@QuarkusTest
@TestProfile(GlobalRouteAdviceTest.class)
public class GlobalRouteAdviceTest extends CamelQuarkusTestSupport {

    /**
     * Global routes can be simulated by NOT overriding isUseRouteBuilder (defaults to true)
     * but having the route already exist in the context before the test runs.
     * In this test, the route is created via the RouteBuilder and persists across test methods,
     * similar to how YAML routes or @Produces RouteBuilder beans behave.
     */

    @Override
    protected void doBeforeEach(QuarkusTestMethodContext context) throws Exception {
        // Apply advice to the route
        AdviceWith.adviceWith(this.context, "globalAdviceRoute", route -> {
            route.weaveAddLast().to("mock:advised");
        });
    }

    @Override
    protected RoutesBuilder createRouteBuilder() {
        return new RouteBuilder() {
            @Override
            public void configure() {
                from("direct:globalStart")
                        .routeId("globalAdviceRoute")
                        .log("Processing: ${body}")
                        .process(exchange -> {
                            String body = exchange.getIn().getBody(String.class);
                            exchange.getIn().setBody(body.toUpperCase());
                        })
                        .to("mock:globalResult");
            }
        };
    }

    @Test
    public void testFirstAdvice() throws Exception {
        MockEndpoint advised = getMockEndpoint("mock:advised");
        advised.expectedMessageCount(1);
        advised.expectedBodiesReceived("HELLO");

        template.sendBody("direct:globalStart", "hello");

        advised.assertIsSatisfied();
    }

    @Test
    public void testSecondAdvice() throws Exception {
        // This test works the same as the first one because automatic cleanup
        // removes the advice from the first test before this one runs
        MockEndpoint advised = getMockEndpoint("mock:advised");
        advised.expectedMessageCount(1);
        advised.expectedBodiesReceived("WORLD");

        template.sendBody("direct:globalStart", "world");

        advised.assertIsSatisfied();
    }
}
