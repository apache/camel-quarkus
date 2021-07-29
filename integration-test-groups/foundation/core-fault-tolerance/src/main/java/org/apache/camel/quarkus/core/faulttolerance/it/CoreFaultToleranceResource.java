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

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.smallrye.faulttolerance.core.circuit.breaker.CircuitBreaker;
import org.apache.camel.CamelContext;
import org.apache.camel.component.microprofile.faulttolerance.FaultToleranceProcessor;

@Path("/core")
@ApplicationScoped
public class CoreFaultToleranceResource {

    @Inject
    CamelContext context;

    @Named("customCircuitBreaker")
    CircuitBreaker<Integer> customCircuitBreaker;

    @Named("customBulkheadExecutorService")
    ExecutorService customBulkheadExecutorService;

    @Path("/fault-tolerance-configurations")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject faultToleranceConfigurations() {
        FaultToleranceProcessor ftp = context.getProcessor("ftp", FaultToleranceProcessor.class);

        JsonObjectBuilder objectBuilder = Json.createObjectBuilder();
        objectBuilder.add("isCustomCircuitBreakerRef", ftp.getCircuitBreaker() == customCircuitBreaker);
        objectBuilder.add("delay", ftp.getDelay());
        objectBuilder.add("successThreshold", ftp.getSuccessThreshold());
        objectBuilder.add("requestVolumeThreshold", ftp.getRequestVolumeThreshold());
        objectBuilder.add("failureRatio", (int) (ftp.getFailureRate() * 100));
        objectBuilder.add("timeoutEnabled", ftp.isTimeoutEnabled());
        objectBuilder.add("timeoutDuration", ftp.getTimeoutDuration());
        objectBuilder.add("timeoutPoolSize", ftp.getTimeoutPoolSize());
        objectBuilder.add("bulkheadEnabled", ftp.isBulkheadEnabled());
        objectBuilder.add("bulkheadMaxConcurrentCalls", ftp.getBulkheadMaxConcurrentCalls());
        objectBuilder.add("bulkheadWaitingTaskQueue", ftp.getBulkheadWaitingTaskQueue());
        objectBuilder.add("isCustomBulkheadExecutorServiceRef", ftp.getExecutorService() == customBulkheadExecutorService);

        return objectBuilder.build();
    }
}
