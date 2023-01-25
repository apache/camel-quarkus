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
package org.apache.camel.quarkus.test.userTestCases;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.junit.callback.QuarkusTestMethodContext;
import jakarta.inject.Inject;
import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.quarkus.test.CamelQuarkusTestSupport;
import org.junit.jupiter.api.Test;

/**
 * Test for https://github.com/apache/camel-quarkus/issues/4362
 */
@QuarkusTest
@TestProfile(AdviceInDoBeforeEachMethodsTest.class)
public class AdviceInDoBeforeEachMethodsTest extends CamelQuarkusTestSupport {

    @Produce("direct:ftp")
    protected ProducerTemplate template;

    @Inject
    protected CamelContext context;

    @EndpointInject("mock:file:sample_requests")
    protected MockEndpoint fileMock;

    @Override
    protected RouteBuilder createRouteBuilder() {
        return new RouteBuilder() {
            public void configure() {
                from("direct:start")
                        .routeId("sampleRoute")
                        .to("file:samples/");
            }
        };
    }

    @Override
    protected void doBeforeEach(QuarkusTestMethodContext context) throws Exception {
        AdviceWith.adviceWith(this.context, "sampleRoute",
                AdviceInDoBeforeEachMethodsTest::enhanceRoute);
    }

    @Test
    void testConsumeFtpWriteToFileOne() throws Exception {
        fileMock.message(0).body().isEqualTo("Hello World");
        template.sendBody("direct:ftp", "Hello World");
        fileMock.assertIsSatisfied();
    }

    @Test
    void testConsumeFtpWriteToFileTwo() throws Exception {
        fileMock.message(0).body().isEqualTo("Hello World");
        template.sendBody("direct:ftp", "Hello World");
        fileMock.assertIsSatisfied();
    }

    private static void enhanceRoute(AdviceWithRouteBuilder route) {
        route.replaceFromWith("direct:ftp");
        route.interceptSendToEndpoint("file:.*samples.*")
                .skipSendToOriginalEndpoint()
                .to("mock:file:sample_requests");
    }
}
