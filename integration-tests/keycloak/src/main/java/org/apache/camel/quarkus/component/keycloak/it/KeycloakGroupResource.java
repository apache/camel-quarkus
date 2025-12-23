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
package org.apache.camel.quarkus.component.keycloak.it;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.camel.component.keycloak.KeycloakConstants;
import org.keycloak.representations.idm.GroupRepresentation;

/**
 * Keycloak REST resource for Group Operations.
 */
@Path("/keycloak")
@ApplicationScoped
public class KeycloakGroupResource extends KeycloakResourceSupport {

    // ==================== Group Operations ====================

    @Path("/group/{realmName}/{groupName}")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public Response createGroupWithHeaders(
            @PathParam("realmName") String realmName,
            @PathParam("groupName") String groupName) {

        Map<String, Object> headers = new HashMap<>();
        headers.put(KeycloakConstants.REALM_NAME, realmName);
        headers.put(KeycloakConstants.GROUP_NAME, groupName);

        Object result = producerTemplate.requestBodyAndHeaders(
                getKeycloakEndpoint() + "&operation=createGroup",
                null,
                headers);

        if (result instanceof Response jaxrsResponse) {
            return Response.status(jaxrsResponse.getStatus())
                    .entity("Group created successfully")
                    .build();
        }

        return Response.ok("Group created successfully").build();
    }

    @Path("/group/{realmName}/pojo")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response createGroupWithPojo(
            @PathParam("realmName") String realmName,
            GroupRepresentation group) {

        Map<String, Object> headers = new HashMap<>();
        headers.put(KeycloakConstants.REALM_NAME, realmName);

        Object result = producerTemplate.requestBodyAndHeaders(
                getKeycloakEndpoint() + "&operation=createGroup&pojoRequest=true",
                group,
                headers);

        if (result instanceof Response jaxrsResponse) {
            return Response.status(jaxrsResponse.getStatus())
                    .entity("Group created successfully")
                    .build();
        }

        return Response.ok("Group created successfully").build();
    }

    @Path("/group/{realmName}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response listGroups(@PathParam("realmName") String realmName) {
        Map<String, Object> headers = new HashMap<>();
        headers.put(KeycloakConstants.REALM_NAME, realmName);

        @SuppressWarnings("unchecked")
        List<GroupRepresentation> result = producerTemplate.requestBodyAndHeaders(
                getKeycloakEndpoint() + "&operation=listGroups",
                null,
                headers,
                List.class);

        return Response.ok(result).build();
    }

    @Path("/group/{realmName}/{groupName}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getGroup(
            @PathParam("realmName") String realmName,
            @PathParam("groupName") String groupName) {

        String groupId = getGroupIdByName(realmName, groupName);

        Map<String, Object> headers = new HashMap<>();
        headers.put(KeycloakConstants.REALM_NAME, realmName);
        headers.put(KeycloakConstants.GROUP_ID, groupId);

        GroupRepresentation result = producerTemplate.requestBodyAndHeaders(
                getKeycloakEndpoint() + "&operation=getGroup",
                null,
                headers,
                GroupRepresentation.class);

        return Response.ok(result).build();
    }

    @Path("/group/{realmName}/{groupName}")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response updateGroup(
            @PathParam("realmName") String realmName,
            @PathParam("groupName") String groupName,
            GroupRepresentation group) {

        String groupId = getGroupIdByName(realmName, groupName);

        Map<String, Object> headers = new HashMap<>();
        headers.put(KeycloakConstants.REALM_NAME, realmName);
        headers.put(KeycloakConstants.GROUP_ID, groupId);

        group.setId(groupId);

        String result = producerTemplate.requestBodyAndHeaders(
                getKeycloakEndpoint() + "&operation=updateGroup&pojoRequest=true",
                group,
                headers,
                String.class);

        return Response.ok(result).build();
    }

    @Path("/group/{realmName}/{groupName}")
    @DELETE
    @Produces(MediaType.TEXT_PLAIN)
    public Response deleteGroup(
            @PathParam("realmName") String realmName,
            @PathParam("groupName") String groupName) {

        String groupId = getGroupIdByName(realmName, groupName);

        Map<String, Object> headers = new HashMap<>();
        headers.put(KeycloakConstants.REALM_NAME, realmName);
        headers.put(KeycloakConstants.GROUP_ID, groupId);

        String result = producerTemplate.requestBodyAndHeaders(
                getKeycloakEndpoint() + "&operation=deleteGroup",
                null,
                headers,
                String.class);

        return Response.ok(result).build();
    }

    // ==================== Group-User Operations ====================

    @Path("/group-user/{realmName}/{username}/{groupName}")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public Response addUserToGroup(
            @PathParam("realmName") String realmName,
            @PathParam("username") String username,
            @PathParam("groupName") String groupName) {

        String userId = getUserIdByUsername(realmName, username);
        String groupId = getGroupIdByName(realmName, groupName);

        Map<String, Object> headers = new HashMap<>();
        headers.put(KeycloakConstants.REALM_NAME, realmName);
        headers.put(KeycloakConstants.USER_ID, userId);
        headers.put(KeycloakConstants.GROUP_ID, groupId);

        String result = producerTemplate.requestBodyAndHeaders(
                getKeycloakEndpoint() + "&operation=addUserToGroup",
                null,
                headers,
                String.class);

        return Response.ok(result).build();
    }

    @Path("/group-user/{realmName}/{username}/{groupName}")
    @DELETE
    @Produces(MediaType.TEXT_PLAIN)
    public Response removeUserFromGroup(
            @PathParam("realmName") String realmName,
            @PathParam("username") String username,
            @PathParam("groupName") String groupName) {

        String userId = getUserIdByUsername(realmName, username);
        String groupId = getGroupIdByName(realmName, groupName);

        Map<String, Object> headers = new HashMap<>();
        headers.put(KeycloakConstants.REALM_NAME, realmName);
        headers.put(KeycloakConstants.USER_ID, userId);
        headers.put(KeycloakConstants.GROUP_ID, groupId);

        String result = producerTemplate.requestBodyAndHeaders(
                getKeycloakEndpoint() + "&operation=removeUserFromGroup",
                null,
                headers,
                String.class);

        return Response.ok(result).build();
    }

    @Path("/group-user/{realmName}/{username}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response listUserGroups(
            @PathParam("realmName") String realmName,
            @PathParam("username") String username) {

        String userId = getUserIdByUsername(realmName, username);

        Map<String, Object> headers = new HashMap<>();
        headers.put(KeycloakConstants.REALM_NAME, realmName);
        headers.put(KeycloakConstants.USER_ID, userId);

        @SuppressWarnings("unchecked")
        List<GroupRepresentation> result = producerTemplate.requestBodyAndHeaders(
                getKeycloakEndpoint() + "&operation=listUserGroups",
                null,
                headers,
                List.class);

        return Response.ok(result).build();
    }
}
