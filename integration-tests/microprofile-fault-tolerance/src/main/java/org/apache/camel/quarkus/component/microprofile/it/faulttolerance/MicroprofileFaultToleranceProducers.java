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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import io.smallrye.faulttolerance.api.TypedGuard;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Disposes;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.apache.camel.Exchange;

@ApplicationScoped
public class MicroprofileFaultToleranceProducers {
    @Singleton
    @Named("customTypedGuard")
    TypedGuard<Exchange> produceCustomTypedGuard() {
        return TypedGuard.create(Exchange.class).build();
    }

    @ApplicationScoped
    @Named("customExecutorService")
    ExecutorService produceCustomExecutorService() {
        return Executors.newFixedThreadPool(2);
    }

    void disposeCustomExecutorService(
            @Disposes @Named("customExecutorService") ExecutorService executorService) {
        try {
            executorService.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
