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
package org.apache.camel.quarkus.component.kafka.it;

import java.util.UUID;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.callback.QuarkusTestMethodContext;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.quarkus.test.CamelQuarkusTestSupport;
import org.apache.camel.quarkus.test.support.kafka.KafkaTestResource;
import org.junit.jupiter.api.Test;

/**
 * Test that verifies using adviceRoute() in doBeforeEach() works correctly.
 * <p>
 * This test uses the {@code adviceRoute()} utility method to advise
 * EXISTING routes (defined in CamelKafkaRoutes) rather than creating new test routes.
 * <p>
 * The {@code adviceRoute()} method handles the stop/advise/start lifecycle automatically,
 * making it convenient for use in doBeforeEach().
 */
@QuarkusTest
@QuarkusTestResource(KafkaTestResource.class)
public class KafkaAdviceRouteInBeforeEachTest extends CamelQuarkusTestSupport {

    private static final String TOPIC_ADVISED = "advice-test-topic-1";
    private static final String TOPIC_NOT_ADVISED = "advice-test-topic-2";

    @Override
    protected void doBeforeEach(QuarkusTestMethodContext context) throws Exception {
        // Use adviceRoute() to advise an EXISTING route (from CamelKafkaRoutes.java)
        // The adviceRoute() utility handles stop/advise/start automatically
        adviceRoute("advice-test-route-1", route -> {
            // Use specific URI to avoid matching other Kafka endpoints
            route.weaveByToUri("kafka:advice-output-topic-1*").replace().to("mock:kafka-advised");
        });
    }

    @Test
    void testAdvisedRoute() throws Exception {
        MockEndpoint kafkaStub = getMockEndpoint("mock:kafka-advised");
        MockEndpoint result = getMockEndpoint("mock:advice-result-1");

        String messageBody = "Test message " + UUID.randomUUID();

        kafkaStub.expectedMessageCount(1);
        kafkaStub.expectedBodiesReceived("Processed: " + messageBody);
        result.expectedMessageCount(1);

        template.sendBody("kafka:" + TOPIC_ADVISED, messageBody);

        kafkaStub.assertIsSatisfied(10000);
        result.assertIsSatisfied(10000);
    }

    @Test
    void testNonAdvisedRouteStillWorks() throws Exception {
        // Verifies that advice-test-route-2 continues working even though we advised route-1
        MockEndpoint result = getMockEndpoint("mock:advice-result-2");

        String messageBody = "Test message " + UUID.randomUUID();

        result.expectedMessageCount(1);
        result.expectedBodiesReceived("Processed: " + messageBody);

        template.sendBody("kafka:" + TOPIC_NOT_ADVISED, messageBody);

        result.assertIsSatisfied(10000);
    }
}
