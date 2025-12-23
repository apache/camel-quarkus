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
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;

/**
 * Keycloak REST resource for client Operations (Client Scope, Client Role, Client Secret Management).
 */
@Path("/keycloak")
@ApplicationScoped
public class KeycloakClientResource extends KeycloakResourceSupport {

    // ==================== Client Operations ====================

    @Path("/client/{realmName}/{clientId}")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public Response createClientWithHeaders(
            @PathParam("realmName") String realmName,
            @PathParam("clientId") String clientId) {

        Map<String, Object> headers = new HashMap<>();
        headers.put(KeycloakConstants.REALM_NAME, realmName);
        headers.put(KeycloakConstants.CLIENT_ID, clientId);

        Object result = producerTemplate.requestBodyAndHeaders(
                getKeycloakEndpoint() + "&operation=createClient",
                null,
                headers);

        if (result instanceof Response jaxrsResponse) {
            return Response.status(jaxrsResponse.getStatus())
                    .entity("Client created successfully")
                    .build();
        }

        return Response.ok("Client created successfully").build();
    }

    @Path("/client/{realmName}/pojo")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response createClientWithPojo(
            @PathParam("realmName") String realmName,
            ClientRepresentation client) {

        Map<String, Object> headers = new HashMap<>();
        headers.put(KeycloakConstants.REALM_NAME, realmName);

        Object result = producerTemplate.requestBodyAndHeaders(
                getKeycloakEndpoint() + "&operation=createClient&pojoRequest=true",
                client,
                headers);

        if (result instanceof Response jaxrsResponse) {
            return Response.status(jaxrsResponse.getStatus())
                    .entity("Client created successfully")
                    .build();
        }

        return Response.ok("Client created successfully").build();
    }

    @Path("/client/{realmName}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response listClients(@PathParam("realmName") String realmName) {
        Map<String, Object> headers = new HashMap<>();
        headers.put(KeycloakConstants.REALM_NAME, realmName);

        @SuppressWarnings("unchecked")
        List<ClientRepresentation> result = producerTemplate.requestBodyAndHeaders(
                getKeycloakEndpoint() + "&operation=listClients",
                null,
                headers,
                List.class);

        return Response.ok(result).build();
    }

    @Path("/client/{realmName}/{clientId}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getClient(
            @PathParam("realmName") String realmName,
            @PathParam("clientId") String clientId) {

        // First, list clients to find the client UUID by clientId
        String clientUuid = getClientUuidByClientId(realmName, clientId);

        Map<String, Object> headers = new HashMap<>();
        headers.put(KeycloakConstants.REALM_NAME, realmName);
        headers.put(KeycloakConstants.CLIENT_UUID, clientUuid);

        ClientRepresentation result = producerTemplate.requestBodyAndHeaders(
                getKeycloakEndpoint() + "&operation=getClient",
                null,
                headers,
                ClientRepresentation.class);

        return Response.ok(result).build();
    }

    @Path("/client/{realmName}/{clientId}")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response updateClient(
            @PathParam("realmName") String realmName,
            @PathParam("clientId") String clientId,
            ClientRepresentation client) {

        // First, get the client UUID by clientId
        String clientUuid = getClientUuidByClientId(realmName, clientId);

        Map<String, Object> headers = new HashMap<>();
        headers.put(KeycloakConstants.REALM_NAME, realmName);
        headers.put(KeycloakConstants.CLIENT_UUID, clientUuid);

        // Set the ID in the client object
        client.setId(clientUuid);

        String result = producerTemplate.requestBodyAndHeaders(
                getKeycloakEndpoint() + "&operation=updateClient&pojoRequest=true",
                client,
                headers,
                String.class);

        return Response.ok(result).build();
    }

    @Path("/client/{realmName}/{clientId}")
    @DELETE
    @Produces(MediaType.TEXT_PLAIN)
    public Response deleteClient(
            @PathParam("realmName") String realmName,
            @PathParam("clientId") String clientId) {

        // First, get the client UUID by clientId
        String clientUuid = getClientUuidByClientId(realmName, clientId);

        Map<String, Object> headers = new HashMap<>();
        headers.put(KeycloakConstants.REALM_NAME, realmName);
        headers.put(KeycloakConstants.CLIENT_UUID, clientUuid);

        String result = producerTemplate.requestBodyAndHeaders(
                getKeycloakEndpoint() + "&operation=deleteClient",
                null,
                headers,
                String.class);

        return Response.ok(result).build();
    }

    // ==================== Client Role Operations ====================

    @Path("/client-role/{realmName}/{clientId}/{roleName}")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public Response createClientRoleWithHeaders(
            @PathParam("realmName") String realmName,
            @PathParam("clientId") String clientId,
            @PathParam("roleName") String roleName,
            @QueryParam("description") String description) {

        // Get client UUID
        String clientUuid = getClientUuidByClientId(realmName, clientId);

        Map<String, Object> headers = new HashMap<>();
        headers.put(KeycloakConstants.REALM_NAME, realmName);
        headers.put(KeycloakConstants.CLIENT_UUID, clientUuid);
        headers.put(KeycloakConstants.ROLE_NAME, roleName);
        if (description != null) {
            headers.put(KeycloakConstants.ROLE_DESCRIPTION, description);
        }

        String result = producerTemplate.requestBodyAndHeaders(
                getKeycloakEndpoint() + "&operation=createClientRole",
                null,
                headers,
                String.class);

        return Response.ok(result).build();
    }

    @Path("/client-role/{realmName}/{clientId}/pojo")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response createClientRoleWithPojo(
            @PathParam("realmName") String realmName,
            @PathParam("clientId") String clientId,
            RoleRepresentation role) {

        // Get client UUID
        String clientUuid = getClientUuidByClientId(realmName, clientId);

        Map<String, Object> headers = new HashMap<>();
        headers.put(KeycloakConstants.REALM_NAME, realmName);
        headers.put(KeycloakConstants.CLIENT_UUID, clientUuid);

        String result = producerTemplate.requestBodyAndHeaders(
                getKeycloakEndpoint() + "&operation=createClientRole&pojoRequest=true",
                role,
                headers,
                String.class);

        return Response.ok(result).build();
    }

    @Path("/client-role/{realmName}/{clientId}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response listClientRoles(
            @PathParam("realmName") String realmName,
            @PathParam("clientId") String clientId) {

        // Get client UUID
        String clientUuid = getClientUuidByClientId(realmName, clientId);

        Map<String, Object> headers = new HashMap<>();
        headers.put(KeycloakConstants.REALM_NAME, realmName);
        headers.put(KeycloakConstants.CLIENT_UUID, clientUuid);

        @SuppressWarnings("unchecked")
        List<RoleRepresentation> result = producerTemplate.requestBodyAndHeaders(
                getKeycloakEndpoint() + "&operation=listClientRoles",
                null,
                headers,
                List.class);

        return Response.ok(result).build();
    }

    @Path("/client-role/{realmName}/{clientId}/{roleName}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getClientRole(
            @PathParam("realmName") String realmName,
            @PathParam("clientId") String clientId,
            @PathParam("roleName") String roleName) {

        // Get client UUID
        String clientUuid = getClientUuidByClientId(realmName, clientId);

        Map<String, Object> headers = new HashMap<>();
        headers.put(KeycloakConstants.REALM_NAME, realmName);
        headers.put(KeycloakConstants.CLIENT_UUID, clientUuid);
        headers.put(KeycloakConstants.ROLE_NAME, roleName);

        RoleRepresentation result = producerTemplate.requestBodyAndHeaders(
                getKeycloakEndpoint() + "&operation=getClientRole",
                null,
                headers,
                RoleRepresentation.class);

        return Response.ok(result).build();
    }

    @Path("/client-role/{realmName}/{clientId}/{roleName}")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response updateClientRole(
            @PathParam("realmName") String realmName,
            @PathParam("clientId") String clientId,
            @PathParam("roleName") String roleName,
            RoleRepresentation role) {

        // Get client UUID
        String clientUuid = getClientUuidByClientId(realmName, clientId);

        Map<String, Object> headers = new HashMap<>();
        headers.put(KeycloakConstants.REALM_NAME, realmName);
        headers.put(KeycloakConstants.CLIENT_UUID, clientUuid);
        headers.put(KeycloakConstants.ROLE_NAME, roleName);

        // Set the role name
        role.setName(roleName);

        String result = producerTemplate.requestBodyAndHeaders(
                getKeycloakEndpoint() + "&operation=updateClientRole&pojoRequest=true",
                role,
                headers,
                String.class);

        return Response.ok(result).build();
    }

    @Path("/client-role/{realmName}/{clientId}/{roleName}")
    @DELETE
    @Produces(MediaType.TEXT_PLAIN)
    public Response deleteClientRole(
            @PathParam("realmName") String realmName,
            @PathParam("clientId") String clientId,
            @PathParam("roleName") String roleName) {

        // Get client UUID
        String clientUuid = getClientUuidByClientId(realmName, clientId);

        Map<String, Object> headers = new HashMap<>();
        headers.put(KeycloakConstants.REALM_NAME, realmName);
        headers.put(KeycloakConstants.CLIENT_UUID, clientUuid);
        headers.put(KeycloakConstants.ROLE_NAME, roleName);

        String result = producerTemplate.requestBodyAndHeaders(
                getKeycloakEndpoint() + "&operation=deleteClientRole",
                null,
                headers,
                String.class);

        return Response.ok(result).build();
    }

    // ==================== Client Role - User Assignment Operations ====================

    @Path("/client-role-user/{realmName}/{clientId}/{username}/{roleName}")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public Response assignClientRoleToUser(
            @PathParam("realmName") String realmName,
            @PathParam("clientId") String clientId,
            @PathParam("username") String username,
            @PathParam("roleName") String roleName) {

        // Get user ID and client UUID
        String userId = getUserIdByUsername(realmName, username);
        String clientUuid = getClientUuidByClientId(realmName, clientId);

        Map<String, Object> headers = new HashMap<>();
        headers.put(KeycloakConstants.REALM_NAME, realmName);
        headers.put(KeycloakConstants.USER_ID, userId);
        headers.put(KeycloakConstants.CLIENT_UUID, clientUuid);
        headers.put(KeycloakConstants.ROLE_NAME, roleName);

        String result = producerTemplate.requestBodyAndHeaders(
                getKeycloakEndpoint() + "&operation=assignClientRoleToUser",
                null,
                headers,
                String.class);

        return Response.ok(result).build();
    }

    @Path("/client-role-user/{realmName}/{clientId}/{username}/{roleName}")
    @DELETE
    @Produces(MediaType.TEXT_PLAIN)
    public Response removeClientRoleFromUser(
            @PathParam("realmName") String realmName,
            @PathParam("clientId") String clientId,
            @PathParam("username") String username,
            @PathParam("roleName") String roleName) {

        // Get user ID and client UUID
        String userId = getUserIdByUsername(realmName, username);
        String clientUuid = getClientUuidByClientId(realmName, clientId);

        Map<String, Object> headers = new HashMap<>();
        headers.put(KeycloakConstants.REALM_NAME, realmName);
        headers.put(KeycloakConstants.USER_ID, userId);
        headers.put(KeycloakConstants.CLIENT_UUID, clientUuid);
        headers.put(KeycloakConstants.ROLE_NAME, roleName);

        String result = producerTemplate.requestBodyAndHeaders(
                getKeycloakEndpoint() + "&operation=removeClientRoleFromUser",
                null,
                headers,
                String.class);

        return Response.ok(result).build();
    }

    // ==================== Client Scope Operations ====================

    @Path("/client-scope/{realmName}/{scopeName}")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public Response createClientScopeWithHeaders(
            @PathParam("realmName") String realmName,
            @PathParam("scopeName") String scopeName,
            @QueryParam("protocol") String protocol,
            @QueryParam("description") String description) {

        Map<String, Object> headers = new HashMap<>();
        headers.put(KeycloakConstants.REALM_NAME, realmName);
        headers.put(KeycloakConstants.CLIENT_SCOPE_NAME, scopeName);

        Object result = producerTemplate.requestBodyAndHeaders(
                getKeycloakEndpoint() + "&operation=createClientScope",
                null,
                headers);

        if (result instanceof Response jaxrsResponse) {
            return Response.status(jaxrsResponse.getStatus())
                    .entity("Client scope created successfully")
                    .build();
        }

        return Response.ok("Client scope created successfully").build();
    }

    @Path("/client-scope/{realmName}/pojo")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response createClientScopeWithPojo(
            @PathParam("realmName") String realmName,
            ClientScopeRepresentation clientScope) {

        Map<String, Object> headers = new HashMap<>();
        headers.put(KeycloakConstants.REALM_NAME, realmName);

        Object result = producerTemplate.requestBodyAndHeaders(
                getKeycloakEndpoint() + "&operation=createClientScope&pojoRequest=true",
                clientScope,
                headers);

        if (result instanceof Response jaxrsResponse) {
            return Response.status(jaxrsResponse.getStatus())
                    .entity("Client scope created successfully")
                    .build();
        }

        return Response.ok("Client scope created successfully").build();
    }

    @Path("/client-scope/{realmName}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response listClientScopes(@PathParam("realmName") String realmName) {
        Map<String, Object> headers = new HashMap<>();
        headers.put(KeycloakConstants.REALM_NAME, realmName);

        @SuppressWarnings("unchecked")
        List<ClientScopeRepresentation> result = producerTemplate.requestBodyAndHeaders(
                getKeycloakEndpoint() + "&operation=listClientScopes",
                null,
                headers,
                List.class);

        return Response.ok(result).build();
    }

    @Path("/client-scope/{realmName}/{scopeName}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getClientScope(
            @PathParam("realmName") String realmName,
            @PathParam("scopeName") String scopeName) {

        // First, list client scopes to find the scope ID by name
        String scopeId = getClientScopeIdByName(realmName, scopeName);

        Map<String, Object> headers = new HashMap<>();
        headers.put(KeycloakConstants.REALM_NAME, realmName);
        headers.put(KeycloakConstants.CLIENT_SCOPE_ID, scopeId);

        ClientScopeRepresentation result = producerTemplate.requestBodyAndHeaders(
                getKeycloakEndpoint() + "&operation=getClientScope",
                null,
                headers,
                ClientScopeRepresentation.class);

        return Response.ok(result).build();
    }

    @Path("/client-scope/{realmName}/{scopeName}")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response updateClientScope(
            @PathParam("realmName") String realmName,
            @PathParam("scopeName") String scopeName,
            ClientScopeRepresentation clientScope) {

        // First, get the client scope ID by name
        String scopeId = getClientScopeIdByName(realmName, scopeName);

        Map<String, Object> headers = new HashMap<>();
        headers.put(KeycloakConstants.REALM_NAME, realmName);
        headers.put(KeycloakConstants.CLIENT_SCOPE_ID, scopeId);

        // Set the ID in the client scope object
        clientScope.setId(scopeId);

        String result = producerTemplate.requestBodyAndHeaders(
                getKeycloakEndpoint() + "&operation=updateClientScope&pojoRequest=true",
                clientScope,
                headers,
                String.class);

        return Response.ok(result).build();
    }

    @Path("/client-scope/{realmName}/{scopeName}")
    @DELETE
    @Produces(MediaType.TEXT_PLAIN)
    public Response deleteClientScope(
            @PathParam("realmName") String realmName,
            @PathParam("scopeName") String scopeName) {

        // First, get the client scope ID by name
        String scopeId = getClientScopeIdByName(realmName, scopeName);

        Map<String, Object> headers = new HashMap<>();
        headers.put(KeycloakConstants.REALM_NAME, realmName);
        headers.put(KeycloakConstants.CLIENT_SCOPE_ID, scopeId);

        String result = producerTemplate.requestBodyAndHeaders(
                getKeycloakEndpoint() + "&operation=deleteClientScope",
                null,
                headers,
                String.class);

        return Response.ok(result).build();
    }

    // ==================== Client Secret Management Operations ====================

    @Path("/client-secret/{realmName}/{clientId}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getClientSecret(
            @PathParam("realmName") String realmName,
            @PathParam("clientId") String clientId) {

        // First, get the client UUID by clientId
        String clientUuid = getClientUuidByClientId(realmName, clientId);

        Map<String, Object> headers = new HashMap<>();
        headers.put(KeycloakConstants.REALM_NAME, realmName);
        headers.put(KeycloakConstants.CLIENT_UUID, clientUuid);

        CredentialRepresentation result = producerTemplate.requestBodyAndHeaders(
                getKeycloakEndpoint() + "&operation=getClientSecret",
                null,
                headers,
                CredentialRepresentation.class);

        return Response.ok(result).build();
    }

    @Path("/client-secret/{realmName}/{clientId}/regenerate")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Response regenerateClientSecret(
            @PathParam("realmName") String realmName,
            @PathParam("clientId") String clientId) {

        // First, get the client UUID by clientId
        String clientUuid = getClientUuidByClientId(realmName, clientId);

        Map<String, Object> headers = new HashMap<>();
        headers.put(KeycloakConstants.REALM_NAME, realmName);
        headers.put(KeycloakConstants.CLIENT_UUID, clientUuid);

        CredentialRepresentation result = producerTemplate.requestBodyAndHeaders(
                getKeycloakEndpoint() + "&operation=regenerateClientSecret",
                null,
                headers,
                CredentialRepresentation.class);

        return Response.ok(result).build();
    }
}
