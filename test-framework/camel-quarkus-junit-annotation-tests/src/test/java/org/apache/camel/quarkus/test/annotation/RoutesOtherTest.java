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
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@CamelQuarkusTest(routes = RoutesOtherTest.Routes.class)
class RoutesOtherTest {
    static final String ROUTE_ID = "annotation-routes-other";
    static final String UNLISTED_ROUTE_ID = "annotation-routes-unlisted";

    @Inject
    CamelContext context;

    @Inject
    FluentProducerTemplate template;

    @Test
    void onlyTheRoutesOfThisClassAreAdded() {
        assertThat(context.getRoute(ROUTE_ID)).isNotNull();
        assertThat(context.getRoute(RoutesTest.ROUTE_ID)).isNull();
        assertThat(context.getRoute(UNLISTED_ROUTE_ID)).isNull();

        String reply = template.to("direct:annotationRoutesOther").withBody("World").request(String.class);
        assertThat(reply).isEqualTo("Bye World");
    }

    public static class Routes extends RouteBuilder {
        @Override
        public void configure() {
            from("direct:annotationRoutesOther")
                    .routeId(ROUTE_ID)
                    .transform().simple("Bye ${body}");
        }
    }

    /**
     * Not listed in {@link CamelQuarkusTest#routes()}, so never added.
     */
    public static class UnlistedRoutes extends RouteBuilder {
        @Override
        public void configure() {
            from("direct:annotationRoutesUnlisted").routeId(UNLISTED_ROUTE_ID).log("${body}");
        }
    }
}
