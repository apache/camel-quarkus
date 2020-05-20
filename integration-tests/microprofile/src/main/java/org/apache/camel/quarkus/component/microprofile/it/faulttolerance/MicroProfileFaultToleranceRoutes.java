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
package org.apache.camel.quarkus.component.microprofile.it.faulttolerance;

import java.util.concurrent.atomic.AtomicInteger;

import org.apache.camel.builder.RouteBuilder;

public class MicroProfileFaultToleranceRoutes extends RouteBuilder {

    public static final String FALLBACK_RESULT = "Fallback response";
    public static final String RESULT = "Hello Camel Quarkus MicroProfile Fault Tolerance";
    private static final AtomicInteger COUNTER = new AtomicInteger();

    @Override
    public void configure() throws Exception {
        from("direct:faultTolerance")
                .circuitBreaker()
                .process(exchange -> {
                    if (COUNTER.incrementAndGet() == 1) {
                        throw new IllegalStateException("Simulated Exception");
                    }
                    exchange.getMessage().setBody(RESULT);
                })
                .onFallback()
                .setBody().constant(FALLBACK_RESULT)
                .end();
    }
}
