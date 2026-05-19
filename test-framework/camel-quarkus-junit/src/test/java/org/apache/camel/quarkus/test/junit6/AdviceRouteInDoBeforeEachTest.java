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
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.quarkus.test.CamelQuarkusTestSupport;
import org.junit.jupiter.api.Test;

import static org.apache.camel.builder.Builder.simple;

/**
 * Test that verifies using adviceRoute() in doBeforeEach() works correctly.
 * <p>
 * While using adviceRoute() in doBeforeEach() works, it's not necessary since routes
 * aren't started yet. Using AdviceWith.adviceWith() directly is cleaner in this context.
 * <p>
 * This test exists to verify that if users mistakenly use adviceRoute() in doBeforeEach(),
 * it still works correctly:
 * <ol>
 * <li>adviceRoute() stops, advises, and starts the route</li>
 * <li>Auto-start runs afterward but skips routes that are already started</li>
 * <li>Advice cleanup still works based on route definition comparison</li>
 * </ol>
 */
@QuarkusTest
@TestProfile(AdviceRouteInDoBeforeEachTest.class)
public class AdviceRouteInDoBeforeEachTest extends CamelQuarkusTestSupport {

    @Override
    protected void doBeforeEach(QuarkusTestMethodContext context) throws Exception {
        // Using adviceRoute() in doBeforeEach - works but not necessary
        // (direct AdviceWith is cleaner here since routes aren't started yet)
        adviceRoute("test-route", route -> {
            route.weaveByToUri("seda:*").replace().to("mock:seda-stub");
        });
    }

    @Override
    protected RoutesBuilder createRouteBuilder() {
        return new RouteBuilder() {
            @Override
            public void configure() {
                from("direct:input1")
                        .routeId("test-route")
                        .setBody(simple("Processed: ${body}"))
                        .to("seda:output")
                        .to("mock:result1");

                from("direct:input2")
                        .routeId("other-route")
                        .setBody(simple("Direct: ${body}"))
                        .to("mock:result2");
            }
        };
    }

    @Test
    void testAdvisedRoute() throws Exception {
        MockEndpoint sedaStub = getMockEndpoint("mock:seda-stub");
        MockEndpoint result = getMockEndpoint("mock:result1");

        sedaStub.expectedMessageCount(1);
        sedaStub.expectedBodiesReceived("Processed: test1");
        result.expectedMessageCount(1);

        template.sendBody("direct:input1", "test1");

        sedaStub.assertIsSatisfied();
        result.assertIsSatisfied();
    }

    @Test
    void testNonAdvisedRouteStillWorks() throws Exception {
        // Verifies auto-start worked for other-route even though we used adviceRoute()
        MockEndpoint result = getMockEndpoint("mock:result2");

        result.expectedMessageCount(1);
        result.expectedBodiesReceived("Direct: test2");

        template.sendBody("direct:input2", "test2");

        result.assertIsSatisfied();
    }
}
