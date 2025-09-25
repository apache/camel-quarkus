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
package org.apache.camel.quarkus.component.management.it;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;

public class Routes extends RouteBuilder {
    @Override
    public void configure() throws Exception {
        getContext().setDebugging(false);

        from("direct:start").routeId("hello").setBody().constant("Hello World");

        from("direct:step").routeId("hellosteps").step("hellostep")
                .doTry()
                .setBody().constant("Hello Step")
                .process(new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        // not used processor, just to be able to call `doFinally`
                    }
                })
                .doFinally()
                .id("trystep")
                .end()
                .stop()
                .id("stopstep");

        from("direct:count").routeId("count")
                .bean(ManagedCounter.class, "increment").id("counter");

        // This route ensures that a dataformat is available in the context.
        from("direct:dataformat").routeId("dataformat")
                .setBody(constant("Hello"))
                .marshal().json();
    }
}
