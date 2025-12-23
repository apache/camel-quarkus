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
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.keycloak.representations.idm.authorization.ResourcePermissionRepresentation;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;

/**
 * Keycloak REST resource for Authorization Services (Resource, Resource Policy, Resource Permission).
 */
@Path("/keycloak")
@ApplicationScoped
public class KeycloakAuthorizationServicesResource extends KeycloakResourceSupport {

    @Path("/resource/{realmName}/{clientId}/pojo")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response createResource(
            @PathParam("realmName") String realmName,
            @PathParam("clientId") String clientId,
            ResourceRepresentation resource) {

        String clientUuid = getClientUuidByClientId(realmName, clientId);

        Map<String, Object> headers = new HashMap<>();
        headers.put(KeycloakConstants.REALM_NAME, realmName);
        headers.put(KeycloakConstants.CLIENT_UUID, clientUuid);

        Object result = producerTemplate.requestBodyAndHeaders(
                getKeycloakEndpoint() + "&operation=createResource&pojoRequest=true",
                resource,
                headers);

        if (result instanceof Response jaxrsResponse) {
            String location = jaxrsResponse.getHeaderString("Location");
            Response.ResponseBuilder builder = Response.status(jaxrsResponse.getStatus())
                    .entity("Authorization resource created successfully");
            if (location != null) {
                builder.header("Location", location);
            }
            return builder.build();
        }

        return Response.status(201).entity("Authorization resource created successfully").build();
    }

    @Path("/resource/{realmName}/{clientId}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response listResources(
            @PathParam("realmName") String realmName,
            @PathParam("clientId") String clientId) {

        String clientUuid = getClientUuidByClientId(realmName, clientId);

        Map<String, Object> headers = new HashMap<>();
        headers.put(KeycloakConstants.REALM_NAME, realmName);
        headers.put(KeycloakConstants.CLIENT_UUID, clientUuid);

        @SuppressWarnings("unchecked")
        List<ResourceRepresentation> result = producerTemplate.requestBodyAndHeaders(
                getKeycloakEndpoint() + "&operation=listResources",
                null,
                headers,
                List.class);

        return Response.ok(result).build();
    }

    @Path("/resource/{realmName}/{clientId}/{resourceId}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getResource(
            @PathParam("realmName") String realmName,
            @PathParam("clientId") String clientId,
            @PathParam("resourceId") String resourceId) {

        String clientUuid = getClientUuidByClientId(realmName, clientId);

        Map<String, Object> headers = new HashMap<>();
        headers.put(KeycloakConstants.REALM_NAME, realmName);
        headers.put(KeycloakConstants.CLIENT_UUID, clientUuid);
        headers.put(KeycloakConstants.RESOURCE_ID, resourceId);

        ResourceRepresentation result = producerTemplate.requestBodyAndHeaders(
                getKeycloakEndpoint() + "&operation=getResource",
                null,
                headers,
                ResourceRepresentation.class);

        return Response.ok(result).build();
    }

    @Path("/resource/{realmName}/{clientId}/{resourceId}")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response updateResource(
            @PathParam("realmName") String realmName,
            @PathParam("clientId") String clientId,
            @PathParam("resourceId") String resourceId,
            ResourceRepresentation resource) {

        String clientUuid = getClientUuidByClientId(realmName, clientId);

        Map<String, Object> headers = new HashMap<>();
        headers.put(KeycloakConstants.REALM_NAME, realmName);
        headers.put(KeycloakConstants.CLIENT_UUID, clientUuid);
        headers.put(KeycloakConstants.RESOURCE_ID, resourceId);

        resource.setId(resourceId);

        String result = producerTemplate.requestBodyAndHeaders(
                getKeycloakEndpoint() + "&operation=updateResource&pojoRequest=true",
                resource,
                headers,
                String.class);

        return Response.ok(result).build();
    }

    @Path("/resource/{realmName}/{clientId}/{resourceId}")
    @DELETE
    @Produces(MediaType.TEXT_PLAIN)
    public Response deleteResource(
            @PathParam("realmName") String realmName,
            @PathParam("clientId") String clientId,
            @PathParam("resourceId") String resourceId) {

        String clientUuid = getClientUuidByClientId(realmName, clientId);

        Map<String, Object> headers = new HashMap<>();
        headers.put(KeycloakConstants.REALM_NAME, realmName);
        headers.put(KeycloakConstants.CLIENT_UUID, clientUuid);
        headers.put(KeycloakConstants.RESOURCE_ID, resourceId);

        String result = producerTemplate.requestBodyAndHeaders(
                getKeycloakEndpoint() + "&operation=deleteResource",
                null,
                headers,
                String.class);

        return Response.ok(result).build();
    }

    // ==================== Authorization Services - Policy Operations ====================

    @Path("/resource-policy/{realmName}/{clientId}/pojo")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response createResourcePolicy(
            @PathParam("realmName") String realmName,
            @PathParam("clientId") String clientId,
            PolicyRepresentation policy) {

        String clientUuid = getClientUuidByClientId(realmName, clientId);

        Map<String, Object> headers = new HashMap<>();
        headers.put(KeycloakConstants.REALM_NAME, realmName);
        headers.put(KeycloakConstants.CLIENT_UUID, clientUuid);

        Object result = producerTemplate.requestBodyAndHeaders(
                getKeycloakEndpoint() + "&operation=createResourcePolicy&pojoRequest=true",
                policy,
                headers);

        if (result instanceof Response jaxrsResponse) {
            String location = jaxrsResponse.getHeaderString("Location");
            Response.ResponseBuilder builder = Response.status(jaxrsResponse.getStatus())
                    .entity("Authorization policy created successfully");
            if (location != null) {
                builder.header("Location", location);
            }
            return builder.build();
        }

        return Response.status(201).entity("Authorization policy created successfully").build();
    }

    @Path("/resource-policy/{realmName}/{clientId}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response listResourcePolicies(
            @PathParam("realmName") String realmName,
            @PathParam("clientId") String clientId) {

        String clientUuid = getClientUuidByClientId(realmName, clientId);

        Map<String, Object> headers = new HashMap<>();
        headers.put(KeycloakConstants.REALM_NAME, realmName);
        headers.put(KeycloakConstants.CLIENT_UUID, clientUuid);

        @SuppressWarnings("unchecked")
        List<PolicyRepresentation> result = producerTemplate.requestBodyAndHeaders(
                getKeycloakEndpoint() + "&operation=listResourcePolicies",
                null,
                headers,
                List.class);

        return Response.ok(result).build();
    }

    @Path("/resource-policy/{realmName}/{clientId}/{policyId}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getResourcePolicy(
            @PathParam("realmName") String realmName,
            @PathParam("clientId") String clientId,
            @PathParam("policyId") String policyId) {

        String clientUuid = getClientUuidByClientId(realmName, clientId);

        Map<String, Object> headers = new HashMap<>();
        headers.put(KeycloakConstants.REALM_NAME, realmName);
        headers.put(KeycloakConstants.CLIENT_UUID, clientUuid);
        headers.put(KeycloakConstants.POLICY_ID, policyId);

        PolicyRepresentation result = producerTemplate.requestBodyAndHeaders(
                getKeycloakEndpoint() + "&operation=getResourcePolicy",
                null,
                headers,
                PolicyRepresentation.class);

        return Response.ok(result).build();
    }

    @Path("/resource-policy/{realmName}/{clientId}/{policyId}")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response updateResourcePolicy(
            @PathParam("realmName") String realmName,
            @PathParam("clientId") String clientId,
            @PathParam("policyId") String policyId,
            PolicyRepresentation policy) {

        String clientUuid = getClientUuidByClientId(realmName, clientId);

        Map<String, Object> headers = new HashMap<>();
        headers.put(KeycloakConstants.REALM_NAME, realmName);
        headers.put(KeycloakConstants.CLIENT_UUID, clientUuid);
        headers.put(KeycloakConstants.POLICY_ID, policyId);

        policy.setId(policyId);

        String result = producerTemplate.requestBodyAndHeaders(
                getKeycloakEndpoint() + "&operation=updateResourcePolicy&pojoRequest=true",
                policy,
                headers,
                String.class);

        return Response.ok(result).build();
    }

    @Path("/resource-policy/{realmName}/{clientId}/{policyId}")
    @DELETE
    @Produces(MediaType.TEXT_PLAIN)
    public Response deleteResourcePolicy(
            @PathParam("realmName") String realmName,
            @PathParam("clientId") String clientId,
            @PathParam("policyId") String policyId) {

        String clientUuid = getClientUuidByClientId(realmName, clientId);

        Map<String, Object> headers = new HashMap<>();
        headers.put(KeycloakConstants.REALM_NAME, realmName);
        headers.put(KeycloakConstants.CLIENT_UUID, clientUuid);
        headers.put(KeycloakConstants.POLICY_ID, policyId);

        String result = producerTemplate.requestBodyAndHeaders(
                getKeycloakEndpoint() + "&operation=deleteResourcePolicy",
                null,
                headers,
                String.class);

        return Response.ok(result).build();
    }

    // ==================== Authorization Services - Permission Operations ====================

    @Path("/resource-permission/{realmName}/{clientId}/pojo")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response createResourcePermission(
            @PathParam("realmName") String realmName,
            @PathParam("clientId") String clientId,
            ResourcePermissionRepresentation permission) {

        String clientUuid = getClientUuidByClientId(realmName, clientId);

        Map<String, Object> headers = new HashMap<>();
        headers.put(KeycloakConstants.REALM_NAME, realmName);
        headers.put(KeycloakConstants.CLIENT_UUID, clientUuid);

        Object result = producerTemplate.requestBodyAndHeaders(
                getKeycloakEndpoint() + "&operation=createResourcePermission&pojoRequest=true",
                permission,
                headers);

        if (result instanceof Response jaxrsResponse) {
            String location = jaxrsResponse.getHeaderString("Location");
            Response.ResponseBuilder builder = Response.status(jaxrsResponse.getStatus())
                    .entity("Authorization permission created successfully");
            if (location != null) {
                builder.header("Location", location);
            }
            return builder.build();
        }

        return Response.status(201).entity("Authorization permission created successfully").build();
    }

    @Path("/resource-permission/{realmName}/{clientId}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response listResourcePermissions(
            @PathParam("realmName") String realmName,
            @PathParam("clientId") String clientId) {

        String clientUuid = getClientUuidByClientId(realmName, clientId);

        Map<String, Object> headers = new HashMap<>();
        headers.put(KeycloakConstants.REALM_NAME, realmName);
        headers.put(KeycloakConstants.CLIENT_UUID, clientUuid);

        @SuppressWarnings("unchecked")
        List<ResourcePermissionRepresentation> result = producerTemplate.requestBodyAndHeaders(
                getKeycloakEndpoint() + "&operation=listResourcePermissions",
                null,
                headers,
                List.class);

        return Response.ok(result).build();
    }

    @Path("/resource-permission/{realmName}/{clientId}/{permissionId}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getResourcePermission(
            @PathParam("realmName") String realmName,
            @PathParam("clientId") String clientId,
            @PathParam("permissionId") String permissionId) {

        String clientUuid = getClientUuidByClientId(realmName, clientId);

        Map<String, Object> headers = new HashMap<>();
        headers.put(KeycloakConstants.REALM_NAME, realmName);
        headers.put(KeycloakConstants.CLIENT_UUID, clientUuid);
        headers.put(KeycloakConstants.PERMISSION_ID, permissionId);

        PolicyRepresentation result = producerTemplate.requestBodyAndHeaders(
                getKeycloakEndpoint() + "&operation=getResourcePermission",
                null,
                headers,
                PolicyRepresentation.class);

        return Response.ok(result).build();
    }

    @Path("/resource-permission/{realmName}/{clientId}/{permissionId}")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response updateResourcePermission(
            @PathParam("realmName") String realmName,
            @PathParam("clientId") String clientId,
            @PathParam("permissionId") String permissionId,
            PolicyRepresentation permission) {

        String clientUuid = getClientUuidByClientId(realmName, clientId);

        Map<String, Object> headers = new HashMap<>();
        headers.put(KeycloakConstants.REALM_NAME, realmName);
        headers.put(KeycloakConstants.CLIENT_UUID, clientUuid);
        headers.put(KeycloakConstants.PERMISSION_ID, permissionId);

        permission.setId(permissionId);

        String result = producerTemplate.requestBodyAndHeaders(
                getKeycloakEndpoint() + "&operation=updateResourcePermission&pojoRequest=true",
                permission,
                headers,
                String.class);

        return Response.ok(result).build();
    }

    @Path("/resource-permission/{realmName}/{clientId}/{permissionId}")
    @DELETE
    @Produces(MediaType.TEXT_PLAIN)
    public Response deleteResourcePermission(
            @PathParam("realmName") String realmName,
            @PathParam("clientId") String clientId,
            @PathParam("permissionId") String permissionId) {

        String clientUuid = getClientUuidByClientId(realmName, clientId);

        Map<String, Object> headers = new HashMap<>();
        headers.put(KeycloakConstants.REALM_NAME, realmName);
        headers.put(KeycloakConstants.CLIENT_UUID, clientUuid);
        headers.put(KeycloakConstants.PERMISSION_ID, permissionId);

        String result = producerTemplate.requestBodyAndHeaders(
                getKeycloakEndpoint() + "&operation=deleteResourcePermission",
                null,
                headers,
                String.class);

        return Response.ok(result).build();
    }

}
