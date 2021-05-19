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

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.ErrorHandlerBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.jta.JtaTransactionErrorHandlerBuilder;
import org.apache.camel.processor.errorhandler.RedeliveryPolicy;

public class JmsRoutes extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        from("jms:queue:typeTest?concurrentConsumers=5")
                .to("jms:queue:typeTestResult");

        from("jms:queue:typeTestResult?artemisStreamingEnabled=false")
                .to("mock:jmsType");

        // Map message type routes
        from("jms:queue:mapTest")
                .to("mock:mapResult");

        // JMS selector routes
        from("jms:queue:selectorA")
                .to("jms:queue:selectorB");

        // JMS transaction tests
        from("jms:queue:txTest?transacted=true").errorHandler(setUpJtaErrorHandler())
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

        from("jms:queue:transferExchange?transferExchange=true")
                .to("mock:transferExchangeResult");

        from("jms:queue:objectTest")
                .to("mock:objectTestResult");

        // Topic routes
        from("jms:topic:test?clientId=123&durableSubscriptionName=camel-quarkus")
                .to("mock:topicResultA");

        from("jms:topic:test?clientId=456&durableSubscriptionName=camel-quarkus")
                .to("mock:topicResultB");

        from("jms:queue:resequence")
                // sort by body by allowing duplicates (message can have same JMSPriority)
                // and use reverse ordering so 9 is first output (most important), and 0 is last
                // use batch mode and fire every 3rd second
                .resequence(body()).batch().timeout(10000).allowDuplicates().reverse()
                .to("mock:resequence");

    }

    private ErrorHandlerBuilder setUpJtaErrorHandler() {
        JtaTransactionErrorHandlerBuilder builder = new JtaTransactionErrorHandlerBuilder();
        RedeliveryPolicy policy = new RedeliveryPolicy();
        policy.setMaximumRedeliveries(4);
        builder.setRedeliveryPolicy(policy);
        return builder;
    }
}
