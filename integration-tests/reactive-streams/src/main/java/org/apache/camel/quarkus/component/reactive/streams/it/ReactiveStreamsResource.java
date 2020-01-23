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
package org.apache.camel.quarkus.component.reactive.streams.it;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.camel.CamelContext;
import org.apache.camel.FluentProducerTemplate;
import org.apache.camel.component.reactive.streams.ReactiveStreamsComponent;
import org.apache.camel.component.reactive.streams.ReactiveStreamsEndpoint;
import org.apache.camel.component.reactive.streams.api.CamelReactiveStreamsService;
import org.apache.camel.component.reactive.streams.api.CamelReactiveStreamsServiceFactory;
import org.apache.camel.quarkus.component.reactive.streams.it.support.TestSubscriber;

@Path("/reactive-streams")
@ApplicationScoped
public class ReactiveStreamsResource {
    @Inject
    CamelContext camelContext;
    @Inject
    FluentProducerTemplate producerTemplate;
    @Inject
    CamelReactiveStreamsService reactiveStreamsService;
    @Inject
    CamelReactiveStreamsServiceFactory reactiveStreamsServiceFactory;

    @Path("/inspect")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject get() {
        ReactiveStreamsComponent component = camelContext.getComponent("reactive-streams", ReactiveStreamsComponent.class);
        ReactiveStreamsEndpoint endpoint = camelContext.getEndpointRegistry().values().stream()
                .filter(ReactiveStreamsEndpoint.class::isInstance)
                .map(ReactiveStreamsEndpoint.class::cast)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unable to find and endpoint of type ReactiveStreamsEndpoint"));

        return Json.createObjectBuilder()
                .add("reactive-streams-component-type", component.getClass().getName())
                .add("reactive-streams-component-backpressure-strategy", component.getBackpressureStrategy().toString())
                .add("reactive-streams-endpoint-backpressure-strategy", endpoint.getBackpressureStrategy().toString())
                .add("reactive-streams-service-type", reactiveStreamsService.getClass().getName())
                .add("reactive-streams-service-factory-type", reactiveStreamsServiceFactory.getClass().getName())
                .build();
    }

    @Path("/to-upper")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public String toUpper(String payload) throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<String> result = new AtomicReference<>();

        TestSubscriber<String> subscriber = TestSubscriber.onNext(data -> {
            result.set(data);
            latch.countDown();
        });

        subscriber.setInitiallyRequested(1);
        reactiveStreamsService.fromStream("toUpper", String.class).subscribe(subscriber);

        producerTemplate.to("direct:toUpper").withBody(payload).send();

        latch.await(5, TimeUnit.SECONDS);

        return result.get();
    }
}
