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
import org.keycloak.representations.idm.IdentityProviderRepresentation;

/**
 * Keycloak REST resource for Identity Provider Operations.
 */
@Path("/keycloak")
@ApplicationScoped
public class KeycloakIdentityProviderResource extends KeycloakResourceSupport {

    // ==================== Identity Provider Operations ====================

    @Path("/identity-provider/{realmName}/pojo")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response createIdentityProvider(
            @PathParam("realmName") String realmName,
            IdentityProviderRepresentation idp) {

        Map<String, Object> headers = new HashMap<>();
        headers.put(KeycloakConstants.REALM_NAME, realmName);

        Object result = producerTemplate.requestBodyAndHeaders(
                getKeycloakEndpoint() + "&operation=createIdentityProvider&pojoRequest=true",
                idp,
                headers);

        if (result instanceof Response jaxrsResponse) {
            return Response.status(jaxrsResponse.getStatus())
                    .entity("Identity provider created successfully")
                    .build();
        }

        return Response.ok("Identity provider created successfully").build();
    }

    @Path("/identity-provider/{realmName}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response listIdentityProviders(@PathParam("realmName") String realmName) {
        Map<String, Object> headers = new HashMap<>();
        headers.put(KeycloakConstants.REALM_NAME, realmName);

        @SuppressWarnings("unchecked")
        List<IdentityProviderRepresentation> result = producerTemplate.requestBodyAndHeaders(
                getKeycloakEndpoint() + "&operation=listIdentityProviders",
                null,
                headers,
                List.class);

        return Response.ok(result).build();
    }

    @Path("/identity-provider/{realmName}/{idpAlias}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getIdentityProvider(
            @PathParam("realmName") String realmName,
            @PathParam("idpAlias") String idpAlias) {

        Map<String, Object> headers = new HashMap<>();
        headers.put(KeycloakConstants.REALM_NAME, realmName);
        headers.put(KeycloakConstants.IDP_ALIAS, idpAlias);

        IdentityProviderRepresentation result = producerTemplate.requestBodyAndHeaders(
                getKeycloakEndpoint() + "&operation=getIdentityProvider",
                null,
                headers,
                IdentityProviderRepresentation.class);

        return Response.ok(result).build();
    }

    @Path("/identity-provider/{realmName}/{idpAlias}")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response updateIdentityProvider(
            @PathParam("realmName") String realmName,
            @PathParam("idpAlias") String idpAlias,
            IdentityProviderRepresentation idp) {

        Map<String, Object> headers = new HashMap<>();
        headers.put(KeycloakConstants.REALM_NAME, realmName);
        headers.put(KeycloakConstants.IDP_ALIAS, idpAlias);

        idp.setAlias(idpAlias);

        String result = producerTemplate.requestBodyAndHeaders(
                getKeycloakEndpoint() + "&operation=updateIdentityProvider&pojoRequest=true",
                idp,
                headers,
                String.class);

        return Response.ok(result).build();
    }

    @Path("/identity-provider/{realmName}/{idpAlias}")
    @DELETE
    @Produces(MediaType.TEXT_PLAIN)
    public Response deleteIdentityProvider(
            @PathParam("realmName") String realmName,
            @PathParam("idpAlias") String idpAlias) {

        Map<String, Object> headers = new HashMap<>();
        headers.put(KeycloakConstants.REALM_NAME, realmName);
        headers.put(KeycloakConstants.IDP_ALIAS, idpAlias);

        String result = producerTemplate.requestBodyAndHeaders(
                getKeycloakEndpoint() + "&operation=deleteIdentityProvider",
                null,
                headers,
                String.class);

        return Response.ok(result).build();
    }

}
