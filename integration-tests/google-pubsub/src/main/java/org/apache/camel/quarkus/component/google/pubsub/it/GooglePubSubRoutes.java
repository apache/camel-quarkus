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
package org.apache.camel.quarkus.component.google.pubsub.it;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.processor.aggregate.GroupedExchangeAggregationStrategy;

import static org.apache.camel.component.google.pubsub.GooglePubsubConstants.ORDERING_KEY;

@ApplicationScoped
public class GooglePubSubRoutes extends RouteBuilder {

    public static final String GROUP_DIRECT_AGGREGATOR = "direct:grouped_id";
    public static final String ORDERING_DIRECT_IN = "direct:ordering_in";
    public static final String ACK_DIRECT_IN = "direct:ack_in";
    public static final String ACK_MOCK_RESULT = "mock:ack_result";

    @Produces
    @Named("ackFailing")
    AcKFailing acKFailing = new AcKFailing(false);

    @Override
    public void configure() {

        from(GROUP_DIRECT_AGGREGATOR)
                .aggregate(new GroupedExchangeAggregationStrategy()).constant(true)
                .completionSize(2).completionTimeout(10000L)
                .to("google-pubsub:{{project.id}}:{{google-pubsub.grouped-topic-name}}");

        from(ORDERING_DIRECT_IN)
                .log("processing ordering exchange, body: ${body}")
                .setHeader(ORDERING_KEY, constant("orderkey"))
                .log("Header was set, sending to google-pubsub:{{project.id}}:{{google-pubsub.ordering-topic-name}}")
                .to("google-pubsub:{{project.id}}:{{google-pubsub.ordering-topic-name}}?messageOrderingEnabled=true&pubsubEndpoint=pubsub.googleapis.com:443");

        from(ACK_DIRECT_IN)
                .to("google-pubsub:{{project.id}}:{{google-pubsub.ack-topic-name}}");

        from("google-pubsub:{{project.id}}:{{google-pubsub.ack-subscription-name}}?synchronousPull=true")
                .routeId("Fail_Receive").autoStartup(true).process(exchange -> {
                    if (acKFailing.isFail()) {
                        Thread.sleep(750);
                        throw new Exception("fail");
                    }
                }).to(ACK_MOCK_RESULT);
    }

    static class AcKFailing {

        volatile boolean fail;

        public AcKFailing(boolean fail) {
            this.fail = fail;
        }

        public boolean isFail() {
            return fail;
        }

        public void setFail(boolean fail) {
            this.fail = fail;
        }
    }
}
