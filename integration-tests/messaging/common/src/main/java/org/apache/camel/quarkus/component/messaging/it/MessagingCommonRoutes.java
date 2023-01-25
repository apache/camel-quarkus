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
package org.apache.camel.quarkus.component.messaging.it;

import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.quarkus.component.messaging.it.util.scheme.ComponentScheme;

@ApplicationScoped
@RegisterForReflection(targets = IllegalStateException.class, serialization = true)
public class MessagingCommonRoutes extends RouteBuilder {

    @Inject
    ComponentScheme componentScheme;

    @Override
    public void configure() throws Exception {
        fromF("%s:queue:typeTest?concurrentConsumers=5", componentScheme)
                .toF("%s:queue:typeTestResult", componentScheme);

        String disableStreaming = "";
        if (isDisableStreaming()) {
            disableStreaming = "artemisStreamingEnabled=false";
        }
        fromF("%s:queue:typeTestResult?%s", componentScheme, disableStreaming)
                .to("mock:jmsType");

        // Map message type routes
        fromF("%s:queue:mapTest", componentScheme)
                .to("mock:mapResult");

        // JMS selector routes
        fromF("%s:queue:selectorA", componentScheme)
                .toF("%s:queue:selectorB", componentScheme);

        // JMS transaction tests
        fromF("%s:queue:txTest?transacted=true", componentScheme)
                .errorHandler(jtaTransactionErrorHandler().maximumRedeliveries(4))
                .transacted()
                .process(new Processor() {
                    private int count;

                    @Override
                    public void process(Exchange exchange) throws Exception {
                        if (++count <= 2) {
                            throw new IllegalStateException("Count less than 2 - retry...");
                        }
                        exchange.getMessage().setBody("JMS Transaction Success");
                        exchange.getMessage().setHeader("count", count);
                    }
                })
                .to("mock:txResult");

        fromF("%s:queue:objectTest", componentScheme)
                .to("mock:objectTestResult");

        // Topic routes
        fromF("%s:topic:test?clientId=123&durableSubscriptionName=camel-quarkus", componentScheme)
                .to("mock:topicResultA");

        fromF("%s:topic:test?clientId=456&durableSubscriptionName=camel-quarkus", componentScheme)
                .to("mock:topicResultB");

        fromF("%s:queue:resequence", componentScheme)
                // sort by body by allowing duplicates (message can have same JMSPriority)
                // and use reverse ordering so 9 is first output (most important), and 0 is last
                // use batch mode and fire every 3rd second
                .resequence(body()).batch().timeout(10000).allowDuplicates().reverse()
                .to("mock:resequence");

        from("direct:replyTo")
                .toF("%s:queue:replyQueueA?replyTo=replyQueueB&preserveMessageQos=true", componentScheme)
                .to("mock:replyToDone");

        fromF("%s:queue:replyQueueA", componentScheme)
                .to("mock:replyToStart")
                .transform(body().prepend("Hello "));

        fromF("%s:queue:replyQueueB?disableReplyTo=true", componentScheme)
                .to("mock:replyToEnd");

    }

    private boolean isDisableStreaming() {
        try {
            Class.forName("org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory");
            return !componentScheme.getScheme().startsWith("sjms");
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
