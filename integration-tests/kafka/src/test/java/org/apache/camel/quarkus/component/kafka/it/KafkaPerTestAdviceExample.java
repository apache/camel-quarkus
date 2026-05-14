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
import org.apache.camel.quarkus.component.kafka.CamelKafkaRoutes;
import org.apache.camel.quarkus.test.CamelQuarkusTestSupport;
import org.apache.camel.quarkus.test.support.kafka.KafkaTestResource;
import org.junit.jupiter.api.Test;

import static org.apache.camel.builder.Builder.constant;
import static org.apache.camel.builder.Builder.simple;

/**
 * Example: Applying different AdviceWith per test method using the {@code adviceRoute()} utility.
 * <p>
 * This test advises an EXISTING route (defined in CamelKafkaRoutes.java)
 * with DIFFERENT advice in each test method.
 * <p>
 * This demonstrates using different advice per test without manual route lifecycle management.
 */
@QuarkusTest
@QuarkusTestResource(KafkaTestResource.class)
public class KafkaPerTestAdviceExample extends CamelQuarkusTestSupport {

    private static final String TOPIC = "per-test-advice-topic";

    @Test
    void testAdviceToMock() throws Exception {
        // Advise an existing route with the first variation
        adviceRoute("per-test-advice-route", route -> {
            route.weaveByToUri("kafka:per-test-output-topic*").replace().to("mock:result1");
        });

        // Now test with the advised route
        MockEndpoint result = getMockEndpoint("mock:result1");
        String messageBody = "Test message " + UUID.randomUUID();

        result.expectedMessageCount(1);
        result.expectedBodiesReceived("Processed: " + messageBody);

        template.sendBody("kafka:" + TOPIC, messageBody);

        result.assertIsSatisfied(10000);
    }

    @Test
    void testAdviceToDifferentMock() throws Exception {
        // Apply DIFFERENT advice to the same existing route
        adviceRoute("per-test-advice-route", route -> {
            // Different advice: replace with a different mock and add a transform
            route.weaveByToUri("kafka:per-test-output-topic*").replace()
                    .transform(simple("TRANSFORMED: ${body}"))
                    .to("mock:result2");
        });

        // Test with the differently advised route
        MockEndpoint result = getMockEndpoint("mock:result2");
        String messageBody = "Another message " + UUID.randomUUID();

        result.expectedMessageCount(1);
        result.expectedBodiesReceived("TRANSFORMED: Processed: " + messageBody);

        template.sendBody("kafka:" + TOPIC, messageBody);

        result.assertIsSatisfied(10000);
    }

    @Test
    void testAdviceToAddStep() throws Exception {
        // Yet another different advice variation on the same existing route
        adviceRoute("per-test-advice-route", route -> {
            route.weaveByToUri("kafka:per-test-output-topic*")
                    .before()
                    .setHeader("AdviceApplied", constant(true))
                    .to("mock:intercepted");

            route.weaveByToUri("kafka:per-test-output-topic*").replace().to("mock:result3");
        });

        MockEndpoint intercepted = getMockEndpoint("mock:intercepted");
        MockEndpoint result = getMockEndpoint("mock:result3");
        String messageBody = "Third message " + UUID.randomUUID();

        intercepted.expectedMessageCount(1);
        intercepted.expectedHeaderReceived("AdviceApplied", true);
        result.expectedMessageCount(1);

        template.sendBody("kafka:" + TOPIC, messageBody);

        intercepted.assertIsSatisfied(10000);
        result.assertIsSatisfied(10000);
    }
}
