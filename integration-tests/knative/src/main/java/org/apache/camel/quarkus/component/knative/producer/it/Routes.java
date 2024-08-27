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
package org.apache.camel.quarkus.component.knative.producer.it;

import jakarta.enterprise.context.ApplicationScoped;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.cloudevents.CloudEvent;

@ApplicationScoped
public class Routes extends RouteBuilder {

    private static final String TIME = "2018-04-05T17:31:00Z";

    @Override
    public void configure() throws Exception {
        // Routes using ProducerTemplate, need to specify the header CloudEvent.CAMEL_CLOUD_EVENT_SOURCE
        from("direct:channel")
                .setHeader(CloudEvent.CAMEL_CLOUD_EVENT_SOURCE, constant("camel"))
                .to("knative:channel/channel-test")
                .to("mock:channel");

        from("direct:event")
                .setHeader(CloudEvent.CAMEL_CLOUD_EVENT_SOURCE, constant("camel"))
                .to("knative:event/broker-test")
                .to("mock:event");

        from("direct:endpoint")
                .setHeader(CloudEvent.CAMEL_CLOUD_EVENT_SOURCE, constant("camel"))
                .to("knative:endpoint/endpoint-test")
                .to("mock:endpoint");

        // Routes not using ProducerTemplate, the cloud event source header is managed by the consumer
        from("timer:channelTimer?period=1&repeatCount=1")
                .setBody(constant("Hello World From channelTimer!"))
                .to("knative:channel/channel-test")
                .to("mock:channel-timer");

        from("timer:eventTimer?period=1&repeatCount=1")
                .setBody(constant("Hello World From eventTimer!"))
                .to("knative:event/broker-test")
                .to("mock:event-timer");

        from("timer:endpointTimer?period=1&repeatCount=1")
                .setBody(constant("Hello World From endpointTimer!"))
                .to("knative:endpoint/endpoint-test")
                .to("mock:endpoint-timer");
    }
}
