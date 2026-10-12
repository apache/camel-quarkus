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

import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.quarkus.test.AdviceRoute;
import org.apache.camel.quarkus.test.CamelQuarkusTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Advice is applied before the route is created, so the advised route is the only one that ever started.
 */
@CamelQuarkusTest(advices = @AdviceRoute(route = AnnotationApplicationRoutes.ROUTE_ID, advice = IsolatedAdvicesTest.ReplaceConsumer.class), replaceRouteFromWith = AnnotationApplicationRoutes.ROUTE_ID
        + "=direct:annotationIsolatedReplaced")
@TestProfile(IsolatedAdvicesTest.class)
public class IsolatedAdvicesTest implements QuarkusTestProfile {

    @Inject
    CamelContext context;

    @Inject
    ProducerTemplate template;

    @EndpointInject("mock:annotationIsolatedAdvised")
    MockEndpoint advised;

    @Test
    void routeIsAdvisedBeforeItStarts() throws Exception {
        assertThat(context.hasEndpoint("direct:annotationApp")).isNull();

        advised.expectedBodiesReceived("Hello World");
        template.sendBody("direct:annotationIsolatedReplaced", "World");
        advised.assertIsSatisfied();
    }

    public static class ReplaceConsumer extends AdviceWithRouteBuilder {
        @Override
        public void configure() {
            weaveAddLast().to("mock:annotationIsolatedAdvised");
        }
    }
}
