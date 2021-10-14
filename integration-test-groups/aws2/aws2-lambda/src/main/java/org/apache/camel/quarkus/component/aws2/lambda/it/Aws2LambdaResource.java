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
import java.util.Map;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.Provider;

import org.apache.camel.CamelExecutionException;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.aws2.lambda.Lambda2Constants;
import org.apache.camel.component.aws2.lambda.Lambda2Operations;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.lambda.model.AliasConfiguration;
import software.amazon.awssdk.services.lambda.model.CreateEventSourceMappingResponse;
import software.amazon.awssdk.services.lambda.model.EventSourceMappingConfiguration;
import software.amazon.awssdk.services.lambda.model.FunctionConfiguration;
import software.amazon.awssdk.services.lambda.model.GetAliasRequest;
import software.amazon.awssdk.services.lambda.model.GetAliasResponse;
import software.amazon.awssdk.services.lambda.model.GetFunctionResponse;
import software.amazon.awssdk.services.lambda.model.InvalidParameterValueException;
import software.amazon.awssdk.services.lambda.model.LastUpdateStatus;
import software.amazon.awssdk.services.lambda.model.ListAliasesRequest;
import software.amazon.awssdk.services.lambda.model.ListAliasesResponse;
import software.amazon.awssdk.services.lambda.model.ListEventSourceMappingsResponse;
import software.amazon.awssdk.services.lambda.model.ListFunctionsResponse;
import software.amazon.awssdk.services.lambda.model.ListTagsResponse;
import software.amazon.awssdk.services.lambda.model.ListVersionsByFunctionResponse;
import software.amazon.awssdk.services.lambda.model.Runtime;
import software.amazon.awssdk.services.lambda.model.UpdateFunctionCodeRequest;
import software.amazon.awssdk.services.lambda.model.UpdateFunctionCodeResponse;

@Path("/aws2-lambda")
@ApplicationScoped
public class Aws2LambdaResource {

    private static final Logger LOG = Logger.getLogger(Aws2LambdaResource.class);

    @ConfigProperty(name = "aws-lambda.role-arn")
    String roleArn;

    @ConfigProperty(name = "aws-lambda.event-source-arn")
    String eventSourceArn;

    @Inject
    ProducerTemplate producerTemplate;

    @Path("/function/create/{functionName}")
    @POST
    @Consumes("application/zip")
    @Produces(MediaType.TEXT_PLAIN)
    public Response createFunction(byte[] zipFunctionBytes, @PathParam("functionName") String functionName) throws Exception {
        final String response = producerTemplate.requestBodyAndHeaders(
                componentUri(functionName, Lambda2Operations.createFunction),
                zipFunctionBytes,
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

    @Path("/function/get/{functionName}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String getFunction(@PathParam("functionName") String functionName) {
        return producerTemplate
                .requestBody(componentUri(functionName, Lambda2Operations.getFunction), null, GetFunctionResponse.class)
                .configuration().functionName();
    }

    @Path("/function/getArn/{functionName}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String getFunctionArn(@PathParam("functionName") String functionName) {
        return producerTemplate
                .requestBody(componentUri(functionName, Lambda2Operations.getFunction), null, GetFunctionResponse.class)
                .configuration().functionArn();
    }

    @Path("/function/update/{functionName}")
    @PUT
    @Consumes("application/zip")
    @Produces(MediaType.TEXT_PLAIN)
    public Response updateFunction(byte[] zipFunctionBytes, @PathParam("functionName") String functionName) {
        String uri = componentUri(functionName, Lambda2Operations.updateFunction) + "&pojoRequest=true";
        UpdateFunctionCodeRequest ufcRequest = UpdateFunctionCodeRequest.builder().functionName(functionName)
                .zipFile(SdkBytes.fromByteArray(zipFunctionBytes)).build();
        UpdateFunctionCodeResponse ufcResponse = producerTemplate.requestBody(uri, ufcRequest,
                UpdateFunctionCodeResponse.class);
        if (ufcResponse.lastUpdateStatus() == LastUpdateStatus.SUCCESSFUL) {
            return Response.ok().build();
        }
        throw new IllegalStateException(
                ufcResponse.lastUpdateStatusReasonCodeAsString() + ": " + ufcResponse.lastUpdateStatusReason());
    }

    @Path("/function/list")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<String> listFunctions() {
        return producerTemplate.requestBody(
                componentUri("foo", Lambda2Operations.listFunctions),
                null,
                ListFunctionsResponse.class)
                .functions().stream()
                .map(FunctionConfiguration::functionName)
                .collect(Collectors.toList());
    }

    @Path("/function/invoke/{functionName}")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String invokeFunction(byte[] message, @PathParam("functionName") String functionName) {
        return producerTemplate.requestBody(
                componentUri(functionName, Lambda2Operations.invokeFunction),
                message,
                String.class);
    }

    @Path("/function/delete/{functionName}")
    @DELETE
    public void deleteFunction(@PathParam("functionName") String functionName) {
        producerTemplate.requestBody(
                componentUri(functionName, Lambda2Operations.deleteFunction),
                null,
                Object.class);
    }

    @Path("/alias/create")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public Response createAlias(@QueryParam("functionName") String functionName,
            @QueryParam("functionVersion") String functionVersion, @QueryParam("aliasName") String aliasName) throws Exception {
        try {
            final String response = producerTemplate.requestBodyAndHeaders(
                    componentUri(functionName, Lambda2Operations.createAlias),
                    null,
                    new LinkedHashMap<String, Object>() {
                        {
                            put(Lambda2Constants.FUNCTION_ALIAS_NAME, aliasName);
                            put(Lambda2Constants.FUNCTION_VERSION, functionVersion);
                        }
                    },
                    String.class);
            return Response
                    .created(new URI("https://camel.apache.org/"))
                    .entity(response)
                    .build();
        } catch (Exception e) {
            LOG.info("Exception caught in alias/create", e);
            LOG.info("Exception cause in alias/create", e.getCause());
            throw e;
        }
    }

    @Path("/alias/get")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getAlias(@QueryParam("functionName") String functionName, @QueryParam("aliasName") String aliasName) {
        try {
            GetAliasRequest getAliasRequest = GetAliasRequest.builder().functionName(functionName).name(aliasName).build();
            String endpointUri = componentUri(functionName, Lambda2Operations.getAlias) + "&pojoRequest=true";

            return producerTemplate
                    .requestBody(endpointUri, getAliasRequest, GetAliasResponse.class)
                    .functionVersion();
        } catch (Exception e) {
            LOG.info("Exception caught in alias/get", e);
            LOG.info("Exception cause in alias/get", e.getCause());
            throw e;
        }
    }

    @Path("/alias/delete")
    @DELETE
    public void deleteAlias(@QueryParam("functionName") String functionName, @QueryParam("aliasName") String aliasName) {
        try {
            producerTemplate.requestBodyAndHeader(
                    componentUri(functionName, Lambda2Operations.deleteAlias),
                    null,
                    Lambda2Constants.FUNCTION_ALIAS_NAME,
                    aliasName,
                    Object.class);
        } catch (Exception e) {
            LOG.info("Exception caught in alias/delete", e);
            LOG.info("Exception cause in alias/delete", e.getCause());
            throw e;
        }
    }

    @Path("/alias/list")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<String> listAliases(@QueryParam("functionName") String functionName) {
        try {
            ListAliasesRequest listAliasesReq = ListAliasesRequest.builder().functionName(functionName).build();
            return producerTemplate.requestBody(
                    componentUri(functionName, Lambda2Operations.listAliases) + "&pojoRequest=true",
                    listAliasesReq,
                    ListAliasesResponse.class)
                    .aliases().stream()
                    .map(AliasConfiguration::name)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            LOG.info("Exception caught in alias/list", e);
            LOG.info("Exception cause in alias/list", e.getCause());
            throw e;
        }
    }

    @Path("/tag/create")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public Response tagLambdaFunction(@QueryParam("functionArn") String functionArn,
            @QueryParam("tagResourceKey") String tagResourceKey, @QueryParam("tagResourceValue") String tagResourceValue)
            throws Exception {
        Map<String, String> resourceTags = Map.of(tagResourceKey, tagResourceValue);
        final String response = producerTemplate.requestBodyAndHeaders(
                componentUri(null, Lambda2Operations.tagResource),
                null,
                new LinkedHashMap<String, Object>() {
                    {
                        put(Lambda2Constants.RESOURCE_ARN, functionArn);
                        put(Lambda2Constants.RESOURCE_TAGS, resourceTags);
                    }
                },
                String.class);
        return Response
                .created(new URI("https://camel.apache.org/"))
                .entity(response)
                .build();
    }

    @Path("/tag/list")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, String> listLambdaFunctionTags(@QueryParam("functionArn") String functionArn) {
        return producerTemplate.requestBodyAndHeaders(
                componentUri(null, Lambda2Operations.listTags),
                null,
                new LinkedHashMap<String, Object>() {
                    {
                        put(Lambda2Constants.RESOURCE_ARN, functionArn);
                    }
                },
                ListTagsResponse.class)
                .tags();
    }

    @Path("/tag/delete")
    @DELETE
    public void untagLambdaFunction(@QueryParam("functionArn") String functionArn,
            @QueryParam("tagResourceKey") String tagResourceKey) {
        producerTemplate.requestBodyAndHeaders(
                componentUri(null, Lambda2Operations.untagResource),
                null,
                new LinkedHashMap<String, Object>() {
                    {
                        put(Lambda2Constants.RESOURCE_ARN, functionArn);
                        put(Lambda2Constants.RESOURCE_TAG_KEYS, List.of(tagResourceKey));
                    }
                });
    }

    @Path("/version/publish")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public Response publishVersion(@QueryParam("functionName") String functionName,
            @QueryParam("versionDescription") String versionDescription) throws Exception {
        final String response = producerTemplate.requestBody(
                componentUri(functionName, Lambda2Operations.publishVersion),
                null,
                String.class);
        return Response
                .created(new URI("https://camel.apache.org/"))
                .entity(response)
                .build();
    }

    @Path("/version/list")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<String> listVersions(@QueryParam("functionName") String functionName) {
        try {
            return producerTemplate.requestBody(
                    componentUri(functionName, Lambda2Operations.listVersions),
                    null,
                    ListVersionsByFunctionResponse.class)
                    .versions().stream()
                    .map(FunctionConfiguration::version)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            LOG.info("Exception caught in version/list", e);
            LOG.info("Exception cause in version/list", e.getCause());
            throw e;
        }
    }

    @Path("/event-source-mapping/create")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public Response createEventSourceMapping(@QueryParam("functionName") String functionName) throws Exception {
        try {
            CreateEventSourceMappingResponse response = producerTemplate
                    .requestBodyAndHeader(
                            componentUri(functionName, Lambda2Operations.createEventSourceMapping),
                            null,
                            Lambda2Constants.EVENT_SOURCE_ARN,
                            eventSourceArn,
                            CreateEventSourceMappingResponse.class);

            return Response.created(new URI("https://camel.apache.org/")).entity(response.uuid()).build();
        } catch (Exception e) {
            LOG.info("Exception caught in event-source-mapping/create", e);
            LOG.info("Exception cause in event-source-mapping/create", e.getCause());
            throw e;
        }
    }

    @Path("/event-source-mapping/list")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, String> listEventSourceMappings(@QueryParam("functionName") String functionName) {
        try {
            ListEventSourceMappingsResponse response = producerTemplate.requestBody(
                    componentUri(functionName, Lambda2Operations.listEventSourceMapping),
                    null,
                    ListEventSourceMappingsResponse.class);
            return response.eventSourceMappings().stream()
                    .collect(Collectors.toMap(EventSourceMappingConfiguration::uuid, EventSourceMappingConfiguration::state));
        } catch (Exception e) {
            LOG.info("Exception caught in event-source-mapping/list", e);
            LOG.info("Exception cause in event-source-mapping/list", e.getCause());
            throw e;
        }
    }

    @Path("/event-source-mapping/delete")
    @DELETE
    public void deleteEventSourceMapping(@QueryParam("eventSourceMappingUuid") String eventSourceMappingUuid) {
        try {
            producerTemplate.requestBodyAndHeader(
                    componentUri(null, Lambda2Operations.deleteEventSourceMapping),
                    null,
                    Lambda2Constants.EVENT_SOURCE_UUID,
                    eventSourceMappingUuid);
        } catch (Exception e) {
            LOG.info("Exception caught in event-source-mapping/delete", e);
            LOG.info("Exception cause in event-source-mapping/delete", e.getCause());
            throw e;
        }
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
