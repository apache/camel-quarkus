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
import org.apache.camel.EndpointInject;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.quarkus.test.CamelQuarkusTest;
import org.apache.camel.quarkus.test.CamelTestHelper;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@CamelQuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class HelperAdviceRouteTest {

    @Inject
    CamelTestHelper helper;

    @Inject
    ProducerTemplate template;

    @EndpointInject("mock:annotationApp")
    MockEndpoint app;

    @Test
    @Order(1)
    void adviceAppliedInTestMethod() throws Exception {
        helper.adviceRoute(AnnotationApplicationRoutes.ROUTE_ID, route -> route.weaveAddLast().to("mock:annotationHelper"));

        MockEndpoint mock = helper.getMockEndpoint("mock:annotationHelper");
        mock.expectedBodiesReceived("Hello World");
        app.expectedBodiesReceived("Hello World");
        template.sendBody("direct:annotationApp", "World");
        mock.assertIsSatisfied();
        app.assertIsSatisfied();
    }

    @Test
    @Order(2)
    void adviceIsUndoneAfterTestMethod() throws Exception {
        MockEndpoint mock = helper.getMockEndpoint("mock:annotationHelper");
        mock.expectedMessageCount(0);
        app.expectedBodiesReceived("Hello Camel");

        template.sendBody("direct:annotationApp", "Camel");

        app.assertIsSatisfied();
        mock.assertIsSatisfied();
    }
}
