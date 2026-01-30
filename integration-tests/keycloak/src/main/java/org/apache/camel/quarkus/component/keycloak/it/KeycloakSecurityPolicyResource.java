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

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.camel.Exchange;
import org.apache.camel.component.keycloak.security.KeycloakSecurityConstants;

/**
 * Keycloak REST resource for SecurityPolicy operations.
 */
@Path("/keycloak")
@ApplicationScoped
public class KeycloakSecurityPolicyResource extends KeycloakResourceSupport {

    @Path("/secure-policy/user-with-token-in-property")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response securityPolicyWithTokenInProperty(@QueryParam("propertyToken") String propertyToken) {
        Exchange exchange = producerTemplate.send("direct:secure-default", e -> {
            e.setProperty(KeycloakSecurityConstants.ACCESS_TOKEN_PROPERTY, propertyToken);
        });

        if (exchange.getException() != null) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(exchange.getException().getMessage())
                    .build();
        }
        return Response.ok(exchange.getMessage().getBody()).build();
    }

    @Path("/secure-policy/user-with-token-in-header")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response securityPolicyWithTokenInHeader(@QueryParam("headerToken") String headerToken) {

        Exchange exchange = producerTemplate.send("direct:secure-default", e -> {
            e.getIn().setHeader(KeycloakSecurityConstants.ACCESS_TOKEN_HEADER, headerToken);
        });

        if (exchange.getException() != null) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(exchange.getException().getMessage())
                    .build();
        }

        return Response.ok(exchange.getMessage().getBody()).build();
    }

    @Path("/secure-policy/user-with-token-in-property-and-header")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response securityPolicyWithPropertyPreferredOverHeader(@QueryParam("propertyToken") String propertyToken,
            @QueryParam("headerToken") String headerToken) {

        Exchange exchange = producerTemplate.send("direct:secure-default", e -> {
            e.setProperty(KeycloakSecurityConstants.ACCESS_TOKEN_PROPERTY, propertyToken);
            e.getIn().setHeader(KeycloakSecurityConstants.ACCESS_TOKEN_HEADER, headerToken);
        });

        if (exchange.getException() != null) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(exchange.getException().getMessage())
                    .build();
        }

        return Response.ok(exchange.getMessage().getBody()).build();
    }

    @Path("/secure-policy/max-security")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response securityPolicyWithHeaderDisabled(@QueryParam("propertyToken") String propertyToken,
            @QueryParam("headerToken") String headerToken) {
        Exchange exchange;
        if (propertyToken != null) {
            exchange = producerTemplate.send("direct:max-security", e -> {
                e.setProperty(KeycloakSecurityConstants.ACCESS_TOKEN_PROPERTY, propertyToken);
            });
        } else if (headerToken != null) {
            exchange = producerTemplate.send("direct:max-security", e -> {
                e.getIn().setHeader(KeycloakSecurityConstants.ACCESS_TOKEN_HEADER, headerToken);
            });
        } else {
            exchange = producerTemplate.send("direct:max-security", e -> {
            });
        }

        if (exchange.getException() != null) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(exchange.getException().getMessage())
                    .build();
        }

        return Response.ok(exchange.getMessage().getBody()).build();
    }

    @Path("/secure-policy/legacy-unsafe")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response securityPolicyWithLegacyUnsafe(@QueryParam("propertyToken") String propertyToken,
            @QueryParam("headerToken") String headerToken) {

        Exchange exchange = producerTemplate.send("direct:legacy-unsafe", e -> {
            e.setProperty(KeycloakSecurityConstants.ACCESS_TOKEN_PROPERTY, propertyToken);
            e.getIn().setHeader(KeycloakSecurityConstants.ACCESS_TOKEN_HEADER, headerToken);
        });

        if (exchange.getException() != null) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(exchange.getException().getMessage())
                    .build();
        }

        return Response.ok(exchange.getMessage().getBody()).build();
    }

    @Path("/secure-policy/authorization-header-format")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response securityPolicyWithAuthorizationHeader(@QueryParam("headerToken") String headerToken) {

        String result = producerTemplate.requestBodyAndHeader("direct:secure-default", "test",
                "Authorization", "Bearer " + headerToken, String.class);

        return Response.ok(result).build();
    }

    @Path("/secure-policy/admin-only")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response securityPolicyWithAdminOnly(@QueryParam("propertyToken") String propertyToken,
            @QueryParam("headerToken") String headerToken) {

        Exchange exchange = producerTemplate.send("direct:admin-only", e -> {
            e.setProperty(KeycloakSecurityConstants.ACCESS_TOKEN_PROPERTY, propertyToken);
            e.getIn().setHeader(KeycloakSecurityConstants.ACCESS_TOKEN_HEADER, headerToken);
        });

        if (exchange.getException() != null) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(exchange.getException().getMessage())
                    .build();
        }

        return Response.ok(exchange.getMessage().getBody()).build();
    }

}
