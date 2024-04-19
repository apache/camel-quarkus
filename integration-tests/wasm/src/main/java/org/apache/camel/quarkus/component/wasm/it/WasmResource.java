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
package org.apache.camel.quarkus.component.wasm.it;

import java.util.Map;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.ProducerTemplate;
import org.jboss.resteasy.annotations.jaxrs.QueryParam;

@Path("/wasm")
public class WasmResource {
    @Inject
    ProducerTemplate producerTemplate;

    @Path("/execute")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public Response executeWasmToUpper(@QueryParam("endpointUri") String endpointUri, String body) {
        Exchange result = producerTemplate.request(endpointUri, exchange -> {
            Message message = exchange.getMessage();
            message.setBody(body);
            message.setHeader("foo", "bar");
        });

        Exception exception = result.getException();
        if (exception == null) {
            Message message = result.getMessage();
            return Response.ok().entity(Map.of(
                    "body", message.getBody(String.class),
                    "foo", message.getHeader("foo", String.class)))
                    .build();

        } else {
            String message = exception.getMessage();
            if (exception.getCause() instanceof RuntimeException) {
                message = exception.getCause().getMessage();
            }
            return Response.serverError()
                    .entity(Map.of("exception", message))
                    .build();
        }
    }
}
