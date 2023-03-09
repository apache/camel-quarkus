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

import jakarta.inject.Inject;

import io.smallrye.mutiny.Multi;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.reactive.streams.api.CamelReactiveStreamsService;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Outgoing;
import org.reactivestreams.Subscriber;

public class SubscriberRoute extends RouteBuilder {

    @Inject
    CamelReactiveStreamsService reactive;

    @Incoming("camel-route-sub")
    public Subscriber<String> sink() {
        return reactive.streamSubscriber("camel-sub", String.class);
    }

    @Outgoing("camel-route-sub")
    public Multi<String> source() {
        return Multi.createFrom().items("a", "b", "c", "d");
    }

    @Override
    public void configure() {
        from("reactive-streams:camel-sub")
                .to("file:./target?fileName=values.txt&fileExist=append");
    }
}
