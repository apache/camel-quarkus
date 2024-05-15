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

import io.smallrye.faulttolerance.core.circuit.breaker.CircuitBreaker;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.microprofile.faulttolerance.FaultToleranceProcessor;
import org.apache.camel.component.mock.MockEndpoint;

@Path("/microprofile-fault-tolerance")
public class MicroprofileFaultToleranceResource {

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    CamelContext context;

    @Named("customCircuitBreaker")
    CircuitBreaker<Integer> customCircuitBreaker;

    @Named("customBulkheadExecutorService")
    ExecutorService customBulkheadExecutorService;

    @Path("/route/{route}")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public String triggerFaultToleranceRoute(@PathParam("route") String route) {
        return producerTemplate.requestBody("direct:" + route, null, String.class);
    }

    @Path("/faultToleranceWithThreshold/{route}")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public String faultToleranceWithThreshold(@PathParam("route") String route) {
        try {
            return producerTemplate.requestBody("direct:" + route, null, String.class);
        } catch (Exception e) {
            return e.getCause().getMessage();
        }
    }

    @Path("/inheritErrorHandler")
    @POST
    public void inheritErrorHandler() throws Exception {
        MockEndpoint start = context.getEndpoint("mock:start", MockEndpoint.class);
        start.expectedMessageCount(4);

        MockEndpoint end = context.getEndpoint("mock:end", MockEndpoint.class);
        end.expectedMessageCount(0);

        MockEndpoint dead = context.getEndpoint("mock:dead", MockEndpoint.class);
        dead.expectedMessageCount(1);

        producerTemplate.requestBody("direct:inheritErrorHandler", null, String.class);

        start.assertIsSatisfied(5000);
        end.assertIsSatisfied(5000);
        dead.assertIsSatisfied(5000);
    }

    @Path("/configuration")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject faultToleranceConfigurations() {
        FaultToleranceProcessor processor = context.getProcessor("ftp", FaultToleranceProcessor.class);
        JsonObjectBuilder objectBuilder = Json.createObjectBuilder();
        objectBuilder.add("isCustomCircuitBreakerRef", processor.getCircuitBreaker() == customCircuitBreaker);
        objectBuilder.add("delay", processor.getDelay());
        objectBuilder.add("successThreshold", processor.getSuccessThreshold());
        objectBuilder.add("requestVolumeThreshold", processor.getRequestVolumeThreshold());
        objectBuilder.add("failureRatio", (int) (processor.getFailureRate() * 100));
        objectBuilder.add("timeoutEnabled", processor.isTimeoutEnabled());
        objectBuilder.add("timeoutDuration", processor.getTimeoutDuration());
        objectBuilder.add("timeoutPoolSize", processor.getTimeoutPoolSize());
        objectBuilder.add("bulkheadEnabled", processor.isBulkheadEnabled());
        objectBuilder.add("bulkheadMaxConcurrentCalls", processor.getBulkheadMaxConcurrentCalls());
        objectBuilder.add("bulkheadWaitingTaskQueue", processor.getBulkheadWaitingTaskQueue());
        objectBuilder.add("isCustomBulkheadExecutorServiceRef",
                processor.getExecutorService() == customBulkheadExecutorService);

        return objectBuilder.build();
    }
}
