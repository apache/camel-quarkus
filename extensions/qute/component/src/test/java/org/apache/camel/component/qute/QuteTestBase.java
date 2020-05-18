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
package org.apache.camel.component.qute;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.InvalidPayloadException;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.Builder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.builder.ValueBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;

public abstract class QuteTestBase {

    protected volatile ProducerTemplate template;
    protected volatile CamelContext context;

    @BeforeEach
    void beforeEach() throws Exception {
        context = new DefaultCamelContext();
        context.getShutdownStrategy().setTimeout(10);
        template = context.createProducerTemplate();
        template.start();

        context.addRoutes(createRouteBuilder());
        context.start();
    }

    @AfterEach
    void afterEach() {
        if (template != null) {
            template.stop();
        }
        if (context != null) {
            context.stop();
        }
    }

    protected MockEndpoint getMockEndpoint(String endpointUri) {
        return context.getEndpoint(endpointUri, MockEndpoint.class);
    }

    protected void assertMockEndpointsSatisfied() throws InterruptedException {
        MockEndpoint.assertIsSatisfied(context);
    }

    protected static ValueBuilder body() {
        return Builder.body();
    }

    /**
     * Asserts that the given exchange has an OUT message of the given body value
     *
     * @param  exchange                the exchange which should have an OUT message
     * @param  expected                the expected value of the OUT message
     * @throws InvalidPayloadException is thrown if the payload is not the expected class type
     */
    protected static void assertOutMessageBodyEquals(Exchange exchange, Object expected) throws InvalidPayloadException {
        Assertions.assertNotNull(exchange, "Should have a response exchange!");

        Object actual;
        if (expected == null) {
            actual = exchange.getOut().getMandatoryBody();
            Assertions.assertEquals(expected, actual, "output body of: " + exchange);
        } else {
            actual = exchange.getOut().getMandatoryBody(expected.getClass());
        }
        Assertions.assertEquals(expected, actual, "output body of: " + exchange);

    }

    protected RoutesBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() {
                // no routes added by default
            }
        };
    }

}
