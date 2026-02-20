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

import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.camel.Exchange;
import org.apache.camel.component.keycloak.KeycloakConstants;

@Path("/keycloak/evaluate-permission")
@ApplicationScoped
public class KeycloakEvaluatePermissionResource extends KeycloakResourceSupport {

    /**
     * permissionsOnly mode: sets CamelKeycloakPermissionsOnly=true
     * Returns Map with keys: permissions, permissionCount, granted
     */
    @Path("/permissions-only")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response testEvaluatePermissionWithPermissionsOnly(
            @QueryParam("clientId") String clientId,
            @QueryParam("clientSecret") String clientSecret,
            @QueryParam("accessToken") String accessToken,
            @QueryParam("resourceNames") String resourceNames,
            @QueryParam("scopes") String scopes) {

        Exchange exchange = producerTemplate.send("direct:evaluatePermission", e -> {
            e.getIn().setHeader("X-Authz-Client-Id", clientId);
            e.getIn().setHeader("X-Authz-Client-Secret", clientSecret);
            e.getIn().setHeader(KeycloakConstants.ACCESS_TOKEN, accessToken);
            e.getIn().setHeader(KeycloakConstants.PERMISSIONS_ONLY, Boolean.TRUE);
            if (resourceNames != null) {
                e.getIn().setHeader(KeycloakConstants.PERMISSION_RESOURCE_NAMES, resourceNames);
            }
            if (scopes != null) {
                e.getIn().setHeader(KeycloakConstants.PERMISSION_SCOPES, scopes);
            }
        });

        if (exchange.getException() != null) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(exchange.getException().getMessage())
                    .build();
        }
        return Response.ok(exchange.getMessage().getBody(Map.class)).build();
    }

    /**
     * RPT mode: PERMISSIONS_ONLY header deliberately omitted
     * Returns Map with keys: token, tokenType, expiresIn, refreshToken, refreshExpiresIn, upgraded.
     */
    @Path("/rpt")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response testEvaluatePermissionWithRPT(
            @QueryParam("clientId") String clientId,
            @QueryParam("clientSecret") String clientSecret,
            @QueryParam("accessToken") String accessToken,
            @QueryParam("resourceNames") String resourceNames) {

        Exchange exchange = producerTemplate.send("direct:evaluatePermission", e -> {
            e.getIn().setHeader("X-Authz-Client-Id", clientId);
            e.getIn().setHeader("X-Authz-Client-Secret", clientSecret);
            e.getIn().setHeader(KeycloakConstants.ACCESS_TOKEN, accessToken);
            // if no PERMISSIONS_ONLY in header then go RPT
            if (resourceNames != null) {
                e.getIn().setHeader(KeycloakConstants.PERMISSION_RESOURCE_NAMES, resourceNames);
            }
        });

        if (exchange.getException() != null) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(exchange.getException().getMessage())
                    .build();
        }
        return Response.ok(exchange.getMessage().getBody(Map.class)).build();
    }

    /**
     * test evaluate permission with username and password
     */
    @Path("/username-password")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response testEvaluatePermissionWithUsernameAndPassword(
            @QueryParam("clientId") String clientId,
            @QueryParam("clientSecret") String clientSecret,
            @QueryParam("username") String username,
            @QueryParam("password") String password,
            @QueryParam("resourceNames") String resourceNames) {

        Exchange exchange = producerTemplate.send("direct:evaluatePermissionUserPass", e -> {
            e.getIn().setHeader("X-Authz-Client-Id", clientId);
            e.getIn().setHeader("X-Authz-Client-Secret", clientSecret);
            e.getIn().setHeader("X-Authz-Username", username);
            e.getIn().setHeader("X-Authz-Password", password);
            e.getIn().setHeader(KeycloakConstants.PERMISSIONS_ONLY, Boolean.TRUE);
            if (resourceNames != null) {
                e.getIn().setHeader(KeycloakConstants.PERMISSION_RESOURCE_NAMES, resourceNames);
            }
        });

        if (exchange.getException() != null) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(exchange.getException().getMessage())
                    .build();
        }
        return Response.ok(exchange.getMessage().getBody(Map.class)).build();
    }

    /**
     * test SUBJECT_TOKEN header configuration
     */
    @Path("/subject-token")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response subjectToken(
            @QueryParam("clientId") String clientId,
            @QueryParam("clientSecret") String clientSecret,
            @QueryParam("subjectToken") String subjectToken,
            @QueryParam("resourceNames") String resourceNames) {

        Exchange exchange = producerTemplate.send("direct:evaluatePermission", e -> {
            e.getIn().setHeader("X-Authz-Client-Id", clientId);
            e.getIn().setHeader("X-Authz-Client-Secret", clientSecret);
            e.getIn().setHeader(KeycloakConstants.SUBJECT_TOKEN, subjectToken);
            e.getIn().setHeader(KeycloakConstants.PERMISSIONS_ONLY, Boolean.TRUE);
            if (resourceNames != null) {
                e.getIn().setHeader(KeycloakConstants.PERMISSION_RESOURCE_NAMES, resourceNames);
            }
        });

        if (exchange.getException() != null) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(exchange.getException().getMessage())
                    .build();
        }
        return Response.ok(exchange.getMessage().getBody(Map.class)).build();
    }

    /**
     * test PERMISSION_AUDIENCE header configuration
     */
    @Path("/audience")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response audience(
            @QueryParam("clientId") String clientId,
            @QueryParam("clientSecret") String clientSecret,
            @QueryParam("accessToken") String accessToken,
            @QueryParam("audience") String audience,
            @QueryParam("resourceNames") String resourceNames) {

        Exchange exchange = producerTemplate.send("direct:evaluatePermission", e -> {
            e.getIn().setHeader("X-Authz-Client-Id", clientId);
            e.getIn().setHeader("X-Authz-Client-Secret", clientSecret);
            e.getIn().setHeader(KeycloakConstants.ACCESS_TOKEN, accessToken);
            e.getIn().setHeader(KeycloakConstants.PERMISSION_AUDIENCE, audience);
            e.getIn().setHeader(KeycloakConstants.PERMISSIONS_ONLY, Boolean.TRUE);
            if (resourceNames != null) {
                e.getIn().setHeader(KeycloakConstants.PERMISSION_RESOURCE_NAMES, resourceNames);
            }
        });

        if (exchange.getException() != null) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(exchange.getException().getMessage())
                    .build();
        }
        return Response.ok(exchange.getMessage().getBody(Map.class)).build();
    }
}
