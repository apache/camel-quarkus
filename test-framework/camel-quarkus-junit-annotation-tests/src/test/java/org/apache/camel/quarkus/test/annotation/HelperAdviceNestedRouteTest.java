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

/**
 * Advice on a node that is not a direct output of the route, such as the output of a filter.
 */
@CamelQuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class HelperAdviceNestedRouteTest {

    @Inject
    ProducerTemplate template;

    @Inject
    CamelTestHelper helper;

    @EndpointInject("mock:annotationAppNested")
    MockEndpoint original;

    @EndpointInject("mock:annotationNestedAdvised")
    MockEndpoint advised;

    @Test
    @Order(1)
    void nestedNodeIsAdvised() throws Exception {
        helper.adviceRoute(AnnotationApplicationRoutes.NESTED_ROUTE_ID,
                advice -> advice.weaveByToUri("mock:annotationAppNested").replace().to("mock:annotationNestedAdvised"));

        advised.expectedBodiesReceived("first");
        original.expectedMessageCount(0);
        template.sendBody("direct:annotationAppNested", "first");
        advised.assertIsSatisfied();
        original.assertIsSatisfied();
    }

    @Test
    @Order(2)
    void nestedAdviceIsUndone() throws Exception {
        original.expectedBodiesReceived("second");
        advised.expectedMessageCount(0);
        template.sendBody("direct:annotationAppNested", "second");
        original.assertIsSatisfied();
        advised.assertIsSatisfied();
    }
}
