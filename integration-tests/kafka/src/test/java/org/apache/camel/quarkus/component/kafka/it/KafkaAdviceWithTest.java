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
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.quarkus.test.CamelQuarkusTestSupport;
import org.apache.camel.quarkus.test.support.kafka.KafkaTestResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Demonstrates AdviceWith pattern with existing Kafka routes.
 * <p>
 * This test advises EXISTING routes (defined in CamelKafkaRoutes)
 * rather than creating new test routes. This avoids CamelContext state corruption.
 */
@QuarkusTest
@QuarkusTestResource(KafkaTestResource.class)
public class KafkaAdviceWithTest extends CamelQuarkusTestSupport {

    private static final String TOPIC_ADVISED = "advice-test-topic-1";
    private static final String TOPIC_NOT_ADVISED = "advice-test-topic-2";

    @BeforeEach
    void beforeEach() throws Exception {
        // Advise an EXISTING route (defined in CamelKafkaRoutes.java)
        AdviceWith.adviceWith(this.context, "advice-test-route-1", route -> {
            // Replace the specific Kafka producer with a mock
            // Use exact URI instead of wildcard to avoid matching other Kafka endpoints
            route.weaveByToUri("kafka:advice-output-topic-1*").replace().to("mock:kafka-advised");
        });
    }

    @Test
    void testAdvisedKafkaRoute() throws Exception {
        // The Kafka producer was replaced with mock:kafka-advised by the advice
        MockEndpoint kafkaStub = getMockEndpoint("mock:kafka-advised");
        // The route also has a built-in mock endpoint
        MockEndpoint result = getMockEndpoint("mock:advice-result-1");

        String messageBody = "Test message " + UUID.randomUUID();

        kafkaStub.expectedMessageCount(1);
        kafkaStub.expectedBodiesReceived("Processed: " + messageBody);
        result.expectedMessageCount(1);

        // Send message to Kafka topic
        template.sendBody("kafka:" + TOPIC_ADVISED, messageBody);

        // Verify the message was received and processed
        kafkaStub.assertIsSatisfied(10000);
        result.assertIsSatisfied(10000);
    }

    @Test
    void testNonAdvisedKafkaRouteStillWorks() throws Exception {
        // This verifies that advice-test-route-2 continues to work even though we advised route-1
        MockEndpoint result = getMockEndpoint("mock:advice-result-2");

        String messageBody = "Test message " + UUID.randomUUID();

        result.expectedMessageCount(1);
        result.expectedBodiesReceived("Processed: " + messageBody);

        // Send message to Kafka topic
        template.sendBody("kafka:" + TOPIC_NOT_ADVISED, messageBody);

        // Verify the message was received and processed
        result.assertIsSatisfied(10000);
    }

    @Test
    void testMultipleMessagesOnAdvisedRoute() throws Exception {
        // Test that the consumer continues to work properly across multiple messages
        MockEndpoint kafkaStub = getMockEndpoint("mock:kafka-advised");
        MockEndpoint result = getMockEndpoint("mock:advice-result-1");

        kafkaStub.expectedMessageCount(3);
        result.expectedMessageCount(3);

        // Send multiple messages
        for (int i = 0; i < 3; i++) {
            template.sendBody("kafka:" + TOPIC_ADVISED, "Message " + i);
        }

        kafkaStub.assertIsSatisfied(10000);
        result.assertIsSatisfied(10000);
    }
}
