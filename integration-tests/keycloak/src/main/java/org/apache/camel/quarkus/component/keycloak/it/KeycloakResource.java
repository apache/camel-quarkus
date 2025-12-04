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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.keycloak.KeycloakConstants;
import org.apache.camel.component.mock.MockEndpoint;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import org.keycloak.representations.idm.AdminEventRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

@Path("/keycloak")
@ApplicationScoped
public class KeycloakResource {

    private static final Logger LOG = Logger.getLogger(KeycloakResource.class);

    private static final String COMPONENT_KEYCLOAK = "keycloak";

    @Inject
    CamelContext context;

    @Inject
    ProducerTemplate producerTemplate;

    @ConfigProperty(name = "keycloak.url")
    String keycloakUrl;

    @ConfigProperty(name = "keycloak.username")
    String keycloakUsername;

    @ConfigProperty(name = "keycloak.password")
    String keycloakPassword;

    @ConfigProperty(name = "keycloak.realm")
    String keycloakRealm;

    private String getKeycloakEndpoint() {
        return String.format("keycloak:admin?serverUrl=%s&realm=%s&username=%s&password=%s",
                keycloakUrl, keycloakRealm, keycloakUsername, keycloakPassword);
    }

    @Path("/load/component/keycloak")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response loadComponentKeycloak() throws Exception {
        if (context.getComponent(COMPONENT_KEYCLOAK) != null) {
            return Response.ok().build();
        }
        LOG.warnf("Could not load [%s] from the Camel context", COMPONENT_KEYCLOAK);
        return Response.status(500, COMPONENT_KEYCLOAK + " could not be loaded from the Camel context").build();
    }

    // ==================== Realm Operations ====================

    @Path("/realm/{realmName}")
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

    @Path("/realm/pojo")
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

    @Path("/realm/{realmName}")
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

    @Path("/realm/{realmName}")
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

    // ==================== User Operations ====================

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

        if (result instanceof jakarta.ws.rs.core.Response) {
            jakarta.ws.rs.core.Response jaxrsResponse = (jakarta.ws.rs.core.Response) result;
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

        if (result instanceof jakarta.ws.rs.core.Response) {
            jakarta.ws.rs.core.Response jaxrsResponse = (jakarta.ws.rs.core.Response) result;
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

        // First, list users to find the user ID by username
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

    /**
     * Helper method to get user ID by username
     */
    private String getUserIdByUsername(String realmName, String username) {
        Map<String, Object> headers = new HashMap<>();
        headers.put(KeycloakConstants.REALM_NAME, realmName);

        @SuppressWarnings("unchecked")
        List<UserRepresentation> users = producerTemplate.requestBodyAndHeaders(
                getKeycloakEndpoint() + "&operation=listUsers",
                null,
                headers,
                List.class);

        return users.stream()
                .filter(u -> username.equals(u.getUsername()))
                .map(UserRepresentation::getId)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
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

        // First, get the user ID by username
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

    // ==================== Role Operations ====================

    @Path("/role/{realmName}/{roleName}")
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

    @Path("/role/{realmName}/pojo")
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

    @Path("/role/{realmName}/{roleName}")
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

    @Path("/role/{realmName}")
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

    @Path("/role/{realmName}/{roleName}")
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

    // ==================== User-Role Operations ====================

    @Path("/user-role/{realmName}/{username}/{roleName}")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public Response assignRoleToUser(
            @PathParam("realmName") String realmName,
            @PathParam("username") String username,
            @PathParam("roleName") String roleName) {

        // First, get the user ID by username
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

        // First, get the user ID by username
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

    // ==================== Consumer Operations ====================

    @Path("/events/admin/{realmName}/enable")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public Response enableAdminEvents(@PathParam("realmName") String realmName) {
        try {
            // Get the realm representation
            Map<String, Object> headers = new HashMap<>();
            headers.put(KeycloakConstants.REALM_NAME, realmName);

            RealmRepresentation realm = producerTemplate.requestBodyAndHeaders(
                    getKeycloakEndpoint() + "&operation=getRealm",
                    null,
                    headers,
                    RealmRepresentation.class);

            // Enable admin events
            realm.setAdminEventsEnabled(true);
            realm.setAdminEventsDetailsEnabled(true);
            realm.setEventsEnabled(true);

            // Update the realm
            producerTemplate.requestBodyAndHeaders(
                    getKeycloakEndpoint() + "&operation=updateRealm&pojoRequest=true",
                    realm,
                    headers,
                    String.class);

            return Response.ok("Admin events enabled successfully").build();
        } catch (Exception e) {
            LOG.error("Failed to enable admin events", e);
            return Response.status(500).entity("Failed to enable admin events: " + e.getMessage()).build();
        }
    }

    @Path("/events/admin/collected")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCollectedAdminEvents() {
        try {
            MockEndpoint mockEndpoint = context.getEndpoint("mock:admin-events", MockEndpoint.class);
            List<AdminEventRepresentation> events = new ArrayList<>();

            mockEndpoint.getExchanges().forEach(exchange -> {
                Object body = exchange.getIn().getBody();
                if (body instanceof AdminEventRepresentation) {
                    events.add((AdminEventRepresentation) body);
                }
            });

            return Response.ok(events).build();
        } catch (Exception e) {
            LOG.error("Failed to get collected admin events", e);
            return Response.status(500).entity("Failed to get admin events: " + e.getMessage()).build();
        }
    }

    @Path("/events/regular/collected")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCollectedRegularEvents() {
        try {
            MockEndpoint mockEndpoint = context.getEndpoint("mock:events", MockEndpoint.class);
            List<EventRepresentation> events = new ArrayList<>();

            mockEndpoint.getExchanges().forEach(exchange -> {
                Object body = exchange.getIn().getBody();
                if (body instanceof EventRepresentation) {
                    events.add((EventRepresentation) body);
                }
            });

            return Response.ok(events).build();
        } catch (Exception e) {
            LOG.error("Failed to get collected regular events", e);
            return Response.status(500).entity("Failed to get regular events: " + e.getMessage()).build();
        }
    }

    @Path("/events/admin/reset")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public Response resetAdminEventsMock() {
        try {
            MockEndpoint mockEndpoint = context.getEndpoint("mock:admin-events", MockEndpoint.class);
            mockEndpoint.reset();
            return Response.ok("Admin events mock reset successfully").build();
        } catch (Exception e) {
            LOG.error("Failed to reset admin events mock", e);
            return Response.status(500).entity("Failed to reset mock: " + e.getMessage()).build();
        }
    }

    @Path("/events/regular/reset")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public Response resetRegularEventsMock() {
        try {
            MockEndpoint mockEndpoint = context.getEndpoint("mock:events", MockEndpoint.class);
            mockEndpoint.reset();
            return Response.ok("Regular events mock reset successfully").build();
        } catch (Exception e) {
            LOG.error("Failed to reset regular events mock", e);
            return Response.status(500).entity("Failed to reset mock: " + e.getMessage()).build();
        }
    }

    @Path("/consumer/admin-events/start/{realmName}")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public Response startAdminEventsConsumer(@PathParam("realmName") String realmName) {
        try {
            context.getRouteController().startRoute("admin-events-consumer-" + realmName);
            return Response.ok("Admin events consumer started").build();
        } catch (Exception e) {
            LOG.error("Failed to start admin events consumer", e);
            return Response.status(500).entity("Failed to start consumer: " + e.getMessage()).build();
        }
    }

    @Path("/consumer/regular-events/start/{realmName}")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public Response startRegularEventsConsumer(@PathParam("realmName") String realmName) {
        try {
            context.getRouteController().startRoute("regular-events-consumer-" + realmName);
            return Response.ok("Regular events consumer started").build();
        } catch (Exception e) {
            LOG.error("Failed to start regular events consumer", e);
            return Response.status(500).entity("Failed to start consumer: " + e.getMessage()).build();
        }
    }

    @Path("/consumer/route/create/{realmName}")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public Response createConsumerRoutes(@PathParam("realmName") String realmName) {
        try {
            // Create admin events consumer route
            context.addRoutes(new org.apache.camel.builder.RouteBuilder() {
                @Override
                public void configure() throws Exception {
                    from("keycloak:adminEvents"
                            + "?serverUrl=" + keycloakUrl
                            + "&authRealm=" + keycloakRealm
                            + "&username=" + keycloakUsername
                            + "&password=" + keycloakPassword
                            + "&realm=" + realmName
                            + "&eventType=admin-events"
                            + "&maxResults=50"
                            + "&initialDelay=500"
                            + "&delay=1000")
                            .autoStartup(false)
                            .routeId("admin-events-consumer-" + realmName)
                            .to("mock:admin-events");

                    // Create regular events consumer route
                    from("keycloak:events"
                            + "?serverUrl=" + keycloakUrl
                            + "&authRealm=" + keycloakRealm
                            + "&username=" + keycloakUsername
                            + "&password=" + keycloakPassword
                            + "&realm=" + realmName
                            + "&eventType=events"
                            + "&maxResults=50"
                            + "&initialDelay=500"
                            + "&delay=1000")
                            .autoStartup(false)
                            .routeId("regular-events-consumer-" + realmName)
                            .to("mock:events");
                }
            });

            return Response.ok("Consumer routes created successfully").build();
        } catch (Exception e) {
            LOG.error("Failed to create consumer routes", e);
            return Response.status(500).entity("Failed to create routes: " + e.getMessage()).build();
        }
    }
}
