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
package org.apache.camel.quarkus.component.smallrye.reactive.messaging.it;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import jakarta.inject.Inject;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.reactive.streams.api.CamelReactiveStreamsService;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Outgoing;
import org.reactivestreams.Publisher;

public class PublisherRoute extends RouteBuilder {

    @Inject
    CamelReactiveStreamsService camel;

    @Inject
    ResultsBean results;

    @Incoming("sink")
    public CompletionStage<Void> sink(String value) {
        results.addResult(value);
        return CompletableFuture.completedFuture(null);
    }

    @Incoming("camel-route-pub")
    @Outgoing("sink")
    public String extract(Exchange exchange) {
        return exchange.getMessage().getBody(String.class);
    }

    @Outgoing("camel-route-pub")
    public Publisher<Exchange> source() {
        return camel.fromStream("my-stream");
    }

    @Override
    public void configure() {
        from("direct:in")
                .process(exchange -> exchange.getMessage().setBody(exchange.getIn().getBody(String.class).toUpperCase()))
                .to("reactive-streams:my-stream");
    }
}
