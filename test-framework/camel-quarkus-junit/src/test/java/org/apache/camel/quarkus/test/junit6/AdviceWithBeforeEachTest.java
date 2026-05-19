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
import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.quarkus.test.CamelQuarkusTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.apache.camel.builder.Builder.simple;

/**
 * Demonstrates the recommended AdviceWith pattern using @BeforeEach.
 * <p>
 * This pattern is recommended when all tests in the class use the same advice.
 * Key features:
 * <ul>
 * <li>Routes that aren't advised are automatically started after @BeforeEach completes</li>
 * <li>Advice modifications are automatically cleaned up between test methods</li>
 * <li>No need to manually call startRouteDefinitions()</li>
 * </ul>
 */
@QuarkusTest
@TestProfile(AdviceWithBeforeEachTest.class)
public class AdviceWithBeforeEachTest extends CamelQuarkusTestSupport {

    @BeforeEach
    void beforeEach() throws Exception {
        // Apply advice to route1 only - route2 should be auto-started
        AdviceWith.adviceWith(this.context, "advised-route", route -> {
            // Replace the file endpoint with a mock so we can verify the message was processed
            route.weaveByToUri("file:*").replace().to("mock:file-stub");
        });
    }

    @Override
    protected RoutesBuilder createRouteBuilder() {
        return new RouteBuilder() {
            @Override
            public void configure() {
                // Route 1: This route will be advised
                from("direct:input1")
                        .routeId("advised-route")
                        .setBody(simple("Processed: ${body}"))
                        .to("file:target/output")
                        .to("mock:result1");

                // Route 2: This route is NOT advised, but should still work due to auto-start
                from("direct:input2")
                        .routeId("non-advised-route")
                        .setBody(simple("Direct: ${body}"))
                        .to("mock:result2");
            }
        };
    }

    @Test
    void testAdvisedRoute() throws Exception {
        MockEndpoint fileStub = getMockEndpoint("mock:file-stub");
        MockEndpoint result = getMockEndpoint("mock:result1");

        fileStub.expectedMessageCount(1);
        fileStub.expectedBodiesReceived("Processed: message1");
        result.expectedMessageCount(1);

        template.sendBody("direct:input1", "message1");

        fileStub.assertIsSatisfied();
        result.assertIsSatisfied();
    }

    @Test
    void testNonAdvisedRouteStillWorks() throws Exception {
        // This verifies that non-advised routes are auto-started
        MockEndpoint result = getMockEndpoint("mock:result2");

        result.expectedMessageCount(1);
        result.expectedBodiesReceived("Direct: message2");

        template.sendBody("direct:input2", "message2");

        result.assertIsSatisfied();
    }

    @Test
    void testMultipleMessagesOnAdvisedRoute() throws Exception {
        // Test that the route continues to work properly across multiple messages
        MockEndpoint fileStub = getMockEndpoint("mock:file-stub");
        MockEndpoint result = getMockEndpoint("mock:result1");

        fileStub.expectedMessageCount(3);
        result.expectedMessageCount(3);

        // Send multiple messages
        for (int i = 0; i < 3; i++) {
            template.sendBody("direct:input1", "Message " + i);
        }

        fileStub.assertIsSatisfied();
        result.assertIsSatisfied();
    }

    @Test
    void testAdviceIsConsistentAcrossTests() throws Exception {
        // This test verifies that advice is consistently applied even after other tests run
        // If advice cleanup failed, this test might see unexpected behavior
        MockEndpoint fileStub = getMockEndpoint("mock:file-stub");
        MockEndpoint result = getMockEndpoint("mock:result1");

        fileStub.expectedMessageCount(1);
        fileStub.expectedBodiesReceived("Processed: consistent");
        result.expectedMessageCount(1);

        template.sendBody("direct:input1", "consistent");

        fileStub.assertIsSatisfied();
        result.assertIsSatisfied();
    }
}
