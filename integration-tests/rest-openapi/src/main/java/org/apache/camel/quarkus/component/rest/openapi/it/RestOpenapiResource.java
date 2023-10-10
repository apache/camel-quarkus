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
package org.apache.camel.quarkus.component.rest.openapi.it;

import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.rest.openapi.RestOpenApiValidationException;

@Path("/rest-openapi")
@ApplicationScoped
public class RestOpenapiResource {
    @Inject
    ProducerTemplate producerTemplate;

    @Path("/fruits/list/json")
    @Produces(MediaType.APPLICATION_JSON)
    @GET
    public Response invokeApiOperation(@QueryParam("port") int port) {
        return invokeApiOperation("start-web-json", port);
    }

    @Path("/fruits/list/yaml")
    @Produces(MediaType.APPLICATION_JSON)
    @GET
    public Response invokeListFruitsOperationYaml(@QueryParam("port") int port) {
        return invokeApiOperation("start-web-yaml", port);
    }

    @Path("/fruits/list/file")
    @Produces(MediaType.APPLICATION_JSON)
    @GET
    public Response invokeListFruitsOperationFile(@QueryParam("port") int port) {
        return invokeApiOperation("start-file", port);
    }

    @Path("/fruits/list/bean")
    @Produces(MediaType.APPLICATION_JSON)
    @GET
    public Response invokeListFruitsOperationBean(@QueryParam("port") int port) {
        return invokeApiOperation("start-bean", port);
    }

    @Path("/fruits/list/classpath")
    @Produces(MediaType.APPLICATION_JSON)
    @GET
    public Response invokeListFruitsOperationClasspath(@QueryParam("port") int port) {
        return invokeApiOperation("start-classpath", port);
    }

    @Path("/fruits/add")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    @POST
    public Response invokeAddFruitOperation(@QueryParam("port") int port, String fruitJson) {
        Exchange result = producerTemplate.request("direct:validate", new Processor() {
            @Override
            public void process(Exchange exchange) throws Exception {
                Message message = exchange.getMessage();
                message.setHeader(Exchange.CONTENT_TYPE, "application/json");
                message.setHeader("test-port", port);
                message.setBody(fruitJson);
            }
        });

        Exception exception = result.getException();
        if (exception != null) {
            String errorMessage = "";
            if (exception instanceof RestOpenApiValidationException) {
                RestOpenApiValidationException validationException = (RestOpenApiValidationException) exception;
                errorMessage = validationException.getValidationErrors().stream().collect(Collectors.joining(","));
            }
            return Response.serverError().entity(errorMessage).build();
        }
        return Response.ok().entity(result.getMessage().getBody(String.class)).build();
    }

    private Response invokeApiOperation(String endpointName, int port) {
        String response = producerTemplate.requestBodyAndHeader("direct:" + endpointName, null, "test-port", port,
                String.class);
        return Response.ok().entity(response).build();
    }

}
