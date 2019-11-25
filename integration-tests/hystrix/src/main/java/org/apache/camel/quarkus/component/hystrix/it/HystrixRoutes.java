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
package org.apache.camel.quarkus.component.hystrix.it;

import org.apache.camel.builder.RouteBuilder;

public class HystrixRoutes extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        from("direct:fallback")
                .circuitBreaker()
                .hystrixConfiguration()
                .executionTimeoutInMilliseconds(100)
                .end()
                .to("direct:delay")
                .onFallback()
                .setBody(constant("Fallback response"))
                .end();

        from("direct:fallbackViaNetwork")
                .circuitBreaker()
                .throwException(new IllegalStateException("Forced exception"))
                .onFallbackViaNetwork()
                .to("netty-http:http://localhost:8999/network/fallback")
                .end();

        from("direct:delay")
                .delay(simple("${header.delayMilliseconds}"))
                .setBody(constant("Hello Camel Quarkus Hystrix"));

        from("netty-http:http://0.0.0.0:8999/network/fallback")
                .setBody(constant("Fallback via network response"));
    }
}
