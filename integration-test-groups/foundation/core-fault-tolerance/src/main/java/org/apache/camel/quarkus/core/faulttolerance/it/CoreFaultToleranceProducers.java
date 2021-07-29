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

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;

import io.smallrye.faulttolerance.core.FaultToleranceStrategy;
import io.smallrye.faulttolerance.core.InvocationContext;
import io.smallrye.faulttolerance.core.circuit.breaker.CircuitBreaker;
import io.smallrye.faulttolerance.core.stopwatch.SystemStopwatch;
import io.smallrye.faulttolerance.core.util.SetOfThrowables;

public class CoreFaultToleranceProducers {

    @ApplicationScoped
    @Named("customCircuitBreaker")
    CircuitBreaker<Integer> produceCustomCircuitBreaker() {
        FaultToleranceStrategy<Integer> delegate = new FaultToleranceStrategy<Integer>() {
            @Override
            public Integer apply(InvocationContext<Integer> ctx) {
                return null;
            }
        };
        return new CircuitBreaker<Integer>(delegate, "description", SetOfThrowables.EMPTY, SetOfThrowables.EMPTY, 10, 40, 0.1,
                2, new SystemStopwatch()) {
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

}
