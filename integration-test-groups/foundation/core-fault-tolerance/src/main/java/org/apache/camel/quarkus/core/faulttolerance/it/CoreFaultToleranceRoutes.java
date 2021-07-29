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
package org.apache.camel.quarkus.core.faulttolerance.it;

import javax.enterprise.context.ApplicationScoped;

import org.apache.camel.builder.RouteBuilder;

/**
 * No message is expected to flow through this route.
 * The route definition is there only to test the camel fault tolerance configuration parsing.
 */
@ApplicationScoped
public class CoreFaultToleranceRoutes extends RouteBuilder {

    public static final String FALLBACK_RESULT = "Fallback response";
    public static final String RESULT = "Hello Camel Quarkus Core Fault Tolerance";

    @Override
    public void configure() {
        from("direct:faultTolerance").circuitBreaker().id("ftp").process(exchange -> {
            exchange.getMessage().setBody(RESULT);
        }).onFallback().setBody().constant(FALLBACK_RESULT).end();
    }
}
