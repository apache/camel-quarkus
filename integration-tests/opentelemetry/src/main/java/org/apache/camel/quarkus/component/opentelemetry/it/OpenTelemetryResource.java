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
package org.apache.camel.quarkus.component.opentelemetry.it;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.apache.camel.ProducerTemplate;

@Path("/opentelemetry")
@ApplicationScoped
public class OpenTelemetryResource {

    @Inject
    ProducerTemplate producerTemplate;

    @Path("/trace")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String traceRoute() {
        return producerTemplate.requestBody("direct:start", null, String.class);
    }

    @Path("/greet/{name}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String traceRoute(@PathParam("name") String name) {
        return producerTemplate.requestBody("direct:greet", name, String.class);
    }

    @Path("/jdbc/query")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public long jdbcQuery() {
        return producerTemplate.requestBody("direct:jdbcQuery", null, Long.class);
    }
}
