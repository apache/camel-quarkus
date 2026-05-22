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
 * Test to reproduce issue #7264 - weaveById fails on second test when using global routes.
 * The processor ID cannot be found on subsequent tests because the route definition has been modified.
 */
@QuarkusTest
@TestProfile(GlobalRouteWeaveByIdTest.class)
public class GlobalRouteWeaveByIdTest extends CamelQuarkusTestSupport {

    @Override
    protected void doBeforeEach(QuarkusTestMethodContext context) throws Exception {
        // Use weaveById to replace a processor
        AdviceWith.adviceWith(this.context, "routeWithIds", route -> {
            route.weaveById("mockEndpoint").replace().to("mock:replaced");
        });
    }

    @Override
    protected RoutesBuilder createRouteBuilder() {
        return new RouteBuilder() {
            @Override
            public void configure() {
                from("direct:withIds")
                        .routeId("routeWithIds")
                        .log("Step 1").id("step1")
                        .transform().simple("Hello ${body}").id("transform1")
                        .to("mock:withIdsResult").id("mockEndpoint");
            }
        };
    }

    @Test
    public void testFirstWeaveById() throws Exception {
        MockEndpoint replaced = getMockEndpoint("mock:replaced");
        replaced.expectedMessageCount(1);
        replaced.expectedBodiesReceived("Hello World");

        template.sendBody("direct:withIds", "World");

        replaced.assertIsSatisfied();
    }

    @Test
    public void testSecondWeaveById() throws Exception {
        // This test will likely fail because the processor with id "mockEndpoint"
        // no longer exists after the first test replaced it
        MockEndpoint replaced = getMockEndpoint("mock:replaced");
        replaced.expectedMessageCount(1);
        replaced.expectedBodiesReceived("Hello Universe");

        template.sendBody("direct:withIds", "Universe");

        replaced.assertIsSatisfied();
    }
}
