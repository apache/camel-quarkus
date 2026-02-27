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
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.camel.Exchange;
import org.apache.camel.component.keycloak.security.KeycloakSecurityConstants;
import org.apache.camel.component.keycloak.security.KeycloakTokenIntrospector;
import org.apache.camel.component.keycloak.security.cache.TokenCache;
import org.apache.camel.component.keycloak.security.cache.TokenCacheType;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Keycloak REST resource for SecurityPolicy operations.
 */
@Path("/keycloak")
@ApplicationScoped
public class KeycloakSecurityPolicyResource extends KeycloakResourceSupport {

    @ConfigProperty(name = "test.realm")
    String testRealm;

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

    @Path("/secure-policy/introspection-cache-concurrent-map")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response policyWithConcurrentMapCache(
            @QueryParam("propertyToken") String propertyToken) {

        Exchange exchange = producerTemplate.send("direct:introspection-concurrent-map", e -> {
            e.setProperty(KeycloakSecurityConstants.ACCESS_TOKEN_PROPERTY, propertyToken);
        });

        if (exchange.getException() != null) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(exchange.getException().getMessage())
                    .build();
        }
        return Response.ok(exchange.getMessage().getBody()).build();
    }

    @Path("/secure-policy/introspection-no-cache")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response policyWithNoCache(
            @QueryParam("propertyToken") String propertyToken) {

        Exchange exchange = producerTemplate.send("direct:introspection-no-cache", e -> {
            e.setProperty(KeycloakSecurityConstants.ACCESS_TOKEN_PROPERTY, propertyToken);
        });

        if (exchange.getException() != null) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(exchange.getException().getMessage())
                    .build();
        }
        return Response.ok(exchange.getMessage().getBody()).build();
    }

    @Path("/introspection-cache/introspector/concurrent-map")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response introspectorWithConcurrentMap(
            @QueryParam("accessToken") String accessToken,
            @QueryParam("clientId") String clientId,
            @QueryParam("clientSecret") String clientSecret) {

        KeycloakTokenIntrospector introspector = buildIntrospector(clientId, clientSecret,
                TokenCacheType.CONCURRENT_MAP,
                60, 0, false);

        try {
            return buildIntrospectionResponse(introspector, accessToken);
        } finally {
            introspector.close();
        }
    }

    @Path("/introspection-cache/introspector/caffeine")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response introspectorWithCaffeine(
            @QueryParam("accessToken") String accessToken,
            @QueryParam("clientId") String clientId,
            @QueryParam("clientSecret") String clientSecret) {

        KeycloakTokenIntrospector introspector = buildIntrospector(clientId, clientSecret,
                TokenCacheType.CAFFEINE,
                60, 100, false);

        try {
            return buildIntrospectionResponse(introspector, accessToken);
        } finally {
            introspector.close();
        }
    }

    @Path("/introspection-cache/introspector/none")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response introspectorWithNoCache(
            @QueryParam("accessToken") String accessToken,
            @QueryParam("clientId") String clientId,
            @QueryParam("clientSecret") String clientSecret) {

        KeycloakTokenIntrospector introspector = buildIntrospector(clientId, clientSecret,
                TokenCacheType.NONE,
                60, 0, false);

        try {
            return buildIntrospectionResponse(introspector, accessToken);
        } finally {
            introspector.close();
        }
    }

    @Path("/introspection-cache/introspector/caffeine-stats")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response introspectorCaffeineStats(
            @QueryParam("accessToken") String accessToken,
            @QueryParam("clientId") String clientId,
            @QueryParam("clientSecret") String clientSecret) {

        KeycloakTokenIntrospector introspector = buildIntrospector(clientId, clientSecret,
                TokenCacheType.CAFFEINE,
                60, 100, true);

        try {
            introspector.introspect(accessToken);
            introspector.introspect(accessToken);

            TokenCache.CacheStats stats = introspector.getCacheStats();

            Map<String, Object> result = new HashMap<>();
            result.put("hitCount", stats.getHitCount());
            result.put("missCount", stats.getMissCount());
            result.put("evictionCount", stats.getEvictionCount());
            result.put("hitRate", stats.getHitRate());
            result.put("cacheSize", introspector.getCacheSize());

            return Response.ok(result).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(e.getMessage())
                    .build();
        } finally {
            introspector.close();
        }
    }

    private KeycloakTokenIntrospector buildIntrospector(String clientId, String clientSecret, TokenCacheType cacheType,
            long ttl, long maxSize, boolean recordStats) {
        return new KeycloakTokenIntrospector(keycloakUrl, testRealm, clientId, clientSecret, cacheType, ttl, maxSize,
                recordStats);
    }

    private Response buildIntrospectionResponse(
            KeycloakTokenIntrospector introspector, String accessToken) {
        try {
            KeycloakTokenIntrospector.IntrospectionResult result = introspector.introspect(accessToken);

            Map<String, Object> body = new HashMap<>();
            body.put("active", result.isActive());
            body.put("subject", result.getSubject());
            body.put("cacheSize", introspector.getCacheSize());

            TokenCache.CacheStats stats = introspector.getCacheStats();
            if (stats != null) {
                body.put("hitCount", stats.getHitCount());
                body.put("missCount", stats.getMissCount());
                body.put("hitRate", stats.getHitRate());
            }

            return Response.ok(body).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(e.getMessage())
                    .build();
        }
    }
}
