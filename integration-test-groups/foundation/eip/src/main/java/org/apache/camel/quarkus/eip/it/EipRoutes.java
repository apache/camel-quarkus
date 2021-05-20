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

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.ClaimCheckOperation;
import org.apache.camel.processor.loadbalancer.RoundRobinLoadBalancer;

public class EipRoutes extends RouteBuilder {

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

        from("direct:enrich")
                .enrich("direct:prepend-hello");

        from("direct:prepend-hello")
                .setBody(body().prepend("Hello "));

    }

    @Produces
    @Singleton
    @Named("roundRobin")
    RoundRobinLoadBalancer roundRobinLoadBalancer() {
        return new RoundRobinLoadBalancer();
    }
}
