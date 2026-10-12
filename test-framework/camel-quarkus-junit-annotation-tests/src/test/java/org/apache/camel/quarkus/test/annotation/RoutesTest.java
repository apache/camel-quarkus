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
package org.apache.camel.quarkus.test.annotation;

import jakarta.inject.Inject;
import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.quarkus.test.CamelQuarkusTest;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static org.assertj.core.api.Assertions.assertThat;

@CamelQuarkusTest(routes = RoutesTest.Routes.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RoutesTest {
    static final String ROUTE_ID = "annotation-routes";

    @Inject
    CamelContext context;

    @Inject
    ProducerTemplate template;

    @EndpointInject("mock:annotationRoutes")
    MockEndpoint result;

    @Test
    @Order(1)
    void routesAreAdded() throws Exception {
        assertThat(context.getRoute(ROUTE_ID)).isNotNull();
        assertThat(context.getRoute(RoutesOtherTest.ROUTE_ID)).isNull();

        result.expectedBodiesReceived("Hello World");
        template.sendBody("direct:annotationRoutes", "World");
        result.assertIsSatisfied();
    }

    @Test
    @Order(2)
    void mocksAreResetBetweenTestMethods() throws Exception {
        assertThat(result.getReceivedCounter()).isZero();

        result.expectedBodiesReceived("Hello Camel");
        template.sendBody("direct:annotationRoutes", "Camel");
        result.assertIsSatisfied();
    }

    public static class Routes extends RouteBuilder {
        @Override
        public void configure() {
            from("direct:annotationRoutes")
                    .routeId(ROUTE_ID)
                    .transform().simple("Hello ${body}")
                    .to("mock:annotationRoutes");
        }
    }
}
