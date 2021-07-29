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
package org.apache.camel.quarkus.eip.it;

import javax.enterprise.inject.Produces;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.ClaimCheckOperation;
import org.apache.camel.processor.loadbalancer.RoundRobinLoadBalancer;

public class EipRoutes extends RouteBuilder {

    public static final int THROTTLE_PERIOD = 500;
    public static final int THROTTLE_MAXIMUM_REQUEST_COUNT = 2;
    public static final int WEIGHTED_1 = 2;
    public static final int WEIGHTED_2 = 1;

    @Override
    public void configure() {
        from("direct:claimCheckByHeader")
                .claimCheck(ClaimCheckOperation.Set, "${header.claimCheckId}")
                .transform().constant("Bye World")
                .to("mock:claimCheckByHeader")
                .claimCheck(ClaimCheckOperation.Get, "${header.claimCheckId}")
                .to("mock:claimCheckByHeader")
                .transform().constant("Hi World")
                .to("mock:claimCheckByHeader")
                .claimCheck(ClaimCheckOperation.Get, "${header.claimCheckId}")
                .to("mock:claimCheckByHeader");

        from("direct:customLoadBalancer")
                .loadBalance().custom("roundRobin")
                .to("mock:customLoadBalancer1", "mock:customLoadBalancer2");

        from("direct:roundRobinLoadBalancer")
                .loadBalance().roundRobin()
                .to("mock:roundRobinLoadBalancer1", "mock:roundRobinLoadBalancer2");

        from("direct:stickyLoadBalancer")
                .loadBalance().sticky(header("stickyKey"))
                .to("mock:stickyLoadBalancer1", "mock:stickyLoadBalancer2");

        from("direct:weightedLoadBalancer")
                .loadBalance().weighted(false, "" + WEIGHTED_1 + "," + WEIGHTED_2)
                .to("mock:weightedLoadBalancer1", "mock:weightedLoadBalancer2");

        from("direct:enrich")
                .enrich("direct:prepend-hello");

        from("direct:prepend-hello")
                .setBody(body().prepend("Hello "));

        from("direct:failover")
                .loadBalance()
                .failover(MyException.class)
                .to("direct:failover1", "direct:failover2");
        from("direct:failover1").throwException(new MyException());
        from("direct:failover2").setBody(body().prepend("Hello from failover2 "));

        from("direct:loop")
                .loop(3)
                .to("mock:loop");

        from("direct:multicast").multicast().parallelProcessing().to("mock:multicast1", "mock:multicast2", "mock:multicast3");

        from("direct:recipientList").recipientList(constant("mock:recipientList1,mock:recipientList2,mock:recipientList3"));

        from("direct:removeHeader").removeHeader("headerToRemove").to("mock:removeHeader");

        from("direct:removeHeaders").removeHeaders("headerToRemove.*").to("mock:removeHeaders");

        final Processor headersToProperties = e -> {
            e.getMessage().getHeaders().entrySet().stream()
                    .filter(en -> en.getKey().contains("roperty"))
                    .forEach(en -> e.getProperties().put(en.getKey(), en.getValue()));
            ;
        };
        from("direct:removeProperty")
                .process(headersToProperties)
                .removeProperty("propertyToRemove")
                .to("mock:removeProperty");

        from("direct:removeProperties")
                .process(headersToProperties)
                .removeProperties("propertyToRemove.*")
                .to("mock:removeProperties");

        from("direct:routingSlip")
                .routingSlip(header("routingSlipHeader"));

        from("direct:sample")
                .sample()
                .to("mock:sample");

        from("direct:step")
                .step("foo")
                .setBody(e -> "Hello " + e.getMessage().getBody(String.class) + " from step!");

        from("direct:resequenceStream")
                .resequence(header("seqno"))
                .stream().capacity(4).timeout(3000)
                .to("mock:resequenceStream");

        from("direct:threads")
                .threads(2)
                .setBody(e -> "Hello from thread " + Thread.currentThread().getName());

        from("direct:throttle")
                .throttle(THROTTLE_MAXIMUM_REQUEST_COUNT).timePeriodMillis(THROTTLE_PERIOD).rejectExecution(true)
                .to("mock:throttle");

        from("direct:tryCatchFinally")
                .doTry()
                .process(e -> {
                    String body = e.getMessage().getBody(String.class);
                    if ("throw".equals(body)) {
                        throw new MyException();
                    } else {
                        e.getMessage().setBody("Hello " + body);
                    }
                })
                .doCatch(MyException.class)
                .setBody(e -> "Caught " + e.getMessage().getBody(String.class))
                .doFinally()
                .setBody(e -> "Handled by finally: " + e.getMessage().getBody(String.class));

    }

    @Produces
    @Singleton
    @Named("roundRobin")
    RoundRobinLoadBalancer roundRobinLoadBalancer() {
        return new RoundRobinLoadBalancer();
    }

    public static class MyException extends RuntimeException {

    }
}
