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
import org.keycloak.representations.idm.RoleRepresentation;

/**
 * Keycloak REST resource for Role operations.
 */
@Path("/keycloak/role")
@ApplicationScoped
public class KeycloakRoleResource extends KeycloakResourceSupport {

    @Path("/{realmName}/{roleName}")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public Response createRoleWithHeaders(
            @PathParam("realmName") String realmName,
            @PathParam("roleName") String roleName,
            @QueryParam("description") String description) {

        Map<String, Object> headers = new HashMap<>();
        headers.put(KeycloakConstants.REALM_NAME, realmName);
        headers.put(KeycloakConstants.ROLE_NAME, roleName);
        if (description != null) {
            headers.put(KeycloakConstants.ROLE_DESCRIPTION, description);
        }

        String result = producerTemplate.requestBodyAndHeaders(
                getKeycloakEndpoint() + "&operation=createRole",
                null,
                headers,
                String.class);

        return Response.ok(result).build();
    }

    @Path("/{realmName}/pojo")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response createRoleWithPojo(
            @PathParam("realmName") String realmName,
            RoleRepresentation role) {

        Map<String, Object> headers = new HashMap<>();
        headers.put(KeycloakConstants.REALM_NAME, realmName);

        String result = producerTemplate.requestBodyAndHeaders(
                getKeycloakEndpoint() + "&operation=createRole&pojoRequest=true",
                role,
                headers,
                String.class);

        return Response.ok(result).build();
    }

    @Path("/{realmName}/{roleName}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getRole(
            @PathParam("realmName") String realmName,
            @PathParam("roleName") String roleName) {

        Map<String, Object> headers = new HashMap<>();
        headers.put(KeycloakConstants.REALM_NAME, realmName);
        headers.put(KeycloakConstants.ROLE_NAME, roleName);

        RoleRepresentation result = producerTemplate.requestBodyAndHeaders(
                getKeycloakEndpoint() + "&operation=getRole",
                null,
                headers,
                RoleRepresentation.class);

        return Response.ok(result).build();
    }

    @Path("/{realmName}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response listRoles(@PathParam("realmName") String realmName) {
        Map<String, Object> headers = new HashMap<>();
        headers.put(KeycloakConstants.REALM_NAME, realmName);

        @SuppressWarnings("unchecked")
        List<RoleRepresentation> result = producerTemplate.requestBodyAndHeaders(
                getKeycloakEndpoint() + "&operation=listRoles",
                null,
                headers,
                List.class);

        return Response.ok(result).build();
    }

    @Path("/{realmName}/{roleName}")
    @DELETE
    @Produces(MediaType.TEXT_PLAIN)
    public Response deleteRole(
            @PathParam("realmName") String realmName,
            @PathParam("roleName") String roleName) {

        Map<String, Object> headers = new HashMap<>();
        headers.put(KeycloakConstants.REALM_NAME, realmName);
        headers.put(KeycloakConstants.ROLE_NAME, roleName);

        String result = producerTemplate.requestBodyAndHeaders(
                getKeycloakEndpoint() + "&operation=deleteRole",
                null,
                headers,
                String.class);

        return Response.ok(result).build();
    }

    @Path("/{realmName}/{roleName}")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response updateRole(
            @PathParam("realmName") String realmName,
            @PathParam("roleName") String roleName,
            RoleRepresentation role) {

        Map<String, Object> headers = new HashMap<>();
        headers.put(KeycloakConstants.REALM_NAME, realmName);
        headers.put(KeycloakConstants.ROLE_NAME, roleName);

        role.setName(roleName);

        String result = producerTemplate.requestBodyAndHeaders(
                getKeycloakEndpoint() + "&operation=updateRole&pojoRequest=true",
                role,
                headers,
                String.class);

        return Response.ok(result).build();
    }
}
