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
import org.apache.camel.FluentProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.quarkus.test.CamelQuarkusTest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@CamelQuarkusTest(routes = NestedTest.Routes.class)
class NestedTest {

    @Inject
    CamelContext context;

    @Inject
    FluentProducerTemplate template;

    @Test
    void outerTest() {
        assertThat(template.to("direct:annotationNested").withBody("outer").request(String.class)).isEqualTo("Nested outer");
    }

    @Nested
    class Inner {

        @Test
        void innerTestSharesThePlanOfTheEnclosingClass() {
            assertThat(context.getRoutes()).filteredOn(route -> route.getRouteId().equals("annotation-nested"))
                    .hasSize(1);
            assertThat(template.to("direct:annotationNested").withBody("inner").request(String.class))
                    .isEqualTo("Nested inner");
        }
    }

    public static class Routes extends RouteBuilder {
        @Override
        public void configure() {
            from("direct:annotationNested")
                    .routeId("annotation-nested")
                    .transform().simple("Nested ${body}");
        }
    }
}
