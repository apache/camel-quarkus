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
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.quarkus.test.AdviceRoute;
import org.apache.camel.quarkus.test.CamelQuarkusTest;
import org.junit.jupiter.api.Test;

@CamelQuarkusTest(advices = @AdviceRoute(route = AnnotationApplicationRoutes.ROUTE_ID, advice = AdvicesTest.AddMock.class))
class AdvicesTest {

    @Inject
    ProducerTemplate template;

    @EndpointInject("mock:annotationAdvised")
    MockEndpoint advised;

    @Test
    void applicationRouteIsAdvised() throws Exception {
        advised.expectedBodiesReceived("Hello World");
        template.sendBody("direct:annotationApp", "World");
        advised.assertIsSatisfied();
    }

    static class AddMock extends AdviceWithRouteBuilder {
        @Override
        public void configure() {
            weaveAddLast().to("mock:annotationAdvised");
        }
    }
}
