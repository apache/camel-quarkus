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
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.quarkus.test.CamelQuarkusTestSupport;
import org.junit.jupiter.api.Test;

import static org.apache.camel.builder.Builder.constant;
import static org.apache.camel.builder.Builder.simple;

/**
 * Test demonstrating the adviceRoute() utility method for applying different
 * advice per test method. This utility handles route lifecycle (stop, advice, start)
 * automatically, making per-test-method advice much cleaner.
 */
@QuarkusTest
@TestProfile(AdviceRouteUtilityTest.class)
public class AdviceRouteUtilityTest extends CamelQuarkusTestSupport {

    @Override
    protected RoutesBuilder createRouteBuilder() {
        return new RouteBuilder() {
            @Override
            public void configure() {
                from("direct:input")
                        .routeId("test-route")
                        .setBody(simple("Processed: ${body}"))
                        .to("seda:output")
                        .to("mock:result");
            }
        };
    }

    @Test
    void testAdviceToReplaceSeda() throws Exception {
        // Use adviceRoute() utility - it handles stop/advice/start automatically
        adviceRoute("test-route", route -> {
            route.weaveByToUri("seda:output").replace().to("mock:seda-stub");
        });

        MockEndpoint stub = getMockEndpoint("mock:seda-stub");
        MockEndpoint result = getMockEndpoint("mock:result");

        stub.expectedMessageCount(1);
        stub.expectedBodiesReceived("Processed: test1");
        result.expectedMessageCount(1);

        template.sendBody("direct:input", "test1");

        stub.assertIsSatisfied();
        result.assertIsSatisfied();
    }

    @Test
    void testAdviceToAddTransformation() throws Exception {
        // Different advice for this test - add a transformation step
        adviceRoute("test-route", route -> {
            route.weaveByToUri("seda:output").replace()
                    .transform(simple("TRANSFORMED: ${body}"))
                    .to("mock:seda-stub");
        });

        MockEndpoint stub = getMockEndpoint("mock:seda-stub");
        MockEndpoint result = getMockEndpoint("mock:result");

        stub.expectedMessageCount(1);
        stub.expectedBodiesReceived("TRANSFORMED: Processed: test2");
        result.expectedMessageCount(1);

        template.sendBody("direct:input", "test2");

        stub.assertIsSatisfied();
        result.assertIsSatisfied();
    }

    @Test
    void testAdviceToAddInterceptor() throws Exception {
        // Yet another different advice - add an interceptor before the endpoint
        adviceRoute("test-route", route -> {
            route.weaveByToUri("seda:output")
                    .before()
                    .setHeader("Intercepted", constant(true))
                    .to("mock:intercepted");

            route.weaveByToUri("seda:output").replace().to("mock:seda-stub");
        });

        MockEndpoint intercepted = getMockEndpoint("mock:intercepted");
        MockEndpoint stub = getMockEndpoint("mock:seda-stub");
        MockEndpoint result = getMockEndpoint("mock:result");

        intercepted.expectedMessageCount(1);
        intercepted.expectedHeaderReceived("Intercepted", true);
        stub.expectedMessageCount(1);
        result.expectedMessageCount(1);

        template.sendBody("direct:input", "test3");

        intercepted.assertIsSatisfied();
        stub.assertIsSatisfied();
        result.assertIsSatisfied();
    }

    @Test
    void testAdviceCleanupBetweenTests() throws Exception {
        // This test verifies that advice from previous tests was cleaned up
        // If advice wasn't cleaned up, mock:seda-stub would receive messages
        adviceRoute("test-route", route -> {
            // Just replace with a different mock to verify cleanup worked
            route.weaveByToUri("seda:output").replace().to("mock:fresh-stub");
        });

        MockEndpoint freshStub = getMockEndpoint("mock:fresh-stub");
        MockEndpoint result = getMockEndpoint("mock:result");

        freshStub.expectedMessageCount(1);
        result.expectedMessageCount(1);

        template.sendBody("direct:input", "test4");

        freshStub.assertIsSatisfied();
        result.assertIsSatisfied();
    }
}
