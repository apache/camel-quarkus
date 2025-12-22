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
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.camel.component.keycloak.KeycloakConstants;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

/**
 * Keycloak REST resource for User operations.
 */
@Path("/keycloak")
@ApplicationScoped
public class KeycloakUserResource extends KeycloakResourceSupport {

    // ==================== User CRUD Operations ====================

    @Path("/user/{realmName}/{username}")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public Response createUserWithHeaders(
            @PathParam("realmName") String realmName,
            @PathParam("username") String username,
            @QueryParam("email") String email,
            @QueryParam("firstName") String firstName,
            @QueryParam("lastName") String lastName) {

        Map<String, Object> headers = new HashMap<>();
        headers.put(KeycloakConstants.REALM_NAME, realmName);
        headers.put(KeycloakConstants.USERNAME, username);
        headers.put(KeycloakConstants.USER_EMAIL, email);
        headers.put(KeycloakConstants.USER_FIRST_NAME, firstName);
        headers.put(KeycloakConstants.USER_LAST_NAME, lastName);

        Object result = producerTemplate.requestBodyAndHeaders(
                getKeycloakEndpoint() + "&operation=createUser",
                null,
                headers);

        if (result instanceof Response jaxrsResponse) {
            return Response.status(jaxrsResponse.getStatus())
                    .entity("User created successfully")
                    .build();
        }

        return Response.ok("User created successfully").build();
    }

    @Path("/user/{realmName}/pojo")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response createUserWithPojo(
            @PathParam("realmName") String realmName,
            UserRepresentation user) {

        Map<String, Object> headers = new HashMap<>();
        headers.put(KeycloakConstants.REALM_NAME, realmName);

        Object result = producerTemplate.requestBodyAndHeaders(
                getKeycloakEndpoint() + "&operation=createUser&pojoRequest=true",
                user,
                headers);

        if (result instanceof Response jaxrsResponse) {
            return Response.status(jaxrsResponse.getStatus())
                    .entity("User created successfully")
                    .build();
        }

        return Response.ok("User created successfully").build();
    }

    @Path("/user/{realmName}/{username}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUser(
            @PathParam("realmName") String realmName,
            @PathParam("username") String username) {

        String userId = getUserIdByUsername(realmName, username);

        Map<String, Object> headers = new HashMap<>();
        headers.put(KeycloakConstants.REALM_NAME, realmName);
        headers.put(KeycloakConstants.USER_ID, userId);

        UserRepresentation result = producerTemplate.requestBodyAndHeaders(
                getKeycloakEndpoint() + "&operation=getUser",
                null,
                headers,
                UserRepresentation.class);

        return Response.ok(result).build();
    }

    @Path("/user/{realmName}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response listUsers(@PathParam("realmName") String realmName) {
        Map<String, Object> headers = new HashMap<>();
        headers.put(KeycloakConstants.REALM_NAME, realmName);

        @SuppressWarnings("unchecked")
        List<UserRepresentation> result = producerTemplate.requestBodyAndHeaders(
                getKeycloakEndpoint() + "&operation=listUsers",
                null,
                headers,
                List.class);

        return Response.ok(result).build();
    }

    @Path("/user/{realmName}/{username}")
    @DELETE
    @Produces(MediaType.TEXT_PLAIN)
    public Response deleteUser(
            @PathParam("realmName") String realmName,
            @PathParam("username") String username) {

        String userId = getUserIdByUsername(realmName, username);

        Map<String, Object> headers = new HashMap<>();
        headers.put(KeycloakConstants.REALM_NAME, realmName);
        headers.put(KeycloakConstants.USER_ID, userId);

        producerTemplate.requestBodyAndHeaders(
                getKeycloakEndpoint() + "&operation=deleteUser",
                null,
                headers);

        return Response.ok("User deleted successfully").build();
    }

    @Path("/user/{realmName}/{username}")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response updateUser(
            @PathParam("realmName") String realmName,
            @PathParam("username") String username,
            UserRepresentation user) {

        String userId = getUserIdByUsername(realmName, username);

        Map<String, Object> headers = new HashMap<>();
        headers.put(KeycloakConstants.REALM_NAME, realmName);
        headers.put(KeycloakConstants.USER_ID, userId);

        user.setId(userId);

        producerTemplate.requestBodyAndHeaders(
                getKeycloakEndpoint() + "&operation=updateUser&pojoRequest=true",
                user,
                headers);

        return Response.ok("User updated successfully").build();
    }

    @Path("/user/{realmName}/search")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response searchUsers(
            @PathParam("realmName") String realmName,
            @QueryParam("query") String query) {

        Map<String, Object> headers = new HashMap<>();
        headers.put(KeycloakConstants.REALM_NAME, realmName);
        headers.put(KeycloakConstants.SEARCH_QUERY, query);

        @SuppressWarnings("unchecked")
        List<UserRepresentation> result = producerTemplate.requestBodyAndHeaders(
                getKeycloakEndpoint() + "&operation=searchUsers",
                null,
                headers,
                List.class);

        return Response.ok(result).build();
    }

    // ==================== User-Role Operations ====================

    @Path("/user-role/{realmName}/{username}/{roleName}")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public Response assignRoleToUser(
            @PathParam("realmName") String realmName,
            @PathParam("username") String username,
            @PathParam("roleName") String roleName) {

        String userId = getUserIdByUsername(realmName, username);

        Map<String, Object> headers = new HashMap<>();
        headers.put(KeycloakConstants.REALM_NAME, realmName);
        headers.put(KeycloakConstants.USER_ID, userId);
        headers.put(KeycloakConstants.ROLE_NAME, roleName);

        String result = producerTemplate.requestBodyAndHeaders(
                getKeycloakEndpoint() + "&operation=assignRoleToUser",
                null,
                headers,
                String.class);

        return Response.ok(result).build();
    }

    @Path("/user-role/{realmName}/{username}/{roleName}")
    @DELETE
    @Produces(MediaType.TEXT_PLAIN)
    public Response removeRoleFromUser(
            @PathParam("realmName") String realmName,
            @PathParam("username") String username,
            @PathParam("roleName") String roleName) {

        String userId = getUserIdByUsername(realmName, username);

        Map<String, Object> headers = new HashMap<>();
        headers.put(KeycloakConstants.REALM_NAME, realmName);
        headers.put(KeycloakConstants.USER_ID, userId);
        headers.put(KeycloakConstants.ROLE_NAME, roleName);

        String result = producerTemplate.requestBodyAndHeaders(
                getKeycloakEndpoint() + "&operation=removeRoleFromUser",
                null,
                headers,
                String.class);

        return Response.ok(result).build();
    }

    @Path("/user-role/{realmName}/{username}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUserRoles(
            @PathParam("realmName") String realmName,
            @PathParam("username") String username) {

        String userId = getUserIdByUsername(realmName, username);

        Map<String, Object> headers = new HashMap<>();
        headers.put(KeycloakConstants.REALM_NAME, realmName);
        headers.put(KeycloakConstants.USER_ID, userId);

        @SuppressWarnings("unchecked")
        List<RoleRepresentation> result = producerTemplate.requestBodyAndHeaders(
                getKeycloakEndpoint() + "&operation=getUserRoles",
                null,
                headers,
                List.class);

        return Response.ok(result).build();
    }

    // ==================== User Attribute Operations ====================

    @Path("/user-attribute/{realmName}/{username}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUserAttributes(
            @PathParam("realmName") String realmName,
            @PathParam("username") String username) {

        String userId = getUserIdByUsername(realmName, username);

        Map<String, Object> headers = new HashMap<>();
        headers.put(KeycloakConstants.REALM_NAME, realmName);
        headers.put(KeycloakConstants.USER_ID, userId);

        @SuppressWarnings("unchecked")
        Map<String, List<String>> result = producerTemplate.requestBodyAndHeaders(
                getKeycloakEndpoint() + "&operation=getUserAttributes",
                null,
                headers,
                Map.class);

        return Response.ok(result).build();
    }

    @Path("/user-attribute/{realmName}/{username}/{attributeName}")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public Response setUserAttribute(
            @PathParam("realmName") String realmName,
            @PathParam("username") String username,
            @PathParam("attributeName") String attributeName,
            @QueryParam("attributeValue") String attributeValue) {

        String userId = getUserIdByUsername(realmName, username);

        Map<String, Object> headers = new HashMap<>();
        headers.put(KeycloakConstants.REALM_NAME, realmName);
        headers.put(KeycloakConstants.USER_ID, userId);
        headers.put(KeycloakConstants.ATTRIBUTE_NAME, attributeName);
        headers.put(KeycloakConstants.ATTRIBUTE_VALUE, attributeValue);

        String result = producerTemplate.requestBodyAndHeaders(
                getKeycloakEndpoint() + "&operation=setUserAttribute",
                null,
                headers,
                String.class);

        return Response.ok(result).build();
    }

    @Path("/user-attribute/{realmName}/{username}/{attributeName}")
    @DELETE
    @Produces(MediaType.TEXT_PLAIN)
    public Response deleteUserAttribute(
            @PathParam("realmName") String realmName,
            @PathParam("username") String username,
            @PathParam("attributeName") String attributeName) {

        String userId = getUserIdByUsername(realmName, username);

        Map<String, Object> headers = new HashMap<>();
        headers.put(KeycloakConstants.REALM_NAME, realmName);
        headers.put(KeycloakConstants.USER_ID, userId);
        headers.put(KeycloakConstants.ATTRIBUTE_NAME, attributeName);

        String result = producerTemplate.requestBodyAndHeaders(
                getKeycloakEndpoint() + "&operation=deleteUserAttribute",
                null,
                headers,
                String.class);

        return Response.ok(result).build();
    }

    // ==================== User Credential Operations ====================

    @Path("/user-credential/{realmName}/{username}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUserCredentials(
            @PathParam("realmName") String realmName,
            @PathParam("username") String username) {

        String userId = getUserIdByUsername(realmName, username);

        Map<String, Object> headers = new HashMap<>();
        headers.put(KeycloakConstants.REALM_NAME, realmName);
        headers.put(KeycloakConstants.USER_ID, userId);

        @SuppressWarnings("unchecked")
        List<CredentialRepresentation> result = producerTemplate.requestBodyAndHeaders(
                getKeycloakEndpoint() + "&operation=getUserCredentials",
                null,
                headers,
                List.class);

        return Response.ok(result).build();
    }

    @Path("/user-credential/{realmName}/{username}/{credentialId}")
    @DELETE
    @Produces(MediaType.TEXT_PLAIN)
    public Response deleteUserCredential(
            @PathParam("realmName") String realmName,
            @PathParam("username") String username,
            @PathParam("credentialId") String credentialId) {

        String userId = getUserIdByUsername(realmName, username);

        Map<String, Object> headers = new HashMap<>();
        headers.put(KeycloakConstants.REALM_NAME, realmName);
        headers.put(KeycloakConstants.USER_ID, userId);
        headers.put(KeycloakConstants.CREDENTIAL_ID, credentialId);

        String result = producerTemplate.requestBodyAndHeaders(
                getKeycloakEndpoint() + "&operation=deleteUserCredential",
                null,
                headers,
                String.class);

        return Response.ok(result).build();
    }

    // ==================== User Action Operations ====================

    @Path("/user-action/{realmName}/{username}/add")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public Response addRequiredAction(
            @PathParam("realmName") String realmName,
            @PathParam("username") String username,
            @QueryParam("action") String action) {

        String userId = getUserIdByUsername(realmName, username);

        Map<String, Object> headers = new HashMap<>();
        headers.put(KeycloakConstants.REALM_NAME, realmName);
        headers.put(KeycloakConstants.USER_ID, userId);
        headers.put(KeycloakConstants.REQUIRED_ACTION, action);

        String result = producerTemplate.requestBodyAndHeaders(
                getKeycloakEndpoint() + "&operation=addRequiredAction",
                null,
                headers,
                String.class);

        return Response.ok(result).build();
    }

    @Path("/user-action/{realmName}/{username}/remove")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public Response removeRequiredAction(
            @PathParam("realmName") String realmName,
            @PathParam("username") String username,
            @QueryParam("action") String action) {

        String userId = getUserIdByUsername(realmName, username);

        Map<String, Object> headers = new HashMap<>();
        headers.put(KeycloakConstants.REALM_NAME, realmName);
        headers.put(KeycloakConstants.USER_ID, userId);
        headers.put(KeycloakConstants.REQUIRED_ACTION, action);

        String result = producerTemplate.requestBodyAndHeaders(
                getKeycloakEndpoint() + "&operation=removeRequiredAction",
                null,
                headers,
                String.class);

        return Response.ok(result).build();
    }

    @Path("/user-action/{realmName}/{username}/execute")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public Response executeActionsEmail(
            @PathParam("realmName") String realmName,
            @PathParam("username") String username,
            @QueryParam("actions") String actions,
            @QueryParam("redirectUri") String redirectUri,
            @QueryParam("lifespan") Integer lifespan) {

        String userId = getUserIdByUsername(realmName, username);

        Map<String, Object> headers = new HashMap<>();
        headers.put(KeycloakConstants.REALM_NAME, realmName);
        headers.put(KeycloakConstants.USER_ID, userId);

        if (actions != null && !actions.isEmpty()) {
            List<String> actionList = List.of(actions.split(","));
            headers.put(KeycloakConstants.ACTIONS, actionList);
        }

        if (redirectUri != null) {
            headers.put(KeycloakConstants.REDIRECT_URI, redirectUri);
        }

        if (lifespan != null) {
            headers.put(KeycloakConstants.LIFESPAN, lifespan);
        }

        String result = producerTemplate.requestBodyAndHeaders(
                getKeycloakEndpoint() + "&operation=executeActionsEmail",
                null,
                headers,
                String.class);

        return Response.ok(result).build();
    }

    // ==================== User Session Operations ====================

    @Path("/user/{realmName}/{username}/sessions")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response listUserSessions(
            @PathParam("realmName") String realmName,
            @PathParam("username") String username) {

        String userId = getUserIdByUsername(realmName, username);

        Map<String, Object> headers = new HashMap<>();
        headers.put(KeycloakConstants.REALM_NAME, realmName);
        headers.put(KeycloakConstants.USER_ID, userId);

        var result = producerTemplate.requestBodyAndHeaders(
                getKeycloakEndpoint() + "&operation=listUserSessions",
                null,
                headers,
                List.class);

        return Response.ok(result).build();
    }

    @Path("/user/{realmName}/{username}/logout")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public Response logoutUser(
            @PathParam("realmName") String realmName,
            @PathParam("username") String username) {

        String userId = getUserIdByUsername(realmName, username);

        Map<String, Object> headers = new HashMap<>();
        headers.put(KeycloakConstants.REALM_NAME, realmName);
        headers.put(KeycloakConstants.USER_ID, userId);

        producerTemplate.requestBodyAndHeaders(
                getKeycloakEndpoint() + "&operation=logoutUser",
                null,
                headers);

        return Response.ok("User logged out successfully").build();
    }

    // ==================== User Password Operations ====================

    @Path("/user/{realmName}/{username}/reset-password")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public Response resetUserPassword(
            @PathParam("realmName") String realmName,
            @PathParam("username") String username,
            @QueryParam("password") String password,
            @QueryParam("temporary") Boolean temporary) {

        String userId = getUserIdByUsername(realmName, username);

        Map<String, Object> headers = new HashMap<>();
        headers.put(KeycloakConstants.REALM_NAME, realmName);
        headers.put(KeycloakConstants.USER_ID, userId);
        headers.put(KeycloakConstants.USER_PASSWORD, password);
        if (temporary != null) {
            headers.put(KeycloakConstants.PASSWORD_TEMPORARY, temporary);
        }

        producerTemplate.requestBodyAndHeaders(
                getKeycloakEndpoint() + "&operation=resetUserPassword",
                null,
                headers);

        return Response.ok("Password reset successfully").build();
    }

    // ==================== User Email Operations ====================

    @Path("/user/{realmName}/{username}/send-verify-email")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public Response sendVerifyEmail(
            @PathParam("realmName") String realmName,
            @PathParam("username") String username) {

        String userId = getUserIdByUsername(realmName, username);

        Map<String, Object> headers = new HashMap<>();
        headers.put(KeycloakConstants.REALM_NAME, realmName);
        headers.put(KeycloakConstants.USER_ID, userId);

        producerTemplate.requestBodyAndHeaders(
                getKeycloakEndpoint() + "&operation=sendVerifyEmail",
                null,
                headers);

        return Response.ok("Verify email sent successfully").build();
    }

    @Path("/user/{realmName}/{username}/send-password-reset-email")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public Response sendPasswordResetEmail(
            @PathParam("realmName") String realmName,
            @PathParam("username") String username) {

        String userId = getUserIdByUsername(realmName, username);

        Map<String, Object> headers = new HashMap<>();
        headers.put(KeycloakConstants.REALM_NAME, realmName);
        headers.put(KeycloakConstants.USER_ID, userId);

        producerTemplate.requestBodyAndHeaders(
                getKeycloakEndpoint() + "&operation=sendPasswordResetEmail",
                null,
                headers);

        return Response.ok("Password reset email sent successfully").build();
    }
}
