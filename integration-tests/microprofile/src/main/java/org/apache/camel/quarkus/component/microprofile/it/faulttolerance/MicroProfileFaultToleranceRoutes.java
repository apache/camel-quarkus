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

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.camel.builder.RouteBuilder;
import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;

@ApplicationScoped
public class MicroProfileFaultToleranceRoutes extends RouteBuilder {

    public static final String EXCEPTION_MESSAGE = "Simulated Exception";
    public static final String FALLBACK_RESULT = "Fallback response";
    public static final String RESULT = "Hello Camel Quarkus MicroProfile Fault Tolerance";

    @Inject
    GreetingBean greetingBean;

    @Override
    public void configure() throws Exception {
        from("direct:faultToleranceWithBulkhead")
                .circuitBreaker()
                .faultToleranceConfiguration().bulkheadEnabled(true).end()
                .process(exchange -> {
                    AtomicInteger counter = MicroProfileFaultToleranceHelper.getCounter("bulkhead");
                    if (counter.incrementAndGet() == 1) {
                        throw new IllegalStateException(EXCEPTION_MESSAGE);
                    }
                    exchange.getMessage().setBody(RESULT);
                })
                .onFallback()
                .setBody().constant(FALLBACK_RESULT)
                .end();

        from("direct:faultToleranceWithFallback")
                .circuitBreaker()
                .process(exchange -> {
                    AtomicInteger counter = MicroProfileFaultToleranceHelper.getCounter("fallback");
                    if (counter.incrementAndGet() == 1) {
                        throw new IllegalStateException("Simulated Exception");
                    }
                    exchange.getMessage().setBody(RESULT);
                })
                .onFallback()
                .setBody().constant(FALLBACK_RESULT)
                .end();

        from("direct:faultToleranceWithThreshold")
                .circuitBreaker()
                .faultToleranceConfiguration().failureRatio(100).successThreshold(1).requestVolumeThreshold(1).delay(0).end()
                .process(exchange -> {
                    AtomicInteger counter = MicroProfileFaultToleranceHelper.getCounter("threshold");
                    if (counter.incrementAndGet() == 1) {
                        throw new IllegalStateException("Simulated Exception");
                    }
                    exchange.getMessage().setBody("Nothing to see here. Circuit breaker is open...");
                })
                .end()
                .setBody().simple(RESULT);

        from("direct:faultToleranceWithTimeout")
                .circuitBreaker()
                .faultToleranceConfiguration().timeoutEnabled(true).timeoutDuration(500).end()
                .process(exchange -> {
                    AtomicInteger counter = MicroProfileFaultToleranceHelper.getCounter("timeout");
                    if (counter.incrementAndGet() == 1) {
                        Thread.sleep(1000);
                    }
                    exchange.getMessage().setBody(RESULT);
                })
                .onFallback()
                .setBody().simple(FALLBACK_RESULT)
                .end();

        from("direct:faultToleranceWithTimeoutCustomExecutor")
                .circuitBreaker()
                .faultToleranceConfiguration().timeoutEnabled(true).timeoutScheduledExecutorService("myThreadPool")
                .timeoutDuration(500).end()
                .process(exchange -> {
                    AtomicInteger counter = MicroProfileFaultToleranceHelper.getCounter("timeoutCustomExecutor");
                    if (counter.incrementAndGet() == 1) {
                        Thread.sleep(1000);
                    }
                    exchange.getMessage().setBody(RESULT);
                })
                .onFallback()
                .setBody().simple(FALLBACK_RESULT)
                .end();

        from("direct:inheritErrorHandler")
                .errorHandler(deadLetterChannel("mock:dead").maximumRedeliveries(3).redeliveryDelay(0))
                .circuitBreaker().inheritErrorHandler(true)
                .to("mock:start")
                .throwException(new IllegalArgumentException(EXCEPTION_MESSAGE)).end()
                .to("mock:end");

        from("direct:circuitBreakerBean")
                .bean(greetingBean, "greetWithCircuitBreaker");

        from("direct:fallbackBean")
                .bean(greetingBean, "greetWithFallback");

        from("direct:timeoutBean")
                .doTry()
                .bean(greetingBean, "greetWithDelay")
                .doCatch(TimeoutException.class)
                .setBody().constant(FALLBACK_RESULT)
                .end();
    }

    @Named("myThreadPool")
    public ScheduledExecutorService myThreadPool() {
        return getCamelContext().getExecutorServiceManager()
                .newScheduledThreadPool(this, "myThreadPool", 2);
    }
}
