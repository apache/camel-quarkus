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
package org.apache.camel.quarkus.component.aws2.ec2.it;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.aws2.ec2.AWS2EC2Constants;
import org.apache.camel.component.aws2.ec2.AWS2EC2Operations;
import software.amazon.awssdk.services.ec2.model.DescribeInstanceStatusResponse;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesResponse;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.InstanceType;
import software.amazon.awssdk.services.ec2.model.RunInstancesRequest;
import software.amazon.awssdk.services.ec2.model.RunInstancesResponse;
import software.amazon.awssdk.services.ec2.model.StartInstancesResponse;
import software.amazon.awssdk.services.ec2.model.StopInstancesResponse;
import software.amazon.awssdk.services.ec2.model.Tag;
import software.amazon.awssdk.services.ec2.model.TerminateInstancesResponse;

import static jakarta.ws.rs.core.Response.Status.CREATED;

@Path("/aws2-ec2")
@ApplicationScoped
public class Aws2Ec2Resource {

    @Inject
    ProducerTemplate producerTemplate;

    @Path("/instances")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public Response createAndRunInstances(@QueryParam("imageId") String imageId) {
        RunInstancesResponse response = producerTemplate.requestBodyAndHeaders(
                componentUri(AWS2EC2Operations.createAndRunInstances),
                null,
                Map.of(
                        AWS2EC2Constants.IMAGE_ID, imageId,
                        AWS2EC2Constants.INSTANCE_TYPE, InstanceType.T2_MICRO,
                        AWS2EC2Constants.INSTANCE_MIN_COUNT, 1,
                        AWS2EC2Constants.INSTANCE_MAX_COUNT, 1),
                RunInstancesResponse.class);

        String instanceId = response.instances().get(0).instanceId();
        return Response.status(CREATED).entity(instanceId).build();
    }

    @Path("/instances/pojo")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public Response createAndRunInstancesPojo(@QueryParam("imageId") String imageId) {
        RunInstancesRequest request = RunInstancesRequest.builder()
                .imageId(imageId)
                .instanceType(InstanceType.T2_MICRO)
                .minCount(1)
                .maxCount(1)
                .build();

        RunInstancesResponse response = producerTemplate.requestBody(
                componentUri(AWS2EC2Operations.createAndRunInstances) + "&pojoRequest=true",
                request,
                RunInstancesResponse.class);

        String instanceId = response.instances().get(0).instanceId();
        return Response.status(CREATED).entity(instanceId).build();
    }

    @Path("/instances")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<String> describeInstances(@QueryParam("instanceId") List<String> instanceIds) {
        DescribeInstancesResponse response = producerTemplate.requestBodyAndHeader(
                componentUri(AWS2EC2Operations.describeInstances),
                null,
                AWS2EC2Constants.INSTANCES_IDS,
                instanceIds,
                DescribeInstancesResponse.class);

        return response.reservations().stream()
                .flatMap(r -> r.instances().stream())
                .map(Instance::instanceId)
                .collect(Collectors.toList());
    }

    @Path("/instances/{instanceId}/start")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public Response startInstances(@PathParam("instanceId") String instanceId) {
        StartInstancesResponse response = producerTemplate.requestBodyAndHeader(
                componentUri(AWS2EC2Operations.startInstances),
                null,
                AWS2EC2Constants.INSTANCES_IDS,
                List.of(instanceId),
                StartInstancesResponse.class);

        String currentState = response.startingInstances().get(0).currentState().nameAsString();
        return Response.ok(currentState).build();
    }

    @Path("/instances/{instanceId}/stop")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public Response stopInstances(@PathParam("instanceId") String instanceId) {
        StopInstancesResponse response = producerTemplate.requestBodyAndHeader(
                componentUri(AWS2EC2Operations.stopInstances),
                null,
                AWS2EC2Constants.INSTANCES_IDS,
                List.of(instanceId),
                StopInstancesResponse.class);

        String currentState = response.stoppingInstances().get(0).currentState().nameAsString();
        return Response.ok(currentState).build();
    }

    @Path("/instances/{instanceId}/terminate")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public Response terminateInstances(@PathParam("instanceId") String instanceId) {
        TerminateInstancesResponse response = producerTemplate.requestBodyAndHeader(
                componentUri(AWS2EC2Operations.terminateInstances),
                null,
                AWS2EC2Constants.INSTANCES_IDS,
                List.of(instanceId),
                TerminateInstancesResponse.class);

        String currentState = response.terminatingInstances().get(0).currentState().nameAsString();
        return Response.ok(currentState).build();
    }

    @Path("/instances/status")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response describeInstancesStatus(@QueryParam("instanceId") String instanceId) {
        DescribeInstanceStatusResponse response = producerTemplate.requestBodyAndHeader(
                componentUri(AWS2EC2Operations.describeInstancesStatus),
                null,
                AWS2EC2Constants.INSTANCES_IDS,
                List.of(instanceId),
                DescribeInstanceStatusResponse.class);

        if (response.instanceStatuses().isEmpty()) {
            return Response.ok("no-status").build();
        }
        String state = response.instanceStatuses().get(0).instanceState().nameAsString();
        return Response.ok(state).build();
    }

    @Path("/instances/{instanceId}/reboot")
    @POST
    public Response rebootInstances(@PathParam("instanceId") String instanceId) {
        producerTemplate.requestBodyAndHeader(
                componentUri(AWS2EC2Operations.rebootInstances),
                null,
                AWS2EC2Constants.INSTANCES_IDS,
                List.of(instanceId));
        return Response.noContent().build();
    }

    @Path("/instances/{instanceId}/tags")
    @POST
    public Response createTags(@PathParam("instanceId") String instanceId,
            @QueryParam("key") String key,
            @QueryParam("value") String value) {
        Tag tag = Tag.builder().key(key).value(value).build();
        producerTemplate.requestBodyAndHeaders(
                componentUri(AWS2EC2Operations.createTags),
                null,
                Map.of(
                        AWS2EC2Constants.INSTANCES_IDS, List.of(instanceId),
                        AWS2EC2Constants.INSTANCES_TAGS, List.of(tag)));
        return Response.status(CREATED).build();
    }

    @Path("/instances/{instanceId}/tags")
    @DELETE
    public Response deleteTags(@PathParam("instanceId") String instanceId,
            @QueryParam("key") String key) {
        Tag tag = Tag.builder().key(key).build();
        producerTemplate.requestBodyAndHeaders(
                componentUri(AWS2EC2Operations.deleteTags),
                null,
                Map.of(
                        AWS2EC2Constants.INSTANCES_IDS, List.of(instanceId),
                        AWS2EC2Constants.INSTANCES_TAGS, List.of(tag)));
        return Response.noContent().build();
    }

    private String componentUri(AWS2EC2Operations operation) {
        return "aws2-ec2:ec2?operation=" + operation;
    }
}
