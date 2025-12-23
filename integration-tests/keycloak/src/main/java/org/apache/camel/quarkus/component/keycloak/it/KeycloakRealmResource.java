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
import org.keycloak.representations.idm.RealmRepresentation;

/**
 * Keycloak REST resource for Realm operations.
 */
@Path("/keycloak/realm")
@ApplicationScoped
public class KeycloakRealmResource extends KeycloakResourceSupport {

    @Path("/{realmName}")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public Response createRealmWithHeaders(@PathParam("realmName") String realmName) {
        Map<String, Object> headers = new HashMap<>();
        headers.put(KeycloakConstants.REALM_NAME, realmName);

        String result = producerTemplate.requestBodyAndHeaders(
                getKeycloakEndpoint() + "&operation=createRealm",
                null,
                headers,
                String.class);

        return Response.ok(result).build();
    }

    @Path("/pojo")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response createRealmWithPojo(RealmRepresentation realm) {
        Map<String, Object> headers = new HashMap<>();
        headers.put(KeycloakConstants.REALM_NAME, realm.getRealm());

        String result = producerTemplate.requestBodyAndHeaders(
                getKeycloakEndpoint() + "&operation=createRealm&pojoRequest=true",
                realm,
                headers,
                String.class);

        return Response.ok(result).build();
    }

    @Path("/{realmName}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getRealm(@PathParam("realmName") String realmName) {
        Map<String, Object> headers = new HashMap<>();
        headers.put(KeycloakConstants.REALM_NAME, realmName);

        RealmRepresentation result = producerTemplate.requestBodyAndHeaders(
                getKeycloakEndpoint() + "&operation=getRealm",
                null,
                headers,
                RealmRepresentation.class);

        return Response.ok(result).build();
    }

    @Path("/{realmName}")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response updateRealm(
            @PathParam("realmName") String realmName,
            RealmRepresentation realm) {

        Map<String, Object> headers = new HashMap<>();
        headers.put(KeycloakConstants.REALM_NAME, realmName);

        String result = producerTemplate.requestBodyAndHeaders(
                getKeycloakEndpoint() + "&operation=updateRealm&pojoRequest=true",
                realm,
                headers,
                String.class);

        return Response.ok(result).build();
    }

    @Path("/{realmName}")
    @DELETE
    @Produces(MediaType.TEXT_PLAIN)
    public Response deleteRealm(@PathParam("realmName") String realmName) {
        Map<String, Object> headers = new HashMap<>();
        headers.put(KeycloakConstants.REALM_NAME, realmName);

        String result = producerTemplate.requestBodyAndHeaders(
                getKeycloakEndpoint() + "&operation=deleteRealm",
                null,
                headers,
                String.class);

        return Response.ok(result).build();
    }
}
