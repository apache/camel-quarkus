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
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.quarkus.component.messaging.it.util.scheme.ComponentScheme;

@ApplicationScoped
@RegisterForReflection(targets = IllegalStateException.class, serialization = true)
public class MessagingCommonRoutes extends RouteBuilder {

    @Inject
    ComponentScheme componentScheme;

    @Inject
    CamelContext camelContext;

    @Override
    public void configure() throws Exception {
        // Don't start the routes by default, for IBM MQ it is needed to create the destinations beforehand
        // The routes are later started in AbstractMessagingTest#beforeAll method
        camelContext.setAutoStartup(false);

        fromF("%s:queue:testJmsMessageType?concurrentConsumers=5", componentScheme)
                .toF("%s:queue:testJmsMessageType2", componentScheme);

        String disableStreaming = "";
        if (isDisableStreaming()) {
            disableStreaming = "artemisStreamingEnabled=false";
        }
        fromF("%s:queue:testJmsMessageType2?%s", componentScheme, disableStreaming)
                .to("mock:jmsType");

        // Map message type routes
        fromF("%s:queue:testJmsMapMessage", componentScheme)
                .id("hello")
                .to("mock:mapResult");

        // JMS selector routes
        fromF("%s:queue:testJmsSelector", componentScheme)
                .toF("%s:queue:testJmsSelector2", componentScheme);

        // JMS transaction tests
        fromF("%s:queue:testJmsTransaction?transacted=true", componentScheme)
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

        fromF("%s:queue:testJmsObject", componentScheme)
                .to("mock:objectTestResult");

        // Topic routes
        fromF("%s:topic:testJmsTopic?clientId=123&durableSubscriptionName=camel-quarkus", componentScheme)
                .to("mock:topicResultA");

        fromF("%s:topic:testJmsTopic?clientId=456&durableSubscriptionName=camel-quarkus", componentScheme)
                .to("mock:topicResultB");

        fromF("%s:queue:testResequence", componentScheme)
                // sort by body by allowing duplicates (message can have same JMSPriority)
                // and use reverse ordering so 9 is first output (most important), and 0 is last
                // use batch mode and fire every 3rd second
                .resequence(body()).batch().timeout(10000).allowDuplicates().reverse()
                .to("mock:resequence");

        from("direct:replyTo")
                .toF("%s:queue:testJmsReplyTo?replyTo=testJmsReplyTo2&preserveMessageQos=true", componentScheme)
                .to("mock:replyToDone");

        fromF("%s:queue:testJmsReplyTo", componentScheme)
                .to("mock:replyToStart")
                .transform(body().prepend("Hello "));

        fromF("%s:queue:testJmsReplyTo2?disableReplyTo=true", componentScheme)
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
