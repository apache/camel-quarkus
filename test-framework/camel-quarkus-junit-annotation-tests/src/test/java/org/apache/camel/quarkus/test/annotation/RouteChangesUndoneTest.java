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
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.quarkus.test.CamelQuarkusTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs after {@link AdvicesTest}, {@link HelperAdviceRouteTest} and {@link ReplaceFromTest} in the same application, by
 * class name order.
 */
@CamelQuarkusTest
class RouteChangesUndoneTest {

    @Inject
    CamelContext context;

    @Inject
    ProducerTemplate template;

    @EndpointInject("mock:annotationApp")
    MockEndpoint app;

    @EndpointInject("mock:annotationAdvised")
    MockEndpoint advised;

    @Test
    void applicationRouteIsBackToItsOriginalShape() throws Exception {
        assertThat(((ModelCamelContext) context).getRouteDefinition(AnnotationApplicationRoutes.ROUTE_ID).getOutputs())
                .hasSize(3);
        assertThat(context.getRoute(AnnotationApplicationRoutes.ROUTE_ID).getEndpoint().getEndpointUri())
                .isEqualTo("direct://annotationApp");

        app.expectedBodiesReceived("Hello World");
        advised.expectedMessageCount(0);
        template.sendBody("direct:annotationApp", "World");
        app.assertIsSatisfied();
        advised.assertIsSatisfied();
    }
}
