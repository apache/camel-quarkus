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
import org.apache.camel.EndpointInject;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.quarkus.test.CamelQuarkusTest;
import org.junit.jupiter.api.Test;

@CamelQuarkusTest(routes = IsolatedMockEndpointsAndSkipTest.Routes.class, mockEndpointsAndSkip = "direct:annotationSkipFoo")
@TestProfile(IsolatedMockEndpointsAndSkipTest.class)
public class IsolatedMockEndpointsAndSkipTest implements QuarkusTestProfile {

    @Inject
    ProducerTemplate template;

    @EndpointInject("mock:direct:annotationSkipFoo")
    MockEndpoint foo;

    @EndpointInject("mock:annotationSkipResult")
    MockEndpoint result;

    @Test
    void endpointIsMockedAndSkipped() throws Exception {
        foo.expectedBodiesReceived("Hello");
        result.expectedBodiesReceived("Hello");

        template.sendBody("direct:annotationSkipStart", "Hello");

        foo.assertIsSatisfied();
        result.assertIsSatisfied();
    }

    public static class Routes extends RouteBuilder {
        @Override
        public void configure() {
            from("direct:annotationSkipStart").to("direct:annotationSkipFoo").to("mock:annotationSkipResult");
            from("direct:annotationSkipFoo").transform(constant("Bye"));
        }
    }
}
