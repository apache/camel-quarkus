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

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Timeout;

import static org.apache.camel.quarkus.component.microprofile.it.faulttolerance.MicroProfileFaultToleranceRoutes.EXCEPTION_MESSAGE;
import static org.apache.camel.quarkus.component.microprofile.it.faulttolerance.MicroProfileFaultToleranceRoutes.FALLBACK_RESULT;
import static org.apache.camel.quarkus.component.microprofile.it.faulttolerance.MicroProfileFaultToleranceRoutes.RESULT;

@ApplicationScoped
public class GreetingBean {

    @Fallback(fallbackMethod = "fallbackGreeting")
    public String greetWithFallback() {
        AtomicInteger counter = MicroProfileFaultToleranceHelper.getCounter("beanFallback");
        if (counter.incrementAndGet() == 1) {
            throw new IllegalStateException(EXCEPTION_MESSAGE);
        }
        return RESULT;
    }

    @Timeout(250)
    public String greetWithDelay() throws InterruptedException {
        AtomicInteger counter = MicroProfileFaultToleranceHelper.getCounter("beanTimeout");
        if (counter.incrementAndGet() == 1) {
            Thread.sleep(500);
            return "Nothing to see here, method invocation timed out!";
        }
        return RESULT;
    }

    @CircuitBreaker(failureRatio = 1.0, requestVolumeThreshold = 1, delay = 0)
    public String greetWithCircuitBreaker() {
        AtomicInteger counter = MicroProfileFaultToleranceHelper.getCounter("beanThreshold");
        if (counter.incrementAndGet() == 1) {
            throw new IllegalStateException(EXCEPTION_MESSAGE);
        }
        return RESULT;
    }

    public String fallbackGreeting() {
        return FALLBACK_RESULT;
    }
}
