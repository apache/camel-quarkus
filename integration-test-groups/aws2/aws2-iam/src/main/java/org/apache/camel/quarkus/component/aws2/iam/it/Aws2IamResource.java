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
package org.apache.camel.quarkus.component.aws2.iam.it;

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
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.aws2.iam.IAM2Constants;
import org.apache.camel.component.aws2.iam.IAM2Operations;
import software.amazon.awssdk.services.iam.model.CreateGroupResponse;
import software.amazon.awssdk.services.iam.model.CreateUserResponse;
import software.amazon.awssdk.services.iam.model.GetUserResponse;
import software.amazon.awssdk.services.iam.model.ListGroupsResponse;
import software.amazon.awssdk.services.iam.model.ListUsersResponse;

@Path("/aws2-iam")
@ApplicationScoped
public class Aws2IamResource {

    @Inject
    ProducerTemplate producerTemplate;

    @Path("/users")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public Response createUser(@jakarta.ws.rs.QueryParam("userName") String userName) {
        CreateUserResponse response = producerTemplate.requestBodyAndHeader(
                componentUri(IAM2Operations.createUser),
                null,
                IAM2Constants.USERNAME,
                userName,
                CreateUserResponse.class);

        return Response.ok(response.user().userName()).build();
    }

    @Path("/users/{userName}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUser(@PathParam("userName") String userName) {
        GetUserResponse response = producerTemplate.requestBodyAndHeader(
                componentUri(IAM2Operations.getUser),
                null,
                IAM2Constants.USERNAME,
                userName,
                GetUserResponse.class);

        Map<String, String> result = Map.of(
                "userName", response.user().userName(),
                "userId", response.user().userId(),
                "arn", response.user().arn());
        return Response.ok(result).build();
    }

    @Path("/users")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response listUsers() {
        ListUsersResponse response = producerTemplate.requestBody(
                componentUri(IAM2Operations.listUsers),
                null,
                ListUsersResponse.class);

        List<String> userNames = response.users().stream()
                .map(user -> user.userName())
                .collect(Collectors.toList());

        return Response.ok(userNames).build();
    }

    @Path("/users/{userName}")
    @DELETE
    public Response deleteUser(@PathParam("userName") String userName) {
        producerTemplate.requestBodyAndHeader(
                componentUri(IAM2Operations.deleteUser),
                null,
                IAM2Constants.USERNAME,
                userName);

        return Response.noContent().build();
    }

    @Path("/groups")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public Response createGroup(@jakarta.ws.rs.QueryParam("groupName") String groupName) {
        CreateGroupResponse response = producerTemplate.requestBodyAndHeader(
                componentUri(IAM2Operations.createGroup),
                null,
                IAM2Constants.GROUP_NAME,
                groupName,
                CreateGroupResponse.class);

        return Response.ok(response.group().groupName()).build();
    }

    @Path("/groups")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response listGroups() {
        ListGroupsResponse response = producerTemplate.requestBody(
                componentUri(IAM2Operations.listGroups),
                null,
                ListGroupsResponse.class);

        List<String> groupNames = response.groups().stream()
                .map(group -> group.groupName())
                .collect(Collectors.toList());

        return Response.ok(groupNames).build();
    }

    @Path("/groups/{groupName}")
    @DELETE
    public Response deleteGroup(@PathParam("groupName") String groupName) {
        producerTemplate.requestBodyAndHeader(
                componentUri(IAM2Operations.deleteGroup),
                null,
                IAM2Constants.GROUP_NAME,
                groupName);

        return Response.noContent().build();
    }

    @Path("/groups/{groupName}/users/{userName}")
    @POST
    public Response addUserToGroup(@PathParam("groupName") String groupName,
            @PathParam("userName") String userName) {
        producerTemplate.requestBodyAndHeaders(
                componentUri(IAM2Operations.addUserToGroup),
                null,
                Map.of(
                        IAM2Constants.GROUP_NAME, groupName,
                        IAM2Constants.USERNAME, userName));

        return Response.noContent().build();
    }

    @Path("/groups/{groupName}/users/{userName}")
    @DELETE
    public Response removeUserFromGroup(@PathParam("groupName") String groupName,
            @PathParam("userName") String userName) {
        producerTemplate.requestBodyAndHeaders(
                componentUri(IAM2Operations.removeUserFromGroup),
                null,
                Map.of(
                        IAM2Constants.GROUP_NAME, groupName,
                        IAM2Constants.USERNAME, userName));

        return Response.noContent().build();
    }

    private String componentUri(IAM2Operations operation) {
        return "aws2-iam://test?operation=" + operation;
    }
}
