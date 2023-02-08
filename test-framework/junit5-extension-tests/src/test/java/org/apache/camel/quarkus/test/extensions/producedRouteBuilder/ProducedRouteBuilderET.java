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
package org.apache.camel.quarkus.test.extensions.producedRouteBuilder;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.enterprise.inject.Produces;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.quarkus.test.CamelQuarkusTestSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class ProducedRouteBuilderET extends CamelQuarkusTestSupport {

    @BeforeEach
    public void doSomethingBefore() throws Exception {
        AdviceWith.adviceWith(this.context, "sampleRoute",
                ProducedRouteBuilderET::enhanceRoute);
    }

    @Produces
    public RouteBuilder routes() {
        return new RouteBuilder() {
            @Override
            public void configure() {
                from("direct:in").routeId("sampleRoute").to("file:target/data/RouteBuilderET?filename=hello_true.txt");
            }
        };
    }

    @Test
    public void firstTest() throws Exception {
        Assertions.assertTrue(true);
    }

    @Test
    public void secondTest() throws Exception {
        Assertions.assertTrue(true);
    }

    private static void enhanceRoute(AdviceWithRouteBuilder route) {
        route.replaceFromWith("direct:ftp");
        route.interceptSendToEndpoint("file:.*samples.*")
                .skipSendToOriginalEndpoint()
                .to("mock:file:sample_requests");
    }

}
