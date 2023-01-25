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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import io.smallrye.faulttolerance.core.FaultToleranceStrategy;
import io.smallrye.faulttolerance.core.InvocationContext;
import io.smallrye.faulttolerance.core.circuit.breaker.CircuitBreaker;
import io.smallrye.faulttolerance.core.stopwatch.SystemStopwatch;
import io.smallrye.faulttolerance.core.timer.ThreadTimer;
import io.smallrye.faulttolerance.core.util.ExceptionDecision;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Disposes;
import jakarta.inject.Named;

public class CoreFaultToleranceProducers {

    @ApplicationScoped
    @Named("customCircuitBreaker")
    CircuitBreaker<Integer> produceCustomCircuitBreaker(ThreadTimer threadTimer) {
        FaultToleranceStrategy<Integer> delegate = new FaultToleranceStrategy<Integer>() {
            @Override
            public Integer apply(InvocationContext<Integer> ctx) {
                return null;
            }
        };
        return new CircuitBreaker<>(delegate, "description", ExceptionDecision.ALWAYS_FAILURE, 10, 40, 0.1,
                2, SystemStopwatch.INSTANCE, threadTimer) {
            @Override
            public String toString() {
                return "customCircuitBreaker";
            }
        };
    }

    @ApplicationScoped
    @Named("customBulkheadExecutorService")
    ExecutorService produceCustomBulkheadExecutorService() {
        return Executors.newFixedThreadPool(2);
    }

    @ApplicationScoped
    @Named("threadTimer")
    ThreadTimer threadTimer(@Named("threadTimerExecutor") ExecutorService executorService) {
        return new ThreadTimer(executorService);
    }

    @ApplicationScoped
    @Named("threadTimerExecutor")
    ExecutorService threadTimerExecutor() {
        return Executors.newSingleThreadExecutor();
    }

    void disposeThreadTimerExecutor(@Disposes @Named("threadTimerExecutor") ExecutorService threadTimerExecutor,
            ThreadTimer timer) {
        try {
            timer.shutdown();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        try {
            threadTimerExecutor.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
