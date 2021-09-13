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
package org.apache.camel.quarkus.component.aws2.lambda.it;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.Provider;

import org.apache.camel.CamelExecutionException;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.aws2.lambda.Lambda2Constants;
import org.apache.camel.component.aws2.lambda.Lambda2Operations;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import software.amazon.awssdk.services.lambda.model.FunctionConfiguration;
import software.amazon.awssdk.services.lambda.model.InvalidParameterValueException;
import software.amazon.awssdk.services.lambda.model.ListFunctionsResponse;
import software.amazon.awssdk.services.lambda.model.Runtime;

@Path("/aws2-lambda")
@ApplicationScoped
public class Aws2LambdaResource {
    @ConfigProperty(name = "aws-lambda.role-arn")
    String roleArn;

    @Inject
    ProducerTemplate producerTemplate;

    @Path("/create/{functionName}")
    @POST
    @Consumes("application/zip")
    @Produces(MediaType.TEXT_PLAIN)
    public Response createFunction(byte[] message, @PathParam("functionName") String functionName) throws Exception {
        final String response = producerTemplate.requestBodyAndHeaders(
                componentUri(functionName, Lambda2Operations.createFunction),
                message,
                new LinkedHashMap<String, Object>() {
                    {
                        put(Lambda2Constants.ROLE, roleArn);
                        put(Lambda2Constants.RUNTIME, Runtime.PYTHON3_9);
                        put(Lambda2Constants.HANDLER, "index.handler");
                    }
                },
                String.class);
        return Response
                .created(new URI("https://camel.apache.org/"))
                .entity(response)
                .build();
    }

    @Path("/listFunctions")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<String> listFunctions() throws Exception {
        return producerTemplate.requestBody(
                componentUri("foo", Lambda2Operations.listFunctions),
                null,
                ListFunctionsResponse.class)
                .functions().stream()
                .map(FunctionConfiguration::functionName)
                .collect(Collectors.toList());
    }

    @Path("/invoke/{functionName}")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String invoke(byte[] message, @PathParam("functionName") String functionName) throws Exception {
        return producerTemplate.requestBody(
                componentUri(functionName, Lambda2Operations.invokeFunction),
                message,
                String.class);
    }

    @Path("/delete/{functionName}")
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    public void delete(@PathParam("functionName") String functionName) throws Exception {
        producerTemplate.requestBody(
                componentUri(functionName, Lambda2Operations.deleteFunction),
                null,
                Object.class);
    }

    private static String componentUri(String functionName, Lambda2Operations operation) {
        return "aws2-lambda:" + functionName + "?operation=" + operation;
    }

    @Provider
    public static class ExceptionMapper implements javax.ws.rs.ext.ExceptionMapper<CamelExecutionException> {
        @Override
        public Response toResponse(CamelExecutionException exception) {
            if (exception.getCause() instanceof InvalidParameterValueException) {
                return Response.status(Status.BAD_REQUEST).entity(exception.getCause().getMessage()).build();
            }
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(exception.getMessage()).build();
        }
    }

}
