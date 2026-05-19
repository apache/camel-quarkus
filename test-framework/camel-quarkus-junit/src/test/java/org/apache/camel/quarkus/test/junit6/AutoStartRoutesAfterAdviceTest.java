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
 * Test to demonstrate that startRouteDefinitions() is no longer needed when using
 * AdviceWith in doBeforeEach(). Routes that aren't advised are automatically started.
 */
@QuarkusTest
@TestProfile(AutoStartRoutesAfterAdviceTest.class)
public class AutoStartRoutesAfterAdviceTest extends CamelQuarkusTestSupport {

    @Override
    protected void doBeforeEach(QuarkusTestMethodContext context) throws Exception {
        // Apply advice to route1 only - route2 should be auto-started
        AdviceWith.adviceWith(this.context, "route1", route -> {
            route.weaveByToUri("file:*").replace().to("mock:file");
        });

        // NOTE: No need to call startRouteDefinitions() anymore!
        // Routes that weren't advised (like route2) are automatically started
    }

    @Override
    protected RoutesBuilder createRouteBuilder() {
        return new RouteBuilder() {
            @Override
            public void configure() {
                from("direct:test1")
                        .routeId("route1")
                        .setBody(constant("Data 1"))
                        .to("file:target/output1")
                        .to("mock:result1");

                // This route is NOT advised, but should still work
                from("direct:test2")
                        .routeId("route2")
                        .setBody(constant("Data 2"))
                        .to("mock:result2");
            }
        };
    }

    @Test
    public void testAdvisedRoute() throws Exception {
        MockEndpoint fileStub = getMockEndpoint("mock:file");
        MockEndpoint result = getMockEndpoint("mock:result1");

        fileStub.expectedMessageCount(1);
        fileStub.expectedBodiesReceived("Data 1");
        result.expectedMessageCount(1);

        template.sendBody("direct:test1", "ignored");

        fileStub.assertIsSatisfied();
        result.assertIsSatisfied();
    }

    @Test
    public void testNonAdvisedRouteStillWorks() throws Exception {
        // This verifies that route2 was auto-started even though we only advised route1
        MockEndpoint result = getMockEndpoint("mock:result2");

        result.expectedMessageCount(1);
        result.expectedBodiesReceived("Data 2");

        template.sendBody("direct:test2", "ignored");

        result.assertIsSatisfied();
    }
}
