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
package org.apache.camel.quarkus.component.micrometer.observability.it;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.apache.camel.ProducerTemplate;

@Path("/micrometer-observability")
@ApplicationScoped
public class MicrometerObservabilityResource {

    @Inject
    ProducerTemplate producerTemplate;

    @Path("/trace")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String trace() {
        return producerTemplate.requestBody("direct:traced", "test", String.class);
    }

    /**
     * Accepts an optional {@code traceparent} W3C header and forwards it into the Camel exchange.
     * The Propagator will extract the upstream trace context from this header and attach
     * all Camel spans to the same trace, which is the core distributed-tracing scenario.
     */
    @Path("/trace-excluded")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String traceExcluded() {
        return producerTemplate.requestBody("direct:excluded", "test", String.class);
    }

    @Path("/trace-upstream")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String traceUpstream(@HeaderParam("traceparent") String traceparent) {
        if (traceparent != null) {
            return producerTemplate.requestBodyAndHeader("direct:traced", "test",
                    "traceparent", traceparent, String.class);
        }
        return producerTemplate.requestBody("direct:traced", "test", String.class);
    }
}
